package linksawakening.world;

import static linksawakening.world.EntityMotionMath.*;
import static linksawakening.world.EntitySnapshots.*;
import static linksawakening.world.EntityTypeRules.*;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import static linksawakening.world.RoomEntityRuntime.*;

/** Slot clearing, status countdowns and persistence lifecycle. */
final class EntitySlotLifecycle {
    private final RoomEntityRuntime runtime;
    EntitySlotLifecycle(RoomEntityRuntime runtime) { this.runtime = runtime; }

    EntityPickupEvent collectIfNeeded(int frameCounter,
                                             int linkEntityX,
                                             int linkEntityY,
                                             boolean linkAirborne,
                                             boolean linkInteractive) {
        return collectIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne,
            linkInteractive, 0, 0);
    }

    EntityPickupEvent collectIfNeeded(int frameCounter,
                                             int linkEntityX,
                                             int linkEntityY,
                                             boolean linkAirborne,
                                             boolean linkInteractive,
                                             int linkDirection) {
        return collectIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne,
            linkInteractive, linkDirection, 0);
    }

    EntityPickupEvent collectIfNeeded(int frameCounter,
                                             int linkEntityX,
                                             int linkEntityY,
                                             boolean linkAirborne,
                                             boolean linkInteractive,
                                             int linkDirection,
                                             int linkZ) {
        if (!linkInteractive) {
            return null;
        }
        int romDirection = romDirectionForJavaDirection(linkDirection);

        for (int index = runtime.slots.length - 1; index >= 0; index--) {
            RoomEntity entity = runtime.slots[index];
            if (entity.type() == runtime.ENTITY_DROPPABLE_SECRET_SEASHELL) {
                runtime.secretSeashellMotion.ensureInitialized(entity.slot(), runtime.entityRoomId);
            }
            boolean floatingType = FloatingItemMotion.isFloatingItem(entity.type());
            boolean floating = isRuntimeFloatingItem(entity);
            if (!entity.loaded() || entity.status() != EntityStatus.ACTIVE
                || !RoomEntityPickupRules.isPickable(entity.type())
                || (floatingType && !floating)
                || (entity.type() == 0x35 && runtime.heartPiecePickup[entity.slot()] != null)
                || runtime.powerUpPickupCountdown[entity.slot()] != 0
                || runtime.dropPrivateCountdown1[entity.slot()] > 0
                || (entity.type() == runtime.ENTITY_HEART_CONTAINER
                    && runtime.enemyTransitionCountdown[entity.slot()] != 0)
                || (entity.type() == runtime.ENTITY_SLEEPY_TOADSTOOL
                    && runtime.toadstoolPickupSequenceActive[entity.slot()])
                || (isCommonDroppableType(entity.type())
                    && runtime.droppablePrivateState3[entity.slot()] != 0)
                || (entity.type() == runtime.ENTITY_DROPPABLE_SECRET_SEASHELL
                    && (runtime.secretSeashellMotion.privateState3(entity.slot()) != 0
                        || runtime.secretSeashellMotion.privateCountdown1(entity.slot()) != 0))
                || (!floating && !RoomEntityPickupRules.collisionCadenceMatches(
                    frameCounter, entity.slot()))
                || (!floating && linkAirborne)
                || (floating && !FloatingItemMotion.linkZAllowsCollection(
                    runtime.groundInteractionSideScrolling, linkZ))
                || !(floating
                    ? RoomEntityPickupRules.overlapsFloatingItem(entity, linkEntityX, linkEntityY)
                    : RoomEntityPickupRules.overlapsLink(entity, linkEntityX, linkEntityY))) {
                continue;
            }

            int persistentClearMask = entity.type() == runtime.ENTITY_SWORD_SHIELD_PICKUP
                && runtime.chestSwordLevel == 0 ? 0 : persistentClearMask(entity);
            if (entity.type() == runtime.ENTITY_KEY_DROP_POINT) {
                if (runtime.entityRoomId == 0x80 || entity.spriteVariant() != 0) {
                    runtime.enemyTransitionCountdown[entity.slot()] =
                        KeyDropPointMotion.TRANSITION_COUNTDOWN;
                } else {
                    runtime.pendingKeyRewardEvents.add(new KeyRewardEvent(
                        entity.slot(), ChestContentsTable.CHEST_SMALL_KEY));
                    clearEntity(entity.slot());
                }
            } else if (entity.type() == 0x35) {
                runtime.heartPiecePickup[entity.slot()] = new HeartPiecePickupMotion();
                runtime.enemyTransitionCountdown[entity.slot()] = 5;
                runtime.pendingMusicTrack = 0x10;
            } else if (entity.type() == runtime.ENTITY_HEART_CONTAINER) {
                runtime.enemyTransitionCountdown[entity.slot()] = 0x70;
            } else if (entity.type() == runtime.ENTITY_SWORD_SHIELD_PICKUP
                && runtime.chestSwordLevel == 0) {
                runtime.startSwordPickup(entity.slot());
            } else if (entity.type() == runtime.ENTITY_SLEEPY_TOADSTOOL) {
                runtime.startToadstoolPickup(entity.slot());
            } else if (entity.type()
                == EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS) {
                runtime.startInstrumentPickup(entity.slot());
            } else if (entity.type() == 0x33 || entity.type() == 0x34) {
                runtime.powerUpPickupCountdown[entity.slot()] = 0x30;
                runtime.entityInteractionCountdown = 0x30;
                runtime.pendingMusicTrack = 0x27;
            } else if (requiresHeldPickupTransition(entity.type())) {
                runtime.beginLift(entity.slot(), romDirection);
            } else {
                clearEntity(entity.slot());
            }
            return new EntityPickupEvent(entity.slot(), entity.type(), persistentClearMask,
                floating ? entity.spriteVariant() : -1);
        }
        return null;
    }

    int clearEntity(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        runtime.resetEnemyDropState(slot);
        runtime.sideViewPlatformMotion.clear(slot);
        runtime.sideViewPotMotion.clear(slot);
        runtime.genieMotion.clear(slot);
        if (runtime.genieControllerSlot == slot) {
            runtime.genieControllerSlot = -1;
        }
        runtime.genieActiveState[slot] = 0;
        runtime.geniePrivateState1[slot] = 0;
        runtime.geniePrivateState2[slot] = 0;
        runtime.geniePrivateState3[slot] = 0;
        runtime.geniePrivateState4[slot] = 0;
        runtime.geniePrivateCountdown1[slot] = 0;
        runtime.geniePrivateCountdown2[slot] = 0;
        runtime.genieDirection[slot] = 0;
        runtime.genieDeathInitialized[slot] = false;
        runtime.genieSpeedX[slot] = 0;
        runtime.genieSpeedY[slot] = 0;
        runtime.genieSpeedZ[slot] = 0;
        runtime.genieSpeedXAccumulator[slot] = 0;
        runtime.genieSpeedYAccumulator[slot] = 0;
        runtime.genieSpeedZAccumulator[slot] = 0;
        RoomEntity entity = runtime.slots[slot];
        runtime.bombFinalPresentationPending[slot] = false;
        runtime.enemyHitboxFlags[slot] = 0;
        runtime.playerArrowBombArrow[slot] = false;
        runtime.bombPrivateCountdown1[slot] = 0;
        runtime.bombPrivateCountdown3[slot] = 0;
        runtime.magicPowderState[slot] = 0;
        runtime.magicPowderPrivateState4[slot] = 0;
        runtime.secretSeashellMotion.clear(slot);
        runtime.entityPrivateState4[slot] = 0;
        runtime.entityInertia[slot] = 0;
        runtime.musicalNoteInertia[slot] = 0;
        runtime.musicalNoteSpeedX[slot] = 0;
        runtime.musicalNoteSpeedY[slot] = 0;
        runtime.entityUnknownJ[slot] = 0;
        runtime.ironMaskPrivateState2[slot] = 0;
        runtime.ironMasksMaskSourceHookshotSlot[slot] = 0;
        runtime.liftableRockSmashCountdown[slot] = 0;
        runtime.liftableRockSmashSourceVariant[slot] = 0;
        runtime.liftableRockSmashActive[slot] = false;
        if (runtime.latestDroppedBombEntityIndex == slot) {
            runtime.latestDroppedBombEntityIndex = -1;
        }
        if (runtime.latestShotArrowEntityIndex == slot) {
            runtime.latestShotArrowEntityIndex = -1;
        }
        runtime.bombiteMotion.clear(slot);
        runtime.blooperMotion.clear(slot);
        runtime.pincerMotion.clear(slot);
        runtime.bushCrawlerMotion.clear(slot);
        runtime.pokeyMotion.clear(slot);
        runtime.zoraMotion.clear(slot);
        runtime.zombieMotion.clear(slot);
        runtime.buzzBlobMotion.clear(slot);
        runtime.sandCrabMotion.clear(slot);
        runtime.dogMotion.clear(slot);
        runtime.urchinMotion.clear(slot);
        runtime.witchRatMotion.clear(slot);
        runtime.mimicMotion.clear(slot);
        runtime.maskedMimicMotion.clear(slot);
        runtime.miniMoldormMotion.clear(slot);
        runtime.moldormMotion.clear(slot);
        if (!entity.loaded()) {
            return 0;
        }
        runtime.slowTransitionCountdown[slot] = 0;
        runtime.slowTimerInitialized[slot] = false;
        runtime.enemyTransitionCountdown[slot] = 0;
        runtime.enemyStunnedCountdown[slot] = 0;
        runtime.dyingCountdown[slot] = 0;
        runtime.powerRecoilDeath[slot] = false;
        runtime.enemyPhysicsFlags[slot] = 0;
        runtime.enemyHealth[slot] = 0;
        runtime.enemyFlashCountdown[slot] = 0;
        runtime.enemyIgnoreHitsCountdown[slot] = 0;
        runtime.chestSpeedY[slot] = 0;
        runtime.chestSpeedYAccumulator[slot] = 0;
        runtime.chestInertia[slot] = 0;
        runtime.chestItemBySlot[slot] = 0;
        runtime.owlPrivateCountdown1[slot] = 0;
        runtime.entityGroundStatus[slot] = 0;
        runtime.fallingTargetX[slot] = 0;
        runtime.fallingTargetY[slot] = 0;
        runtime.fallingSpeedX[slot] = 0;
        runtime.fallingSpeedY[slot] = 0;
        runtime.fallingSpeedXAccumulator[slot] = 0;
        runtime.fallingSpeedYAccumulator[slot] = 0;
        runtime.fallingVisualYOffset[slot] = 0;
        runtime.liftedPhase[slot] = 0;
        runtime.liftedSourceDirection[slot] = 0;
        runtime.liftedStateInitialized[slot] = false;
        runtime.thrownDirection[slot] = 0xFF;
        runtime.bombDirection[slot] = 0xFF;
        runtime.bombPrivateState4[slot] = 0;
        runtime.magicPowderState[slot] = 0;
        runtime.magicPowderPrivateState4[slot] = 0;
        runtime.bombPrivateCountdown1[slot] = 0;
        runtime.bombPrivateCountdown3[slot] = 0;
        runtime.placedBombMotionInitialized[slot] = false;
        runtime.ledgeTransitionTimer[slot] = 0;
        runtime.thrownMotionInitialized[slot] = false;
        runtime.hookshotChainMotion.clear(slot);
        runtime.hookshotBridgeMotion.clear(slot);
        runtime.boomerangMotion.clear(slot);
        runtime.magicRodFireballMotion.clear(slot);
        runtime.swordBeamMotion.clear(slot);
        if (entity.type() == runtime.ENTITY_HOOKSHOT_CHAIN) {
            runtime.hookshotChainOam = List.of();
        }
        runtime.baseEntityFlipAttribute[slot] = 0;
        runtime.entityOptions1Override[slot] = -1;
        runtime.enemyRecoilMotion.clear(slot);
        runtime.colorShellMotion.clear(slot);
        runtime.butterflyMotion.clear(slot);
        runtime.keeseMotion.clear(slot);
        runtime.roamingEnemyMotion.clear(slot);
        runtime.unmaskedIronMaskMotion.clear(slot);
        runtime.moblinSwordMotion.clear(slot);
        runtime.playerArrowMotion.clear(slot);
        runtime.tektiteMotion.clear(slot);
        runtime.leeverMotion.clear(slot);
        runtime.antiFairyMotion.clear(slot);
        runtime.sparkMotion.clear(slot);
        runtime.zolGelMotion.clear(slot);
        runtime.pokeyMotion.clear(slot);
        runtime.hidingZolMotion.clear(slot);
        runtime.starMotion.clear(slot);
        runtime.blooperMotion.clear(slot);
        runtime.pincerMotion.clear(slot);
        runtime.spikeTrapMotion.clear(slot);
        runtime.pairoddMotion.clear(slot);
        runtime.pairoddProjectileMotion.clear(slot);
        runtime.enemyProjectileMotion.clear(slot);
        runtime.wingedOctorokMotion.clear(slot);
        runtime.bushCrawlerMotion.clear(slot);
        runtime.cuccoMotion.clear(slot);
        runtime.giantGopongaMotion.clear(slot);
        runtime.gopongaProjectileMotion.clear(slot);
        runtime.zoraMotion.clear(slot);
        runtime.zombieMotion.clear(slot);
        runtime.buzzBlobMotion.clear(slot);
        runtime.sandCrabMotion.clear(slot);
        runtime.dogMotion.clear(slot);
        runtime.urchinMotion.clear(slot);
        runtime.witchRatMotion.clear(slot);
        runtime.wingedSwordAttackThisFrame[slot] = false;
        runtime.wingedStateTwoThisFrame[slot] = false;
        runtime.laserMotion.clear(slot);
        runtime.waterTektiteMotion.clear(slot);
        runtime.fishMotion.clear(slot);
        runtime.crowMotion.clear(slot);
        runtime.booBuddyMotion.clear(slot);
        runtime.stalfosAggressiveMotion.clear(slot);
        runtime.stalfosEvasiveMotion.clear(slot);
        runtime.threeOfAKindMotion.clear(slot);
        runtime.gibdoMotion.clear(slot);
        runtime.likeLikeMotion.clear(slot);
        runtime.goombaMotion.clear(slot);
        runtime.snakeMotion.clear(slot);
        runtime.wizrobeMotion.clear(slot);
        runtime.wizrobeProjectileMotion.clear(slot);
        runtime.peaHatMotion.clear(slot);
        runtime.armosMotion.clear(slot);
        runtime.ghiniMotion.clear(slot);
        runtime.hardHatMotion.clear(slot);
        runtime.madBomberMotion.clear(slot);
        runtime.followingNpcMotion.clear(slot);
        runtime.bowWowMotion.clear(slot);
        runtime.moblinKingMotion.clear(slot);
        runtime.thrownEntityMotion.clear(slot);
        if (runtime.liftedEntitySlot == slot) {
            runtime.liftedEntitySlot = -1;
            runtime.liftedCarryState = 0;
            runtime.liftedEffectiveDirection = 0;
        }
        runtime.slots[slot] = RoomEntity.disabled(slot);
        runtime.updatePincerBodyOam();
        return entity.sourceLoadOrder() >= 0 && entity.sourceLoadOrder() < 8
            ? 1 << entity.sourceLoadOrder() : 0;
    }

    RoomEntity applyZolSplit(RoomEntity original, RoomEntity updated,
                                      ZolGelMotion.Split split) {
        EntitySpriteDefinition gelDefinition = runtime.spriteDefinitionFor(runtime.ENTITY_GEL);
        int gelVariant = gelDefinition.supported() ? 0 : -1;
        int slot = original.slot();
        RoomEntity gel = new RoomEntity(slot, original.sourceLoadOrder(), runtime.ENTITY_GEL,
            (split.originalX() - 4) & 0xFF, split.originalY(), EntityStatus.ACTIVE,
            gelDefinition, gelVariant, original.entityFlipAttribute(), original.spriteTileOffset(),
            split.originalZ(), -1, false);
        runtime.enemyTransitionCountdown[slot] = 0;
        runtime.enemyStunnedCountdown[slot] = 0;
        runtime.enemyHealth[slot] = runtime.initialHealth(runtime.ENTITY_GEL);
        runtime.enemyFlashCountdown[slot] = 0;
        runtime.enemyIgnoreHitsCountdown[slot] = 0;
        runtime.dyingCountdown[slot] = 0;
        runtime.powerRecoilDeath[slot] = false;
        runtime.enemyRecoilMotion.clear(slot);

        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot >= 0) {
            runtime.baseEntityFlipAttribute[freeSlot] = runtime.baseEntityFlipAttribute[original.slot()];
            RoomEntity spawnedGel = new RoomEntity(freeSlot, original.sourceLoadOrder(), runtime.ENTITY_GEL,
                (split.originalX() + 8) & 0xFF, split.originalY(), EntityStatus.ACTIVE,
                gelDefinition, gelVariant, original.entityFlipAttribute(), original.spriteTileOffset(),
                split.originalZ(), -1, false);
            runtime.slots[freeSlot] = spawnedGel;
            runtime.enemyTransitionCountdown[freeSlot] = 0;
            runtime.enemyStunnedCountdown[freeSlot] = 0;
            runtime.dyingCountdown[freeSlot] = 0;
            runtime.powerRecoilDeath[freeSlot] = false;
            runtime.enemyHealth[freeSlot] = runtime.initialHealth(runtime.ENTITY_GEL);
            runtime.enemyFlashCountdown[freeSlot] = 0;
            // SpawnNewEntity sets the new entity's ignore-hits countdown to 1.
            runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
            runtime.enemyRecoilMotion.clear(freeSlot);
            runtime.zolGelMotion.prepareSpawnedGel(freeSlot);
        }
        return gel;
    }

    void handleBowWowTargetContact(RoomEntity source,
                                           BowWowMotion.TargetContact contact,
                                           IntSupplier randomByteSupplier) {
        int targetSlot = contact.slot();
        if (targetSlot < 0 || targetSlot >= runtime.slots.length) {
            return;
        }
        RoomEntity target = runtime.slots[targetSlot];
        if (!target.loaded()) {
            return;
        }

        if (target.type() == runtime.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            if (runtime.entityPrivateState4[targetSlot] == 0 || runtime.dialogActive
                || runtime.bowWowMotion.privateCountdown2(source.slot()) != 0) {
                return;
            }
            runtime.bowWowMotion.setTransitionCountdown(source.slot(), 0);
            runtime.bowWowMotion.setPrivateCountdown2(source.slot(), 0x80);
            runtime.pendingDialogRequests.add(new DialogRequest(1, runtime.DIALOG_BOW_WOW));
            return;
        }

        if (runtime.enemyFlashCountdown[targetSlot] != 0) {
            return;
        }
        runtime.pendingEntityEvents.add(new EntityCombatEvent(
            source.slot(), source.type(), 0, false,
            EntityCombatEvent.SoundChannel.JINGLE, 0x03));
        if (target.type() == runtime.ENTITY_KIKI_THE_MONKEY) {
            runtime.enemyFlashCountdown[targetSlot] = 0x18;
            runtime.entityInertia[targetSlot] = (runtime.entityInertia[targetSlot] + 1) & 0xFF;
            return;
        }
        runtime.handleTerminalEnemyDeath(target, randomByteSupplier);
    }

    void finishBurning(RoomEntity entity) {
        int slot = entity.slot();
        runtime.enemyTransitionCountdown[slot] = 0;
        if (entity.type() == runtime.ENTITY_GIBDO) {
            EntitySpriteDefinition definition = runtime.spriteDefinitionFor(runtime.ENTITY_STALFOS_EVASIVE);
            RoomEntity stalfos = withType(entity, runtime.ENTITY_STALFOS_EVASIVE, definition);
            runtime.slots[slot] = withStatus(stalfos, EntityStatus.ACTIVE);
            runtime.enemyHealth[slot] = runtime.initialHealth(runtime.ENTITY_STALFOS_EVASIVE);
            runtime.enemyFlashCountdown[slot] = 0;
            runtime.enemyIgnoreHitsCountdown[slot] = 0;
            runtime.entityOptions1Override[slot] = -1;
            runtime.enemyPhysicsFlags[slot] = runtime.EVASIVE_PHYSICS_FLAGS;
            runtime.dyingCountdown[slot] = 0;
            runtime.powerRecoilDeath[slot] = false;
            runtime.enemyRecoilMotion.clear(slot);
            runtime.stalfosEvasiveMotion.clear(slot);
            return;
        }

        runtime.enemyPhysicsFlags[slot] = 0x04;
        runtime.dyingCountdown[slot] = 0x1F;
        runtime.powerRecoilDeath[slot] = false;
        runtime.slots[slot] = withDeathPresentation(
            withStatus(entity, EntityStatus.DYING), 3, false);
        runtime.pendingEntityEvents.add(new EntityCombatEvent(
            slot, entity.type(), 0, false,
            EntityCombatEvent.SoundChannel.NOISE, 0x13));
    }

    void decrementSlowTransitionCountdown(int slot, int frameCounter) {
        if ((frameCounter & 0x03) == 0 && runtime.slowTimerInitialized[slot]
            && runtime.slowTransitionCountdown[slot] > 0) {
            runtime.slowTransitionCountdown[slot]--;
        }
    }

    boolean shouldDisappear(RoomEntity entity) { return runtime.enemyInspection.shouldDisappear(entity); }

    void disableEntityWithoutPersistence(int slot) {
        runtime.resetEnemyDropState(slot);
        runtime.sideViewPlatformMotion.clear(slot);
        runtime.sideViewPotMotion.clear(slot);
        runtime.genieMotion.clear(slot);
        if (runtime.genieControllerSlot == slot) {
            runtime.genieControllerSlot = -1;
        }
        runtime.genieActiveState[slot] = 0;
        runtime.geniePrivateState1[slot] = 0;
        runtime.geniePrivateState2[slot] = 0;
        runtime.geniePrivateState3[slot] = 0;
        runtime.geniePrivateState4[slot] = 0;
        runtime.geniePrivateCountdown1[slot] = 0;
        runtime.geniePrivateCountdown2[slot] = 0;
        runtime.genieDirection[slot] = 0;
        runtime.genieDeathInitialized[slot] = false;
        runtime.genieSpeedX[slot] = 0;
        runtime.genieSpeedY[slot] = 0;
        runtime.genieSpeedZ[slot] = 0;
        runtime.genieSpeedXAccumulator[slot] = 0;
        runtime.genieSpeedYAccumulator[slot] = 0;
        runtime.genieSpeedZAccumulator[slot] = 0;
        runtime.secretSeashellMotion.clear(slot);
        runtime.slowTransitionCountdown[slot] = 0;
        runtime.slowTimerInitialized[slot] = false;
        runtime.enemyTransitionCountdown[slot] = 0;
        runtime.bossDeathProducerState[slot] = 0;
        runtime.moldormDestructionTailState[slot] = 0;
        runtime.enemyStunnedCountdown[slot] = 0;
        runtime.dyingCountdown[slot] = 0;
        runtime.powerRecoilDeath[slot] = false;
        runtime.enemyPhysicsFlags[slot] = 0;
        runtime.enemyHealth[slot] = 0;
        runtime.enemyFlashCountdown[slot] = 0;
        runtime.enemyIgnoreHitsCountdown[slot] = 0;
        runtime.chestSpeedY[slot] = 0;
        runtime.chestSpeedYAccumulator[slot] = 0;
        runtime.chestInertia[slot] = 0;
        runtime.chestItemBySlot[slot] = 0;
        runtime.enemyHitboxFlags[slot] = 0;
        runtime.instrumentParticleKind[slot] = 0;
        runtime.instrumentParticleSpeedX[slot] = 0;
        runtime.instrumentParticleSpeedY[slot] = 0;
        runtime.instrumentParticleXAccumulator[slot] = 0;
        runtime.instrumentParticleYAccumulator[slot] = 0;
        runtime.armosKnightMotion.clear(slot);
        runtime.entityGroundStatus[slot] = 0;
        runtime.fallingTargetX[slot] = 0;
        runtime.fallingTargetY[slot] = 0;
        runtime.fallingSpeedX[slot] = 0;
        runtime.fallingSpeedY[slot] = 0;
        runtime.fallingSpeedXAccumulator[slot] = 0;
        runtime.fallingSpeedYAccumulator[slot] = 0;
        runtime.fallingVisualYOffset[slot] = 0;
        runtime.liftedPhase[slot] = 0;
        runtime.liftedSourceDirection[slot] = 0;
        runtime.liftedStateInitialized[slot] = false;
        runtime.thrownDirection[slot] = 0xFF;
        runtime.bombDirection[slot] = 0xFF;
        runtime.bombPrivateState4[slot] = 0;
        runtime.magicPowderState[slot] = 0;
        runtime.magicPowderPrivateState4[slot] = 0;
        runtime.bombPrivateCountdown1[slot] = 0;
        runtime.bombPrivateCountdown3[slot] = 0;
        runtime.ironMaskPrivateState2[slot] = 0;
        runtime.ironMasksMaskSourceHookshotSlot[slot] = 0;
        runtime.liftableRockSmashCountdown[slot] = 0;
        runtime.liftableRockSmashSourceVariant[slot] = 0;
        runtime.liftableRockSmashActive[slot] = false;
        runtime.bombFinalPresentationPending[slot] = false;
        runtime.placedBombMotionInitialized[slot] = false;
        runtime.enemyBombMotionInitialized[slot] = false;
        runtime.ledgeTransitionTimer[slot] = 0;
        runtime.thrownMotionInitialized[slot] = false;
        runtime.baseEntityFlipAttribute[slot] = 0;
        runtime.entityOptions1Override[slot] = -1;
        runtime.enemyRecoilMotion.clear(slot);
        runtime.bomberMotion.clear(slot);
        runtime.madBomberMotion.clear(slot);
        runtime.bombiteMotion.clear(slot);
        runtime.colorShellMotion.clear(slot);
        runtime.butterflyMotion.clear(slot);
        runtime.keeseMotion.clear(slot);
        runtime.roamingEnemyMotion.clear(slot);
        runtime.unmaskedIronMaskMotion.clear(slot);
        runtime.moblinSwordMotion.clear(slot);
        runtime.tektiteMotion.clear(slot);
        runtime.leeverMotion.clear(slot);
        runtime.antiFairyMotion.clear(slot);
        runtime.sparkMotion.clear(slot);
        runtime.zolGelMotion.clear(slot);
        runtime.hidingZolMotion.clear(slot);
        runtime.starMotion.clear(slot);
        runtime.blooperMotion.clear(slot);
        runtime.pincerMotion.clear(slot);
        runtime.bushCrawlerMotion.clear(slot);
        runtime.pokeyMotion.clear(slot);
        runtime.piranhaMotion.clear(slot);
        runtime.zoraMotion.clear(slot);
        runtime.zombieMotion.clear(slot);
        runtime.buzzBlobMotion.clear(slot);
        runtime.sandCrabMotion.clear(slot);
        runtime.dogMotion.clear(slot);
        runtime.mimicMotion.clear(slot);
        runtime.maskedMimicMotion.clear(slot);
        runtime.miniMoldormMotion.clear(slot);
        runtime.moldormMotion.clear(slot);
        runtime.cuccoMotion.clear(slot);
        runtime.spikeTrapMotion.clear(slot);
        runtime.pairoddMotion.clear(slot);
        runtime.pairoddProjectileMotion.clear(slot);
        runtime.playerArrowMotion.clear(slot);
        runtime.magicRodFireballMotion.clear(slot);
        runtime.playerArrowBombArrow[slot] = false;
        if (runtime.latestDroppedBombEntityIndex == slot) {
            runtime.latestDroppedBombEntityIndex = -1;
        }
        if (runtime.latestShotArrowEntityIndex == slot) {
            runtime.latestShotArrowEntityIndex = -1;
        }
        runtime.enemyProjectileMotion.clear(slot);
        runtime.laserMotion.clear(slot);
        runtime.waterTektiteMotion.clear(slot);
        runtime.fishMotion.clear(slot);
        runtime.crowMotion.clear(slot);
        runtime.booBuddyMotion.clear(slot);
        runtime.stalfosAggressiveMotion.clear(slot);
        runtime.stalfosEvasiveMotion.clear(slot);
        runtime.gibdoMotion.clear(slot);
        runtime.goombaMotion.clear(slot);
        runtime.snakeMotion.clear(slot);
        runtime.wizrobeMotion.clear(slot);
        runtime.wizrobeProjectileMotion.clear(slot);
        runtime.peaHatMotion.clear(slot);
        runtime.armosMotion.clear(slot);
        runtime.followingNpcMotion.clear(slot);
        runtime.ghiniMotion.clear(slot);
        runtime.hardHatMotion.clear(slot);
        runtime.polsVoiceMotion.clear(slot);
        runtime.spikedBeetleMotion.clear(slot);
        runtime.bowWowMotion.clear(slot);
        runtime.moblinKingMotion.clear(slot);
        runtime.thrownEntityMotion.clear(slot);
        if (runtime.liftedEntitySlot == slot) {
            runtime.liftedEntitySlot = -1;
            runtime.liftedCarryState = 0;
            runtime.liftedEffectiveDirection = 0;
        }
        runtime.slots[slot] = RoomEntity.disabled(slot);
    }

    boolean decrementEnemyCombatCountdowns(int slot, boolean decrementIgnoreHits) {
        if (runtime.enemyFlashCountdown[slot] > 0) {
            runtime.enemyFlashCountdown[slot]--;
        }
        if (decrementIgnoreHits && !runtime.enemyRecoilMotion.isActive(slot)
            && runtime.enemyIgnoreHitsCountdown[slot] > 0) {
            runtime.enemyIgnoreHitsCountdown[slot]--;
            return true;
        }
        return false;
    }

    void decrementEnemyStatusCountdowns(int slot) {
        if (runtime.enemyTransitionCountdown[slot] > 0) {
            runtime.enemyTransitionCountdown[slot]--;
        }
        if (runtime.enemyStunnedCountdown[slot] > 0) {
            runtime.enemyStunnedCountdown[slot]--;
        }
        if (runtime.dyingCountdown[slot] > 0) {
            runtime.dyingCountdown[slot]--;
        }
        if (runtime.bombPrivateCountdown1[slot] > 0) {
            runtime.bombPrivateCountdown1[slot]--;
        }
        if (runtime.bombPrivateCountdown3[slot] > 0) {
            runtime.bombPrivateCountdown3[slot]--;
        }
        if (runtime.owlPrivateCountdown1[slot] > 0 && runtime.entityTimersInteractive()) {
            runtime.owlPrivateCountdown1[slot]--;
        }
        runtime.hinoxMotion.decrementPrivateCountdown1(slot);
        if (runtime.liftableRockSmashActive[slot] && runtime.liftableRockSmashCountdown[slot] > 0) {
            runtime.liftableRockSmashCountdown[slot]--;
        }
        runtime.blooperMotion.decrementPrivateCountdown3(slot);
        runtime.wingedOctorokMotion.decrementPrivateCountdown2(slot);
    }
}
