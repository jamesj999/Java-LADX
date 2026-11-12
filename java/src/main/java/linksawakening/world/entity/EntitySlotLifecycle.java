package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.List;
import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.motion.BowWowMotion;
import linksawakening.world.entity.motion.FloatingItemMotion;
import linksawakening.world.entity.motion.HeartPiecePickupMotion;
import linksawakening.world.entity.motion.KeyDropPointMotion;
import linksawakening.world.entity.motion.ZolGelMotion;
import linksawakening.world.room.ChestContentsTable;

/** Slot clearing, status countdowns and persistence lifecycle. */
public final class EntitySlotLifecycle {
    private final RoomEntityRuntime runtime;

    EntitySlotLifecycle(RoomEntityRuntime runtime) {
        this.runtime = runtime;
    }

    public int transitionSequenceCounter() {
        return runtime.entityState.transitionSequenceCounter;
    }

    EntityPickupEvent collectIfNeeded(
            int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive) {
        return collectIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, 0, 0);
    }

    EntityPickupEvent collectIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection) {
        return collectIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection, 0);
    }

    EntityPickupEvent collectIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection,
            int linkZ) {
        return collectIfNeeded(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkAirborne,
                linkInteractive,
                linkDirection,
                linkZ,
                false,
                0,
                0,
                0,
                0);
    }

    EntityPickupEvent collectIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection,
            int linkZ,
            boolean swordActive,
            int swordX,
            int swordWidth,
            int swordY,
            int swordHeight) {
        if (!linkInteractive) {
            return null;
        }
        int romDirection = romDirectionForJavaDirection(linkDirection);

        for (int index = runtime.slots.length - 1; index >= 0; index--) {
            RoomEntity entity = runtime.slots[index];
            if (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) {
                runtime.entityState.secretSeashellMotion.ensureInitialized(
                        entity.slot(), runtime.entityState.entityRoomId);
            }
            boolean swordPickup =
                    swordActive && RoomEntityPickupRules.overlapsSword(entity, swordX, swordWidth, swordY, swordHeight);
            boolean floatingType = FloatingItemMotion.isFloatingItem(entity.type());
            boolean floating = isRuntimeFloatingItem(entity);
            if (!entity.loaded()
                    || entity.status() != EntityStatus.ACTIVE
                    || !RoomEntityPickupRules.isPickable(entity.type())
                    || (floatingType && !floating)
                    || (entity.type() == 0x35 && runtime.entityState.heartPiecePickup[entity.slot()] != null)
                    || runtime.entityState.powerUpPickupCountdown[entity.slot()] != 0
                    || runtime.entityState.dropPrivateCountdown1[entity.slot()] > 0
                    || (entity.type() == EntityRomConstants.ENTITY_HEART_CONTAINER
                            && runtime.entityState.enemyTransitionCountdown[entity.slot()] != 0)
                    || (entity.type() == EntityRomConstants.ENTITY_SLEEPY_TOADSTOOL
                            && runtime.entityState.toadstoolPickupSequenceActive[entity.slot()])
                    || (isCommonDroppableType(entity.type())
                            && runtime.entityState.droppablePrivateState3[entity.slot()] != 0)
                    || (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL
                            && (runtime.entityState.secretSeashellMotion.privateState3(entity.slot()) != 0
                                    || runtime.entityState.secretSeashellMotion.privateCountdown1(entity.slot()) != 0))
                    || (!swordPickup
                            && !floating
                            && !RoomEntityPickupRules.collisionCadenceMatches(frameCounter, entity.slot()))
                    || (!swordPickup && !floating && linkAirborne)
                    || (floating
                            && !FloatingItemMotion.linkZAllowsCollection(
                                    runtime.entityState.groundInteractionSideScrolling, linkZ))
                    || !(swordPickup
                            || (floating
                                    ? RoomEntityPickupRules.overlapsFloatingItem(entity, linkEntityX, linkEntityY)
                                    : RoomEntityPickupRules.overlapsLink(entity, linkEntityX, linkEntityY)))) {
                continue;
            }

            int persistentClearMask = entity.type() == EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP
                            && runtime.entityState.chestSwordLevel == 0
                    ? 0
                    : persistentClearMask(entity);
            if (entity.type() == EntityRomConstants.ENTITY_KEY_DROP_POINT) {
                if (runtime.entityState.entityRoomId == 0x80 || entity.spriteVariant() != 0) {
                    runtime.entityState.enemyTransitionCountdown[entity.slot()] =
                            KeyDropPointMotion.TRANSITION_COUNTDOWN;
                } else {
                    runtime.entityState.events.pendingKeyRewardEvents.add(
                            new KeyRewardEvent(entity.slot(), ChestContentsTable.CHEST_SMALL_KEY));
                    clearEntity(entity.slot());
                }
            } else if (entity.type() == 0x35) {
                runtime.entityState.heartPiecePickup[entity.slot()] = new HeartPiecePickupMotion();
                runtime.entityState.enemyTransitionCountdown[entity.slot()] = 5;
                runtime.entityState.pendingMusicTrack = 0x10;
            } else if (entity.type() == EntityRomConstants.ENTITY_HEART_CONTAINER) {
                runtime.entityState.enemyTransitionCountdown[entity.slot()] = 0x70;
            } else if (entity.type() == EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP
                    && runtime.entityState.chestSwordLevel == 0) {
                runtime.startSwordPickup(entity.slot());
            } else if (entity.type() == EntityRomConstants.ENTITY_SLEEPY_TOADSTOOL) {
                runtime.startToadstoolPickup(entity.slot());
            } else if (entity.type() == EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS) {
                runtime.rewards.startInstrumentPickup(entity.slot());
            } else if (entity.type() == 0x33 || entity.type() == 0x34) {
                runtime.entityState.powerUpPickupCountdown[entity.slot()] = 0x30;
                runtime.entityState.entityInteractionCountdown = 0x30;
                runtime.entityState.pendingMusicTrack = 0x27;
            } else if (requiresHeldPickupTransition(entity.type())) {
                runtime.beginLift(entity.slot(), romDirection);
            } else {
                clearEntity(entity.slot());
            }
            return new EntityPickupEvent(
                    entity.slot(), entity.type(), persistentClearMask, floating ? entity.spriteVariant() : -1);
        }
        return null;
    }

    public int clearEntity(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        runtime.slimeEye.clear(slot);
        runtime.pickups.resetEnemyDropState(slot);
        runtime.entityState.sideViewPlatformMotion.clear(slot);
        runtime.entityState.sideViewPotMotion.clear(slot);
        runtime.entityState.genieMotion.clear(slot);
        if (runtime.entityState.genieControllerSlot == slot) {
            runtime.entityState.genieControllerSlot = -1;
        }
        runtime.entityState.genieActiveState[slot] = 0;
        runtime.entityState.geniePrivateState1[slot] = 0;
        runtime.entityState.geniePrivateState2[slot] = 0;
        runtime.entityState.geniePrivateState3[slot] = 0;
        runtime.entityState.geniePrivateState4[slot] = 0;
        runtime.entityState.geniePrivateCountdown1[slot] = 0;
        runtime.entityState.geniePrivateCountdown2[slot] = 0;
        runtime.entityState.genieDirection[slot] = 0;
        runtime.entityState.genieDeathInitialized[slot] = false;
        runtime.entityState.genieSpeedX[slot] = 0;
        runtime.entityState.genieSpeedY[slot] = 0;
        runtime.entityState.genieSpeedZ[slot] = 0;
        runtime.entityState.genieSpeedXAccumulator[slot] = 0;
        runtime.entityState.genieSpeedYAccumulator[slot] = 0;
        runtime.entityState.genieSpeedZAccumulator[slot] = 0;
        RoomEntity entity = runtime.slots[slot];
        runtime.bombFinalPresentationPending[slot] = false;
        runtime.entityState.enemyHitboxFlags[slot] = 0;
        runtime.entityState.playerArrowBombArrow[slot] = false;
        runtime.entityState.bombPrivateCountdown1[slot] = 0;
        runtime.entityState.bombPrivateCountdown3[slot] = 0;
        runtime.entityState.magicPowderState[slot] = 0;
        runtime.entityState.magicPowderPrivateState4[slot] = 0;
        runtime.entityState.secretSeashellMotion.clear(slot);
        runtime.entityState.entityPrivateState4[slot] = 0;
        runtime.entityState.entityInertia[slot] = 0;
        runtime.entityState.musicalNoteInertia[slot] = 0;
        runtime.entityState.musicalNoteSpeedX[slot] = 0;
        runtime.entityState.musicalNoteSpeedY[slot] = 0;
        runtime.entityState.entityUnknownJ[slot] = 0;
        runtime.entityState.unmaskedIronMaskMotion.setMaskRemovalState(slot, 0);
        runtime.entityState.unmaskedIronMaskMotion.setMaskSourceHookshotSlot(slot, 0);
        runtime.entityState.liftableRockSmashCountdown[slot] = 0;
        runtime.entityState.liftableRockSmashSourceVariant[slot] = 0;
        runtime.entityState.liftableRockSmashActive[slot] = false;
        if (runtime.entityState.latestDroppedBombEntityIndex == slot) {
            runtime.entityState.latestDroppedBombEntityIndex = -1;
        }
        if (runtime.entityState.latestShotArrowEntityIndex == slot) {
            runtime.entityState.latestShotArrowEntityIndex = -1;
        }
        runtime.entityState.bombiteMotion.clear(slot);
        runtime.entityState.blooperMotion.clear(slot);
        runtime.entityState.pincerMotion.clear(slot);
        runtime.entityState.bushCrawlerMotion.clear(slot);
        runtime.entityState.pokeyMotion.clear(slot);
        runtime.entityState.zoraMotion.clear(slot);
        runtime.entityState.zombieMotion.clear(slot);
        runtime.entityState.buzzBlobMotion.clear(slot);
        runtime.entityState.sandCrabMotion.clear(slot);
        runtime.entityState.dogMotion.clear(slot);
        runtime.entityState.urchinMotion.clear(slot);
        runtime.entityState.witchRatMotion.clear(slot);
        runtime.entityState.mimicMotion.clear(slot);
        runtime.entityState.maskedMimicMotion.clear(slot);
        runtime.entityState.miniMoldormMotion.clear(slot);
        runtime.entityState.moldormMotion.clear(slot);
        if (!entity.loaded()) {
            return 0;
        }
        runtime.entityState.slowTransitionCountdown[slot] = 0;
        runtime.entityState.slowTimerInitialized[slot] = false;
        runtime.entityState.enemyTransitionCountdown[slot] = 0;
        runtime.entityState.enemyStunnedCountdown[slot] = 0;
        runtime.entityState.dyingCountdown[slot] = 0;
        runtime.entityState.powerRecoilDeath[slot] = false;
        runtime.entityState.enemyPhysicsFlags[slot] = 0;
        runtime.entityState.enemyHealth[slot] = 0;
        runtime.entityState.enemyHealthGroup[slot] = 0;
        runtime.entityState.enemyFlashCountdown[slot] = 0;
        runtime.entityState.enemyIgnoreHitsCountdown[slot] = 0;
        runtime.entityState.chestSpeedY[slot] = 0;
        runtime.entityState.chestSpeedYAccumulator[slot] = 0;
        runtime.entityState.chestInertia[slot] = 0;
        runtime.entityState.chestItemBySlot[slot] = 0;
        runtime.entityState.owlPrivateCountdown1[slot] = 0;
        runtime.entityState.entityGroundStatus[slot] = 0;
        runtime.entityState.fallingTargetX[slot] = 0;
        runtime.entityState.fallingTargetY[slot] = 0;
        runtime.entityState.fallingSpeedX[slot] = 0;
        runtime.entityState.fallingSpeedY[slot] = 0;
        runtime.entityState.fallingSpeedXAccumulator[slot] = 0;
        runtime.entityState.fallingSpeedYAccumulator[slot] = 0;
        runtime.entityState.fallingVisualYOffset[slot] = 0;
        runtime.entityState.liftedPhase[slot] = 0;
        runtime.entityState.liftedSourceDirection[slot] = 0;
        runtime.entityState.liftedStateInitialized[slot] = false;
        runtime.entityState.thrownDirection[slot] = 0xFF;
        runtime.entityState.bombDirection[slot] = 0xFF;
        runtime.entityState.bombPrivateState4[slot] = 0;
        runtime.entityState.magicPowderState[slot] = 0;
        runtime.entityState.magicPowderPrivateState4[slot] = 0;
        runtime.entityState.bombPrivateCountdown1[slot] = 0;
        runtime.entityState.bombPrivateCountdown3[slot] = 0;
        runtime.entityState.placedBombMotionInitialized[slot] = false;
        runtime.entityState.ledgeTransitionTimer[slot] = 0;
        runtime.entityState.thrownMotionInitialized[slot] = false;
        runtime.entityState.hookshotChainMotion.clear(slot);
        runtime.entityState.hookshotBridgeMotion.clear(slot);
        runtime.entityState.boomerangMotion.clear(slot);
        runtime.entityState.magicRodFireballMotion.clear(slot);
        runtime.entityState.swordBeamMotion.clear(slot);
        if (entity.type() == EntityRomConstants.ENTITY_HOOKSHOT_CHAIN) {
            runtime.entityState.hookshotChainOam = List.of();
        }
        runtime.entityState.baseEntityFlipAttribute[slot] = 0;
        runtime.entityState.entityOptions1Override[slot] = -1;
        runtime.entityState.enemyRecoilMotion.clear(slot);
        runtime.entityState.colorShellMotion.clear(slot);
        runtime.entityState.butterflyMotion.clear(slot);
        runtime.entityState.keeseMotion.clear(slot);
        runtime.entityState.roamingEnemyMotion.clear(slot);
        runtime.entityState.unmaskedIronMaskMotion.clear(slot);
        runtime.entityState.moblinSwordMotion.clear(slot);
        runtime.entityState.playerArrowMotion.clear(slot);
        runtime.entityState.tektiteMotion.clear(slot);
        runtime.entityState.leeverMotion.clear(slot);
        runtime.entityState.antiFairyMotion.clear(slot);
        runtime.entityState.sparkMotion.clear(slot);
        runtime.entityState.zolGelMotion.clear(slot);
        runtime.entityState.pokeyMotion.clear(slot);
        runtime.entityState.hidingZolMotion.clear(slot);
        runtime.entityState.starMotion.clear(slot);
        runtime.entityState.blooperMotion.clear(slot);
        runtime.entityState.pincerMotion.clear(slot);
        runtime.entityState.spikeTrapMotion.clear(slot);
        runtime.entityState.pairoddMotion.clear(slot);
        runtime.entityState.pairoddProjectileMotion.clear(slot);
        runtime.entityState.enemyProjectileMotion.clear(slot);
        runtime.entityState.wingedOctorokMotion.clear(slot);
        runtime.entityState.bushCrawlerMotion.clear(slot);
        runtime.entityState.cuccoMotion.clear(slot);
        runtime.entityState.giantGopongaMotion.clear(slot);
        runtime.entityState.gopongaProjectileMotion.clear(slot);
        runtime.entityState.zoraMotion.clear(slot);
        runtime.entityState.zombieMotion.clear(slot);
        runtime.entityState.buzzBlobMotion.clear(slot);
        runtime.entityState.sandCrabMotion.clear(slot);
        runtime.entityState.dogMotion.clear(slot);
        runtime.entityState.urchinMotion.clear(slot);
        runtime.entityState.witchRatMotion.clear(slot);
        runtime.entityState.wingedSwordAttackThisFrame[slot] = false;
        runtime.entityState.wingedStateTwoThisFrame[slot] = false;
        runtime.entityState.laserMotion.clear(slot);
        runtime.entityState.waterTektiteMotion.clear(slot);
        runtime.entityState.fishMotion.clear(slot);
        runtime.entityState.crowMotion.clear(slot);
        runtime.entityState.booBuddyMotion.clear(slot);
        runtime.entityState.stalfosAggressiveMotion.clear(slot);
        runtime.entityState.stalfosEvasiveMotion.clear(slot);
        runtime.entityState.threeOfAKindMotion.clear(slot);
        runtime.entityState.gibdoMotion.clear(slot);
        runtime.entityState.likeLikeMotion.clear(slot);
        runtime.entityState.goombaMotion.clear(slot);
        runtime.entityState.snakeMotion.clear(slot);
        runtime.entityState.wizrobeMotion.clear(slot);
        runtime.entityState.wizrobeProjectileMotion.clear(slot);
        runtime.entityState.peaHatMotion.clear(slot);
        runtime.entityState.armosMotion.clear(slot);
        runtime.entityState.ghiniMotion.clear(slot);
        runtime.entityState.hardHatMotion.clear(slot);
        runtime.entityState.madBomberMotion.clear(slot);
        runtime.entityState.followingNpcMotion.clear(slot);
        runtime.entityState.bowWowMotion.clear(slot);
        runtime.entityState.moblinKingMotion.clear(slot);
        runtime.entityState.thrownEntityMotion.clear(slot);
        if (runtime.entityState.liftedEntitySlot == slot) {
            runtime.entityState.liftedEntitySlot = -1;
            runtime.entityState.liftedCarryState = 0;
            runtime.entityState.liftedEffectiveDirection = 0;
        }
        runtime.slots[slot] = RoomEntity.disabled(slot);
        runtime.slotProperties.updatePincerBodyOam();
        return entity.sourceLoadOrder() >= 0 && entity.sourceLoadOrder() < 8 ? 1 << entity.sourceLoadOrder() : 0;
    }

    public RoomEntity applyZolSplit(RoomEntity original, RoomEntity updated, ZolGelMotion.Split split) {
        EntitySpriteDefinition gelDefinition =
                runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_GEL);
        int gelVariant = gelDefinition.supported() ? 0 : -1;
        int slot = original.slot();
        RoomEntity gel = new RoomEntity(
                slot,
                original.sourceLoadOrder(),
                EntityRomConstants.ENTITY_GEL,
                (split.originalX() - 4) & 0xFF,
                split.originalY(),
                EntityStatus.ACTIVE,
                gelDefinition,
                gelVariant,
                original.entityFlipAttribute(),
                original.spriteTileOffset(),
                split.originalZ(),
                -1,
                false);
        // ConfigureNewEntity ($03:485B) reloads these for the new type.
        runtime.entityState.enemyPhysicsFlags[slot] = initialPhysicsFlags(EntityRomConstants.ENTITY_GEL);
        runtime.entityState.enemyHitboxFlags[slot] = initialHitboxFlags(EntityRomConstants.ENTITY_GEL);
        runtime.entityState.entityOptions1Override[slot] = -1;
        runtime.entityState.enemyTransitionCountdown[slot] = 0;
        runtime.entityState.enemyStunnedCountdown[slot] = 0;
        runtime.entityState.enemyHealthGroup[slot] = runtime.enemyCombatTables == null
                ? 0
                : runtime.enemyCombatTables.healthGroup(EntityRomConstants.ENTITY_GEL);
        runtime.entityState.enemyHealth[slot] = runtime.initialHealth(EntityRomConstants.ENTITY_GEL);
        runtime.entityState.enemyFlashCountdown[slot] = 0;
        runtime.entityState.enemyIgnoreHitsCountdown[slot] = 0;
        runtime.entityState.dyingCountdown[slot] = 0;
        runtime.entityState.powerRecoilDeath[slot] = false;
        runtime.entityState.enemyRecoilMotion.clear(slot);

        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot >= 0) {
            runtime.entityState.baseEntityFlipAttribute[freeSlot] =
                    runtime.entityState.baseEntityFlipAttribute[original.slot()];
            RoomEntity spawnedGel = new RoomEntity(
                    freeSlot,
                    original.sourceLoadOrder(),
                    EntityRomConstants.ENTITY_GEL,
                    (split.originalX() + 8) & 0xFF,
                    split.originalY(),
                    EntityStatus.ACTIVE,
                    gelDefinition,
                    gelVariant,
                    original.entityFlipAttribute(),
                    original.spriteTileOffset(),
                    split.originalZ(),
                    -1,
                    false);
            runtime.slots[freeSlot] = spawnedGel;
            runtime.initializeHealthGroup(freeSlot);
            runtime.entityState.enemyPhysicsFlags[freeSlot] = initialPhysicsFlags(EntityRomConstants.ENTITY_GEL);
            runtime.entityState.enemyHitboxFlags[freeSlot] = initialHitboxFlags(EntityRomConstants.ENTITY_GEL);
            runtime.entityState.entityOptions1Override[freeSlot] = -1;
            runtime.entityState.enemyTransitionCountdown[freeSlot] = 0;
            runtime.entityState.enemyStunnedCountdown[freeSlot] = 0;
            runtime.entityState.dyingCountdown[freeSlot] = 0;
            runtime.entityState.powerRecoilDeath[freeSlot] = false;
            runtime.entityState.enemyHealth[freeSlot] = runtime.initialHealth(EntityRomConstants.ENTITY_GEL);
            runtime.entityState.enemyFlashCountdown[freeSlot] = 0;
            // SpawnNewEntity sets the new entity's ignore-hits countdown to 1.
            runtime.entityState.enemyIgnoreHitsCountdown[freeSlot] = 1;
            runtime.entityState.enemyRecoilMotion.clear(freeSlot);
            runtime.entityState.zolGelMotion.prepareSpawnedGel(freeSlot);
        }
        return gel;
    }

    void handleBowWowTargetContact(
            RoomEntity source, BowWowMotion.TargetContact contact, IntSupplier randomByteSupplier) {
        int targetSlot = contact.slot();
        if (targetSlot < 0 || targetSlot >= runtime.slots.length) {
            return;
        }
        RoomEntity target = runtime.slots[targetSlot];
        if (!target.loaded()) {
            return;
        }

        if (target.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            if (runtime.entityState.entityPrivateState4[targetSlot] == 0
                    || runtime.entityState.linkState.dialogActive
                    || runtime.entityState.bowWowMotion.privateCountdown2(source.slot()) != 0) {
                return;
            }
            runtime.entityState.bowWowMotion.setTransitionCountdown(source.slot(), 0);
            runtime.entityState.bowWowMotion.setPrivateCountdown2(source.slot(), 0x80);
            runtime.entityState.events.pendingDialogRequests.add(
                    new DialogRequest(1, EntityRomConstants.DIALOG_BOW_WOW));
            return;
        }

        if (runtime.entityState.enemyFlashCountdown[targetSlot] != 0) {
            return;
        }
        runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                source.slot(), source.type(), 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x03));
        if (target.type() == EntityRomConstants.ENTITY_KIKI_THE_MONKEY) {
            runtime.entityState.enemyFlashCountdown[targetSlot] = 0x18;
            runtime.entityState.entityInertia[targetSlot] = (runtime.entityState.entityInertia[targetSlot] + 1) & 0xFF;
            return;
        }
        runtime.bossRewards.handleTerminalEnemyDeath(target, randomByteSupplier);
    }

    void finishBurning(RoomEntity entity) {
        int slot = entity.slot();
        runtime.entityState.enemyTransitionCountdown[slot] = 0;
        if (entity.type() == EntityRomConstants.ENTITY_GIBDO) {
            EntitySpriteDefinition definition =
                    runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_STALFOS_EVASIVE);
            RoomEntity stalfos = withType(entity, EntityRomConstants.ENTITY_STALFOS_EVASIVE, definition);
            runtime.slots[slot] = withStatus(stalfos, EntityStatus.ACTIVE);
            runtime.initializeHealthGroup(slot); // $03:4C89 reconfigures attributes/health.
            runtime.entityState.enemyHealth[slot] = runtime.initialHealth(EntityRomConstants.ENTITY_STALFOS_EVASIVE);
            runtime.entityState.enemyFlashCountdown[slot] = 0;
            runtime.entityState.enemyIgnoreHitsCountdown[slot] = 0;
            runtime.entityState.entityOptions1Override[slot] = -1;
            runtime.entityState.enemyPhysicsFlags[slot] = EntityRomConstants.EVASIVE_PHYSICS_FLAGS;
            runtime.entityState.dyingCountdown[slot] = 0;
            runtime.entityState.powerRecoilDeath[slot] = false;
            runtime.entityState.enemyRecoilMotion.clear(slot);
            runtime.entityState.stalfosEvasiveMotion.clear(slot);
            return;
        }

        runtime.entityState.enemyPhysicsFlags[slot] = 0x04;
        runtime.entityState.dyingCountdown[slot] = 0x1F;
        runtime.entityState.powerRecoilDeath[slot] = false;
        runtime.slots[slot] = withDeathPresentation(withStatus(entity, EntityStatus.DYING), 3, false);
        runtime.entityState.events.pendingEntityEvents.add(
                new EntityCombatEvent(slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
    }

    void decrementSlowTransitionCountdown(int slot, int frameCounter) {
        if ((frameCounter & 0x03) == 0
                && runtime.entityState.slowTimerInitialized[slot]
                && runtime.entityState.slowTransitionCountdown[slot] > 0) {
            runtime.entityState.slowTransitionCountdown[slot]--;
        }
    }

    boolean shouldDisappear(RoomEntity entity) {
        return runtime.enemyInspection.shouldDisappear(entity);
    }

    public void disableEntityWithoutPersistence(int slot) {
        runtime.slimeEye.clear(slot);
        runtime.pickups.resetEnemyDropState(slot);
        runtime.entityState.sideViewPlatformMotion.clear(slot);
        runtime.entityState.sideViewPotMotion.clear(slot);
        runtime.entityState.genieMotion.clear(slot);
        if (runtime.entityState.genieControllerSlot == slot) {
            runtime.entityState.genieControllerSlot = -1;
        }
        runtime.entityState.genieActiveState[slot] = 0;
        runtime.entityState.geniePrivateState1[slot] = 0;
        runtime.entityState.geniePrivateState2[slot] = 0;
        runtime.entityState.geniePrivateState3[slot] = 0;
        runtime.entityState.geniePrivateState4[slot] = 0;
        runtime.entityState.geniePrivateCountdown1[slot] = 0;
        runtime.entityState.geniePrivateCountdown2[slot] = 0;
        runtime.entityState.genieDirection[slot] = 0;
        runtime.entityState.genieDeathInitialized[slot] = false;
        runtime.entityState.genieSpeedX[slot] = 0;
        runtime.entityState.genieSpeedY[slot] = 0;
        runtime.entityState.genieSpeedZ[slot] = 0;
        runtime.entityState.genieSpeedXAccumulator[slot] = 0;
        runtime.entityState.genieSpeedYAccumulator[slot] = 0;
        runtime.entityState.genieSpeedZAccumulator[slot] = 0;
        runtime.entityState.secretSeashellMotion.clear(slot);
        runtime.entityState.slowTransitionCountdown[slot] = 0;
        runtime.entityState.slowTimerInitialized[slot] = false;
        runtime.entityState.enemyTransitionCountdown[slot] = 0;
        runtime.entityState.bossDeathProducerState[slot] = 0;
        runtime.entityState.moldormMotion.setDestructionTailState(slot, 0);
        runtime.entityState.enemyStunnedCountdown[slot] = 0;
        runtime.entityState.dyingCountdown[slot] = 0;
        runtime.entityState.powerRecoilDeath[slot] = false;
        runtime.entityState.enemyPhysicsFlags[slot] = 0;
        runtime.entityState.enemyHealth[slot] = 0;
        runtime.entityState.enemyHealthGroup[slot] = 0;
        runtime.entityState.enemyFlashCountdown[slot] = 0;
        runtime.entityState.enemyIgnoreHitsCountdown[slot] = 0;
        runtime.entityState.chestSpeedY[slot] = 0;
        runtime.entityState.chestSpeedYAccumulator[slot] = 0;
        runtime.entityState.chestInertia[slot] = 0;
        runtime.entityState.chestItemBySlot[slot] = 0;
        runtime.entityState.enemyHitboxFlags[slot] = 0;
        runtime.entityState.instrumentParticleKind[slot] = 0;
        runtime.entityState.instrumentParticleSpeedX[slot] = 0;
        runtime.entityState.instrumentParticleSpeedY[slot] = 0;
        runtime.entityState.instrumentParticleXAccumulator[slot] = 0;
        runtime.entityState.instrumentParticleYAccumulator[slot] = 0;
        runtime.entityState.armosKnightMotion.clear(slot);
        runtime.entityState.entityGroundStatus[slot] = 0;
        runtime.entityState.fallingTargetX[slot] = 0;
        runtime.entityState.fallingTargetY[slot] = 0;
        runtime.entityState.fallingSpeedX[slot] = 0;
        runtime.entityState.fallingSpeedY[slot] = 0;
        runtime.entityState.fallingSpeedXAccumulator[slot] = 0;
        runtime.entityState.fallingSpeedYAccumulator[slot] = 0;
        runtime.entityState.fallingVisualYOffset[slot] = 0;
        runtime.entityState.liftedPhase[slot] = 0;
        runtime.entityState.liftedSourceDirection[slot] = 0;
        runtime.entityState.liftedStateInitialized[slot] = false;
        runtime.entityState.thrownDirection[slot] = 0xFF;
        runtime.entityState.bombDirection[slot] = 0xFF;
        runtime.entityState.bombPrivateState4[slot] = 0;
        runtime.entityState.magicPowderState[slot] = 0;
        runtime.entityState.magicPowderPrivateState4[slot] = 0;
        runtime.entityState.bombPrivateCountdown1[slot] = 0;
        runtime.entityState.bombPrivateCountdown3[slot] = 0;
        runtime.entityState.unmaskedIronMaskMotion.setMaskRemovalState(slot, 0);
        runtime.entityState.unmaskedIronMaskMotion.setMaskSourceHookshotSlot(slot, 0);
        runtime.entityState.liftableRockSmashCountdown[slot] = 0;
        runtime.entityState.liftableRockSmashSourceVariant[slot] = 0;
        runtime.entityState.liftableRockSmashActive[slot] = false;
        runtime.bombFinalPresentationPending[slot] = false;
        runtime.entityState.placedBombMotionInitialized[slot] = false;
        runtime.entityState.enemyBombMotionInitialized[slot] = false;
        runtime.entityState.ledgeTransitionTimer[slot] = 0;
        runtime.entityState.thrownMotionInitialized[slot] = false;
        runtime.entityState.baseEntityFlipAttribute[slot] = 0;
        runtime.entityState.entityOptions1Override[slot] = -1;
        runtime.entityState.enemyRecoilMotion.clear(slot);
        runtime.entityState.bomberMotion.clear(slot);
        runtime.entityState.madBomberMotion.clear(slot);
        runtime.entityState.bombiteMotion.clear(slot);
        runtime.entityState.colorShellMotion.clear(slot);
        runtime.entityState.butterflyMotion.clear(slot);
        runtime.entityState.keeseMotion.clear(slot);
        runtime.entityState.roamingEnemyMotion.clear(slot);
        runtime.entityState.unmaskedIronMaskMotion.clear(slot);
        runtime.entityState.moblinSwordMotion.clear(slot);
        runtime.entityState.tektiteMotion.clear(slot);
        runtime.entityState.leeverMotion.clear(slot);
        runtime.entityState.antiFairyMotion.clear(slot);
        runtime.entityState.sparkMotion.clear(slot);
        runtime.entityState.zolGelMotion.clear(slot);
        runtime.entityState.hidingZolMotion.clear(slot);
        runtime.entityState.starMotion.clear(slot);
        runtime.entityState.blooperMotion.clear(slot);
        runtime.entityState.pincerMotion.clear(slot);
        runtime.entityState.bushCrawlerMotion.clear(slot);
        runtime.entityState.pokeyMotion.clear(slot);
        runtime.entityState.piranhaMotion.clear(slot);
        runtime.entityState.zoraMotion.clear(slot);
        runtime.entityState.zombieMotion.clear(slot);
        runtime.entityState.buzzBlobMotion.clear(slot);
        runtime.entityState.sandCrabMotion.clear(slot);
        runtime.entityState.dogMotion.clear(slot);
        runtime.entityState.mimicMotion.clear(slot);
        runtime.entityState.maskedMimicMotion.clear(slot);
        runtime.entityState.miniMoldormMotion.clear(slot);
        runtime.entityState.moldormMotion.clear(slot);
        runtime.entityState.cuccoMotion.clear(slot);
        runtime.entityState.spikeTrapMotion.clear(slot);
        runtime.entityState.pairoddMotion.clear(slot);
        runtime.entityState.pairoddProjectileMotion.clear(slot);
        runtime.entityState.playerArrowMotion.clear(slot);
        runtime.entityState.magicRodFireballMotion.clear(slot);
        runtime.entityState.playerArrowBombArrow[slot] = false;
        if (runtime.entityState.latestDroppedBombEntityIndex == slot) {
            runtime.entityState.latestDroppedBombEntityIndex = -1;
        }
        if (runtime.entityState.latestShotArrowEntityIndex == slot) {
            runtime.entityState.latestShotArrowEntityIndex = -1;
        }
        runtime.entityState.enemyProjectileMotion.clear(slot);
        runtime.entityState.laserMotion.clear(slot);
        runtime.entityState.waterTektiteMotion.clear(slot);
        runtime.entityState.fishMotion.clear(slot);
        runtime.entityState.crowMotion.clear(slot);
        runtime.entityState.booBuddyMotion.clear(slot);
        runtime.entityState.stalfosAggressiveMotion.clear(slot);
        runtime.entityState.stalfosEvasiveMotion.clear(slot);
        runtime.entityState.gibdoMotion.clear(slot);
        runtime.entityState.goombaMotion.clear(slot);
        runtime.entityState.snakeMotion.clear(slot);
        runtime.entityState.wizrobeMotion.clear(slot);
        runtime.entityState.wizrobeProjectileMotion.clear(slot);
        runtime.entityState.peaHatMotion.clear(slot);
        runtime.entityState.armosMotion.clear(slot);
        runtime.entityState.followingNpcMotion.clear(slot);
        runtime.entityState.ghiniMotion.clear(slot);
        runtime.entityState.hardHatMotion.clear(slot);
        runtime.entityState.polsVoiceMotion.clear(slot);
        runtime.entityState.spikedBeetleMotion.clear(slot);
        runtime.entityState.bowWowMotion.clear(slot);
        runtime.entityState.moblinKingMotion.clear(slot);
        runtime.entityState.thrownEntityMotion.clear(slot);
        if (runtime.entityState.liftedEntitySlot == slot) {
            runtime.entityState.liftedEntitySlot = -1;
            runtime.entityState.liftedCarryState = 0;
            runtime.entityState.liftedEffectiveDirection = 0;
        }
        runtime.slots[slot] = RoomEntity.disabled(slot);
    }

    boolean decrementEnemyCombatCountdowns(int slot, boolean decrementIgnoreHits) {
        if (runtime.entityState.enemyFlashCountdown[slot] > 0) {
            runtime.entityState.enemyFlashCountdown[slot]--;
        }
        if (decrementIgnoreHits
                && !runtime.entityState.enemyRecoilMotion.isActive(slot)
                && runtime.entityState.enemyIgnoreHitsCountdown[slot] > 0) {
            runtime.entityState.enemyIgnoreHitsCountdown[slot]--;
            return true;
        }
        return false;
    }

    void decrementEnemyStatusCountdowns(int slot) {
        if (runtime.entityState.enemyTransitionCountdown[slot] > 0) {
            runtime.entityState.enemyTransitionCountdown[slot]--;
        }
        if (runtime.entityState.enemyStunnedCountdown[slot] > 0) {
            runtime.entityState.enemyStunnedCountdown[slot]--;
        }
        if (runtime.entityState.dyingCountdown[slot] > 0) {
            runtime.entityState.dyingCountdown[slot]--;
        }
        if (runtime.entityState.bombPrivateCountdown1[slot] > 0) {
            runtime.entityState.bombPrivateCountdown1[slot]--;
        }
        if (runtime.entityState.bombPrivateCountdown3[slot] > 0) {
            runtime.entityState.bombPrivateCountdown3[slot]--;
        }
        if (runtime.entityState.owlPrivateCountdown1[slot] > 0
                && runtime.backgroundMechanics.entityTimersInteractive()) {
            runtime.entityState.owlPrivateCountdown1[slot]--;
        }
        runtime.entityState.hinoxMotion.decrementPrivateCountdown1(slot);
        if (runtime.entityState.liftableRockSmashActive[slot]
                && runtime.entityState.liftableRockSmashCountdown[slot] > 0) {
            runtime.entityState.liftableRockSmashCountdown[slot]--;
        }
        runtime.entityState.blooperMotion.decrementPrivateCountdown3(slot);
        runtime.entityState.wingedOctorokMotion.decrementPrivateCountdown2(slot);
    }

    public void requestPersistentClearMask(int mask) {
        runtime.entityState.pendingClearedEntityMask |= mask;
    }

    public void delayInteractions(int countdown) {
        runtime.entityState.entityInteractionCountdown = countdown;
    }

    public void recordGelClinging(boolean clinging) {
        runtime.entityState.gelClingingToLink |= clinging;
    }

    public void setDroppedItemBySlot(int slot, int value) {
        runtime.entityState.droppedItemBySlot[slot] = value;
    }

    public void setDyingCountdown(int slot, int value) {
        runtime.entityState.dyingCountdown[slot] = value;
    }

    public void setDropPrivateCountdown3(int slot, int value) {
        runtime.entityState.dropPrivateCountdown3[slot] = value;
    }

    public int warpState(int slot) {
        return runtime.entityState.warpState[slot];
    }

    public void setWarpState(int slot, int value) {
        runtime.entityState.warpState[slot] = value;
    }

    public int warpTransitionCountdown(int slot) {
        return runtime.entityState.warpTransitionCountdown[slot];
    }

    public void setWarpTransitionCountdown(int slot, int value) {
        runtime.entityState.warpTransitionCountdown[slot] = value;
    }

    public int droppablePrivateState3(int slot) {
        return runtime.entityState.droppablePrivateState3[slot];
    }
}
