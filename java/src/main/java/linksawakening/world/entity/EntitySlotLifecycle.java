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
    private final EntitySlotLifecycleServices runtime;

    EntitySlotLifecycle(EntitySlotLifecycleServices runtime) {
        this.runtime = runtime;
    }

    public int transitionSequenceCounter() {
        return runtime.roomVariables().readTransitionSequenceCounter();
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

        for (int index = runtime.slots().length - 1; index >= 0; index--) {
            RoomEntity entity = runtime.slots()[index];
            if (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) {
                runtime.motionSet()
                        .secretSeashellMotion
                        .ensureInitialized(
                                entity.slot(), runtime.roomVariables().readEntityRoomId());
            }
            boolean swordPickup =
                    swordActive && RoomEntityPickupRules.overlapsSword(entity, swordX, swordWidth, swordY, swordHeight);
            boolean floatingType = FloatingItemMotion.isFloatingItem(entity.type());
            boolean floating = isRuntimeFloatingItem(entity);
            if (!entity.loaded()
                    || entity.status() != EntityStatus.ACTIVE
                    || !RoomEntityPickupRules.isPickable(entity.type())
                    || (floatingType && !floating)
                    || (entity.type() == 0x35 && runtime.pickups().readHeartPiecePickup(entity.slot()) != null)
                    || runtime.rewards().readPowerUpPickupCountdown(entity.slot()) != 0
                    || runtime.bossRewards().readDropPrivateCountdown1(entity.slot()) > 0
                    || (entity.type() == EntityRomConstants.ENTITY_HEART_CONTAINER
                            && runtime.physicsTables().readEnemyTransitionCountdown(entity.slot()) != 0)
                    || (entity.type() == EntityRomConstants.ENTITY_SLEEPY_TOADSTOOL
                            && runtime.rewards().readToadstoolPickupSequenceActive(entity.slot()))
                    || (isCommonDroppableType(entity.type())
                            && runtime.bossRewards().readDroppablePrivateState3(entity.slot()) != 0)
                    || (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL
                            && (runtime.motionSet().secretSeashellMotion.privateState3(entity.slot()) != 0
                                    || runtime.motionSet().secretSeashellMotion.privateCountdown1(entity.slot()) != 0))
                    || (!swordPickup
                            && !floating
                            && !RoomEntityPickupRules.collisionCadenceMatches(frameCounter, entity.slot()))
                    || (!swordPickup && !floating && linkAirborne)
                    || (floating
                            && !FloatingItemMotion.linkZAllowsCollection(
                                    runtime.physicsTables().readGroundInteractionSideScrolling(), linkZ))
                    || !(swordPickup
                            || (floating
                                    ? RoomEntityPickupRules.overlapsFloatingItem(entity, linkEntityX, linkEntityY)
                                    : RoomEntityPickupRules.overlapsLink(entity, linkEntityX, linkEntityY)))) {
                continue;
            }

            int persistentClearMask = entity.type() == EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP
                            && runtime.pickups().readChestSwordLevel() == 0
                    ? 0
                    : persistentClearMask(entity);
            if (entity.type() == EntityRomConstants.ENTITY_KEY_DROP_POINT) {
                if (runtime.roomVariables().readEntityRoomId() == 0x80 || entity.spriteVariant() != 0) {
                    runtime.physicsTables()
                            .writeEnemyTransitionCountdown(entity.slot(), KeyDropPointMotion.TRANSITION_COUNTDOWN);
                } else {
                    runtime.events()
                            .pendingKeyRewardEvents
                            .add(new KeyRewardEvent(entity.slot(), ChestContentsTable.CHEST_SMALL_KEY));
                    clearEntity(entity.slot());
                }
            } else if (entity.type() == 0x35) {
                runtime.pickups().writeHeartPiecePickup(entity.slot(), new HeartPiecePickupMotion());
                runtime.physicsTables().writeEnemyTransitionCountdown(entity.slot(), 5);
                runtime.presentationState().writePendingMusicTrack(0x10);
            } else if (entity.type() == EntityRomConstants.ENTITY_HEART_CONTAINER) {
                runtime.physicsTables().writeEnemyTransitionCountdown(entity.slot(), 0x70);
            } else if (entity.type() == EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP
                    && runtime.pickups().readChestSwordLevel() == 0) {
                runtime.startSwordPickup(entity.slot());
            } else if (entity.type() == EntityRomConstants.ENTITY_SLEEPY_TOADSTOOL) {
                runtime.startToadstoolPickup(entity.slot());
            } else if (entity.type() == EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS) {
                runtime.rewards().startInstrumentPickup(entity.slot());
            } else if (entity.type() == 0x33 || entity.type() == 0x34) {
                runtime.rewards().writePowerUpPickupCountdown(entity.slot(), 0x30);
                runtime.roomVariables().writeEntityInteractionCountdown(0x30);
                runtime.presentationState().writePendingMusicTrack(0x27);
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
        if (slot < 0 || slot >= runtime.slots().length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        runtime.slimeEye().clear(slot);
        runtime.pickups().resetEnemyDropState(slot);
        runtime.motionSet().sideViewPlatformMotion.clear(slot);
        runtime.motionSet().sideViewPotMotion.clear(slot);
        runtime.motionSet().genieMotion.clear(slot);
        if (runtime.genie().genieControllerSlot() == slot) {
            runtime.genie().setGenieControllerSlot(-1);
        }
        runtime.genie().setGenieActiveState(slot, 0);
        runtime.genie().setGeniePrivateState1(slot, 0);
        runtime.genie().setGeniePrivateState2(slot, 0);
        runtime.genie().setGeniePrivateState3(slot, 0);
        runtime.genie().setGeniePrivateState4(slot, 0);
        runtime.genie().setGeniePrivateCountdown1(slot, 0);
        runtime.genie().setGeniePrivateCountdown2(slot, 0);
        runtime.genie().setGenieDirection(slot, 0);
        runtime.genie().setGenieDeathInitialized(slot, false);
        runtime.genie().setGenieSpeedX(slot, 0);
        runtime.genie().setGenieSpeedY(slot, 0);
        runtime.genie().setGenieSpeedZ(slot, 0);
        runtime.genie().setGenieSpeedXAccumulator(slot, 0);
        runtime.genie().setGenieSpeedYAccumulator(slot, 0);
        runtime.genie().setGenieSpeedZAccumulator(slot, 0);
        RoomEntity entity = runtime.slots()[slot];
        runtime.setBombFinalPresentationPending(slot, false);
        runtime.combatTables().writeEnemyHitboxFlags(slot, 0);
        runtime.bombInteractions().writePlayerArrowBombArrow(slot, false);
        runtime.bombInteractions().writeBombPrivateCountdown1(slot, 0);
        runtime.bombInteractions().writeBombPrivateCountdown3(slot, 0);
        runtime.projectiles().writeMagicPowderState(slot, 0);
        runtime.projectiles().writeMagicPowderPrivateState4(slot, 0);
        runtime.motionSet().secretSeashellMotion.clear(slot);
        runtime.physicsTables().writeEntityPrivateState4(slot, 0);
        runtime.physicsTables().writeEntityInertia(slot, 0);
        runtime.projectiles().writeMusicalNoteInertia(slot, 0);
        runtime.projectiles().writeMusicalNoteSpeedX(slot, 0);
        runtime.projectiles().writeMusicalNoteSpeedY(slot, 0);
        runtime.physicsTables().writeEntityUnknownJ(slot, 0);
        runtime.motionSet().unmaskedIronMaskMotion.setMaskRemovalState(slot, 0);
        runtime.motionSet().unmaskedIronMaskMotion.setMaskSourceHookshotSlot(slot, 0);
        runtime.liftThrow().writeLiftableRockSmashCountdown(slot, 0);
        runtime.liftThrow().writeLiftableRockSmashSourceVariant(slot, 0);
        runtime.liftThrow().writeLiftableRockSmashActive(slot, false);
        if (runtime.bombInteractions().readLatestDroppedBombEntityIndex() == slot) {
            runtime.bombInteractions().writeLatestDroppedBombEntityIndex(-1);
        }
        if (runtime.projectiles().readLatestShotArrowEntityIndex() == slot) {
            runtime.projectiles().writeLatestShotArrowEntityIndex(-1);
        }
        runtime.motionSet().bombiteMotion.clear(slot);
        runtime.motionSet().blooperMotion.clear(slot);
        runtime.motionSet().pincerMotion.clear(slot);
        runtime.motionSet().bushCrawlerMotion.clear(slot);
        runtime.motionSet().pokeyMotion.clear(slot);
        runtime.motionSet().zoraMotion.clear(slot);
        runtime.motionSet().zombieMotion.clear(slot);
        runtime.motionSet().buzzBlobMotion.clear(slot);
        runtime.motionSet().sandCrabMotion.clear(slot);
        runtime.motionSet().dogMotion.clear(slot);
        runtime.motionSet().urchinMotion.clear(slot);
        runtime.motionSet().witchRatMotion.clear(slot);
        runtime.motionSet().mimicMotion.clear(slot);
        runtime.motionSet().maskedMimicMotion.clear(slot);
        runtime.motionSet().miniMoldormMotion.clear(slot);
        runtime.motionSet().moldormMotion.clear(slot);
        if (!entity.loaded()) {
            return 0;
        }
        runtime.physicsTables().writeSlowTransitionCountdown(slot, 0);
        runtime.physicsTables().writeSlowTimerInitialized(slot, false);
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(slot, 0);
        runtime.combatTables().writeDyingCountdown(slot, 0);
        runtime.combatTables().writePowerRecoilDeath(slot, false);
        runtime.physicsTables().writeEnemyPhysicsFlags(slot, 0);
        runtime.combatTables().writeEnemyHealth(slot, 0);
        runtime.combatTables().writeEnemyHealthGroup(slot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(slot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(slot, 0);
        runtime.pickups().writeChestSpeedY(slot, 0);
        runtime.pickups().writeChestSpeedYAccumulator(slot, 0);
        runtime.pickups().writeChestInertia(slot, 0);
        runtime.pickups().writeChestItemBySlot(slot, 0);
        runtime.story().writeOwlPrivateCountdown1(slot, 0);
        runtime.physicsTables().writeEntityGroundStatus(slot, 0);
        runtime.physicsTables().writeFallingTargetX(slot, 0);
        runtime.physicsTables().writeFallingTargetY(slot, 0);
        runtime.physicsTables().writeFallingSpeedX(slot, 0);
        runtime.physicsTables().writeFallingSpeedY(slot, 0);
        runtime.physicsTables().writeFallingSpeedXAccumulator(slot, 0);
        runtime.physicsTables().writeFallingSpeedYAccumulator(slot, 0);
        runtime.physicsTables().writeFallingVisualYOffset(slot, 0);
        runtime.liftThrow().writeLiftedPhase(slot, 0);
        runtime.liftThrow().writeLiftedSourceDirection(slot, 0);
        runtime.liftThrow().writeLiftedStateInitialized(slot, false);
        runtime.liftThrow().writeThrownDirection(slot, 0xFF);
        runtime.bombInteractions().writeBombDirection(slot, 0xFF);
        runtime.bombInteractions().writeBombPrivateState4(slot, 0);
        runtime.projectiles().writeMagicPowderState(slot, 0);
        runtime.projectiles().writeMagicPowderPrivateState4(slot, 0);
        runtime.bombInteractions().writeBombPrivateCountdown1(slot, 0);
        runtime.bombInteractions().writeBombPrivateCountdown3(slot, 0);
        runtime.bombInteractions().writePlacedBombMotionInitialized(slot, false);
        runtime.liftThrow().writeLedgeTransitionTimer(slot, 0);
        runtime.liftThrow().writeThrownMotionInitialized(slot, false);
        runtime.motionSet().hookshotChainMotion.clear(slot);
        runtime.motionSet().hookshotBridgeMotion.clear(slot);
        runtime.motionSet().boomerangMotion.clear(slot);
        runtime.motionSet().magicRodFireballMotion.clear(slot);
        runtime.motionSet().swordBeamMotion.clear(slot);
        if (entity.type() == EntityRomConstants.ENTITY_HOOKSHOT_CHAIN) {
            runtime.slotProperties().writeHookshotChainOam(List.of());
        }
        runtime.physicsTables().writeBaseEntityFlipAttribute(slot, 0);
        runtime.physicsTables().writeEntityOptions1Override(slot, -1);
        runtime.motionSet().enemyRecoilMotion.clear(slot);
        runtime.motionSet().colorShellMotion.clear(slot);
        runtime.motionSet().butterflyMotion.clear(slot);
        runtime.motionSet().keeseMotion.clear(slot);
        runtime.motionSet().roamingEnemyMotion.clear(slot);
        runtime.motionSet().unmaskedIronMaskMotion.clear(slot);
        runtime.motionSet().moblinSwordMotion.clear(slot);
        runtime.motionSet().playerArrowMotion.clear(slot);
        runtime.motionSet().tektiteMotion.clear(slot);
        runtime.motionSet().leeverMotion.clear(slot);
        runtime.motionSet().antiFairyMotion.clear(slot);
        runtime.motionSet().sparkMotion.clear(slot);
        runtime.motionSet().zolGelMotion.clear(slot);
        runtime.motionSet().pokeyMotion.clear(slot);
        runtime.motionSet().hidingZolMotion.clear(slot);
        runtime.motionSet().starMotion.clear(slot);
        runtime.motionSet().blooperMotion.clear(slot);
        runtime.motionSet().pincerMotion.clear(slot);
        runtime.motionSet().spikeTrapMotion.clear(slot);
        runtime.motionSet().pairoddMotion.clear(slot);
        runtime.motionSet().pairoddProjectileMotion.clear(slot);
        runtime.motionSet().enemyProjectileMotion.clear(slot);
        runtime.motionSet().wingedOctorokMotion.clear(slot);
        runtime.motionSet().bushCrawlerMotion.clear(slot);
        runtime.motionSet().cuccoMotion.clear(slot);
        runtime.motionSet().giantGopongaMotion.clear(slot);
        runtime.motionSet().gopongaProjectileMotion.clear(slot);
        runtime.motionSet().zoraMotion.clear(slot);
        runtime.motionSet().zombieMotion.clear(slot);
        runtime.motionSet().buzzBlobMotion.clear(slot);
        runtime.motionSet().sandCrabMotion.clear(slot);
        runtime.motionSet().dogMotion.clear(slot);
        runtime.motionSet().urchinMotion.clear(slot);
        runtime.motionSet().witchRatMotion.clear(slot);
        runtime.slotProperties().writeWingedSwordAttackThisFrame(slot, false);
        runtime.slotProperties().writeWingedStateTwoThisFrame(slot, false);
        runtime.motionSet().laserMotion.clear(slot);
        runtime.motionSet().waterTektiteMotion.clear(slot);
        runtime.motionSet().fishMotion.clear(slot);
        runtime.motionSet().crowMotion.clear(slot);
        runtime.motionSet().booBuddyMotion.clear(slot);
        runtime.motionSet().stalfosAggressiveMotion.clear(slot);
        runtime.motionSet().stalfosEvasiveMotion.clear(slot);
        runtime.motionSet().threeOfAKindMotion.clear(slot);
        runtime.motionSet().gibdoMotion.clear(slot);
        runtime.motionSet().likeLikeMotion.clear(slot);
        runtime.motionSet().goombaMotion.clear(slot);
        runtime.motionSet().snakeMotion.clear(slot);
        runtime.motionSet().wizrobeMotion.clear(slot);
        runtime.motionSet().wizrobeProjectileMotion.clear(slot);
        runtime.motionSet().peaHatMotion.clear(slot);
        runtime.motionSet().armosMotion.clear(slot);
        runtime.motionSet().ghiniMotion.clear(slot);
        runtime.motionSet().hardHatMotion.clear(slot);
        runtime.motionSet().madBomberMotion.clear(slot);
        runtime.motionSet().followingNpcMotion.clear(slot);
        runtime.motionSet().bowWowMotion.clear(slot);
        runtime.motionSet().moblinKingMotion.clear(slot);
        runtime.motionSet().thrownEntityMotion.clear(slot);
        if (runtime.liftThrow().readLiftedEntitySlot() == slot) {
            runtime.liftThrow().writeLiftedEntitySlot(-1);
            runtime.liftThrow().writeLiftedCarryState(0);
            runtime.liftThrow().writeLiftedEffectiveDirection(0);
        }
        runtime.slots()[slot] = RoomEntity.disabled(slot);
        runtime.slotProperties().updatePincerBodyOam();
        return entity.sourceLoadOrder() >= 0 && entity.sourceLoadOrder() < 8 ? 1 << entity.sourceLoadOrder() : 0;
    }

    public RoomEntity applyZolSplit(RoomEntity original, RoomEntity updated, ZolGelMotion.Split split) {
        EntitySpriteDefinition gelDefinition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_GEL);
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
        runtime.physicsTables().writeEnemyPhysicsFlags(slot, initialPhysicsFlags(EntityRomConstants.ENTITY_GEL));
        runtime.combatTables().writeEnemyHitboxFlags(slot, initialHitboxFlags(EntityRomConstants.ENTITY_GEL));
        runtime.physicsTables().writeEntityOptions1Override(slot, -1);
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(slot, 0);
        runtime.combatTables()
                .writeEnemyHealthGroup(
                        slot,
                        runtime.enemyCombatTables() == null
                                ? 0
                                : runtime.enemyCombatTables().healthGroup(EntityRomConstants.ENTITY_GEL));
        runtime.combatTables().writeEnemyHealth(slot, runtime.initialHealth(EntityRomConstants.ENTITY_GEL));
        runtime.combatTables().writeEnemyFlashCountdown(slot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(slot, 0);
        runtime.combatTables().writeDyingCountdown(slot, 0);
        runtime.combatTables().writePowerRecoilDeath(slot, false);
        runtime.motionSet().enemyRecoilMotion.clear(slot);

        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot >= 0) {
            runtime.physicsTables()
                    .writeBaseEntityFlipAttribute(
                            freeSlot, runtime.physicsTables().readBaseEntityFlipAttribute(original.slot()));
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
            runtime.slots()[freeSlot] = spawnedGel;
            runtime.initializeHealthGroup(freeSlot);
            runtime.physicsTables()
                    .writeEnemyPhysicsFlags(freeSlot, initialPhysicsFlags(EntityRomConstants.ENTITY_GEL));
            runtime.combatTables().writeEnemyHitboxFlags(freeSlot, initialHitboxFlags(EntityRomConstants.ENTITY_GEL));
            runtime.physicsTables().writeEntityOptions1Override(freeSlot, -1);
            runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
            runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
            runtime.combatTables().writeDyingCountdown(freeSlot, 0);
            runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
            runtime.combatTables().writeEnemyHealth(freeSlot, runtime.initialHealth(EntityRomConstants.ENTITY_GEL));
            runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
            // SpawnNewEntity sets the new entity's ignore-hits countdown to 1.
            runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
            runtime.motionSet().enemyRecoilMotion.clear(freeSlot);
            runtime.motionSet().zolGelMotion.prepareSpawnedGel(freeSlot);
        }
        return gel;
    }

    void handleBowWowTargetContact(
            RoomEntity source, BowWowMotion.TargetContact contact, IntSupplier randomByteSupplier) {
        int targetSlot = contact.slot();
        if (targetSlot < 0 || targetSlot >= runtime.slots().length) {
            return;
        }
        RoomEntity target = runtime.slots()[targetSlot];
        if (!target.loaded()) {
            return;
        }

        if (target.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            if (runtime.physicsTables().readEntityPrivateState4(targetSlot) == 0
                    || runtime.linkState().dialogActive
                    || runtime.motionSet().bowWowMotion.privateCountdown2(source.slot()) != 0) {
                return;
            }
            runtime.motionSet().bowWowMotion.setTransitionCountdown(source.slot(), 0);
            runtime.motionSet().bowWowMotion.setPrivateCountdown2(source.slot(), 0x80);
            runtime.events().pendingDialogRequests.add(new DialogRequest(1, EntityRomConstants.DIALOG_BOW_WOW));
            return;
        }

        if (runtime.combatTables().readEnemyFlashCountdown(targetSlot) != 0) {
            return;
        }
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        source.slot(), source.type(), 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x03));
        if (target.type() == EntityRomConstants.ENTITY_KIKI_THE_MONKEY) {
            runtime.combatTables().writeEnemyFlashCountdown(targetSlot, 0x18);
            runtime.physicsTables()
                    .writeEntityInertia(targetSlot, (runtime.physicsTables().readEntityInertia(targetSlot) + 1) & 0xFF);
            return;
        }
        runtime.bossRewards().handleTerminalEnemyDeath(target, randomByteSupplier);
    }

    void finishBurning(RoomEntity entity) {
        int slot = entity.slot();
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0);
        if (entity.type() == EntityRomConstants.ENTITY_GIBDO) {
            EntitySpriteDefinition definition =
                    runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_STALFOS_EVASIVE);
            RoomEntity stalfos = withType(entity, EntityRomConstants.ENTITY_STALFOS_EVASIVE, definition);
            runtime.slots()[slot] = withStatus(stalfos, EntityStatus.ACTIVE);
            runtime.initializeHealthGroup(slot); // $03:4C89 reconfigures attributes/health.
            runtime.combatTables()
                    .writeEnemyHealth(slot, runtime.initialHealth(EntityRomConstants.ENTITY_STALFOS_EVASIVE));
            runtime.combatTables().writeEnemyFlashCountdown(slot, 0);
            runtime.combatTables().writeEnemyIgnoreHitsCountdown(slot, 0);
            runtime.physicsTables().writeEntityOptions1Override(slot, -1);
            runtime.physicsTables().writeEnemyPhysicsFlags(slot, EntityRomConstants.EVASIVE_PHYSICS_FLAGS);
            runtime.combatTables().writeDyingCountdown(slot, 0);
            runtime.combatTables().writePowerRecoilDeath(slot, false);
            runtime.motionSet().enemyRecoilMotion.clear(slot);
            runtime.motionSet().stalfosEvasiveMotion.clear(slot);
            return;
        }

        runtime.physicsTables().writeEnemyPhysicsFlags(slot, 0x04);
        runtime.combatTables().writeDyingCountdown(slot, 0x1F);
        runtime.combatTables().writePowerRecoilDeath(slot, false);
        runtime.slots()[slot] = withDeathPresentation(withStatus(entity, EntityStatus.DYING), 3, false);
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
    }

    void decrementSlowTransitionCountdown(int slot, int frameCounter) {
        if ((frameCounter & 0x03) == 0
                && runtime.physicsTables().readSlowTimerInitialized(slot)
                && runtime.physicsTables().readSlowTransitionCountdown(slot) > 0) {
            runtime.physicsTables().postSlowTransitionCountdown(slot, -1);
        }
    }

    boolean shouldDisappear(RoomEntity entity) {
        return runtime.enemyInspection().shouldDisappear(entity);
    }

    public void disableEntityWithoutPersistence(int slot) {
        runtime.slimeEye().clear(slot);
        runtime.pickups().resetEnemyDropState(slot);
        runtime.motionSet().sideViewPlatformMotion.clear(slot);
        runtime.motionSet().sideViewPotMotion.clear(slot);
        runtime.motionSet().genieMotion.clear(slot);
        if (runtime.genie().genieControllerSlot() == slot) {
            runtime.genie().setGenieControllerSlot(-1);
        }
        runtime.genie().setGenieActiveState(slot, 0);
        runtime.genie().setGeniePrivateState1(slot, 0);
        runtime.genie().setGeniePrivateState2(slot, 0);
        runtime.genie().setGeniePrivateState3(slot, 0);
        runtime.genie().setGeniePrivateState4(slot, 0);
        runtime.genie().setGeniePrivateCountdown1(slot, 0);
        runtime.genie().setGeniePrivateCountdown2(slot, 0);
        runtime.genie().setGenieDirection(slot, 0);
        runtime.genie().setGenieDeathInitialized(slot, false);
        runtime.genie().setGenieSpeedX(slot, 0);
        runtime.genie().setGenieSpeedY(slot, 0);
        runtime.genie().setGenieSpeedZ(slot, 0);
        runtime.genie().setGenieSpeedXAccumulator(slot, 0);
        runtime.genie().setGenieSpeedYAccumulator(slot, 0);
        runtime.genie().setGenieSpeedZAccumulator(slot, 0);
        runtime.motionSet().secretSeashellMotion.clear(slot);
        runtime.physicsTables().writeSlowTransitionCountdown(slot, 0);
        runtime.physicsTables().writeSlowTimerInitialized(slot, false);
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0);
        runtime.bossRewards().writeBossDeathProducerState(slot, 0);
        runtime.motionSet().moldormMotion.setDestructionTailState(slot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(slot, 0);
        runtime.combatTables().writeDyingCountdown(slot, 0);
        runtime.combatTables().writePowerRecoilDeath(slot, false);
        runtime.physicsTables().writeEnemyPhysicsFlags(slot, 0);
        runtime.combatTables().writeEnemyHealth(slot, 0);
        runtime.combatTables().writeEnemyHealthGroup(slot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(slot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(slot, 0);
        runtime.pickups().writeChestSpeedY(slot, 0);
        runtime.pickups().writeChestSpeedYAccumulator(slot, 0);
        runtime.pickups().writeChestInertia(slot, 0);
        runtime.pickups().writeChestItemBySlot(slot, 0);
        runtime.combatTables().writeEnemyHitboxFlags(slot, 0);
        runtime.rewards().writeInstrumentParticleKind(slot, 0);
        runtime.rewards().writeInstrumentParticleSpeedX(slot, 0);
        runtime.rewards().writeInstrumentParticleSpeedY(slot, 0);
        runtime.rewards().writeInstrumentParticleXAccumulator(slot, 0);
        runtime.rewards().writeInstrumentParticleYAccumulator(slot, 0);
        runtime.motionSet().armosKnightMotion.clear(slot);
        runtime.physicsTables().writeEntityGroundStatus(slot, 0);
        runtime.physicsTables().writeFallingTargetX(slot, 0);
        runtime.physicsTables().writeFallingTargetY(slot, 0);
        runtime.physicsTables().writeFallingSpeedX(slot, 0);
        runtime.physicsTables().writeFallingSpeedY(slot, 0);
        runtime.physicsTables().writeFallingSpeedXAccumulator(slot, 0);
        runtime.physicsTables().writeFallingSpeedYAccumulator(slot, 0);
        runtime.physicsTables().writeFallingVisualYOffset(slot, 0);
        runtime.liftThrow().writeLiftedPhase(slot, 0);
        runtime.liftThrow().writeLiftedSourceDirection(slot, 0);
        runtime.liftThrow().writeLiftedStateInitialized(slot, false);
        runtime.liftThrow().writeThrownDirection(slot, 0xFF);
        runtime.bombInteractions().writeBombDirection(slot, 0xFF);
        runtime.bombInteractions().writeBombPrivateState4(slot, 0);
        runtime.projectiles().writeMagicPowderState(slot, 0);
        runtime.projectiles().writeMagicPowderPrivateState4(slot, 0);
        runtime.bombInteractions().writeBombPrivateCountdown1(slot, 0);
        runtime.bombInteractions().writeBombPrivateCountdown3(slot, 0);
        runtime.motionSet().unmaskedIronMaskMotion.setMaskRemovalState(slot, 0);
        runtime.motionSet().unmaskedIronMaskMotion.setMaskSourceHookshotSlot(slot, 0);
        runtime.liftThrow().writeLiftableRockSmashCountdown(slot, 0);
        runtime.liftThrow().writeLiftableRockSmashSourceVariant(slot, 0);
        runtime.liftThrow().writeLiftableRockSmashActive(slot, false);
        runtime.setBombFinalPresentationPending(slot, false);
        runtime.bombInteractions().writePlacedBombMotionInitialized(slot, false);
        runtime.bombInteractions().writeEnemyBombMotionInitialized(slot, false);
        runtime.liftThrow().writeLedgeTransitionTimer(slot, 0);
        runtime.liftThrow().writeThrownMotionInitialized(slot, false);
        runtime.physicsTables().writeBaseEntityFlipAttribute(slot, 0);
        runtime.physicsTables().writeEntityOptions1Override(slot, -1);
        runtime.motionSet().enemyRecoilMotion.clear(slot);
        runtime.motionSet().bomberMotion.clear(slot);
        runtime.motionSet().madBomberMotion.clear(slot);
        runtime.motionSet().bombiteMotion.clear(slot);
        runtime.motionSet().colorShellMotion.clear(slot);
        runtime.motionSet().butterflyMotion.clear(slot);
        runtime.motionSet().keeseMotion.clear(slot);
        runtime.motionSet().roamingEnemyMotion.clear(slot);
        runtime.motionSet().unmaskedIronMaskMotion.clear(slot);
        runtime.motionSet().moblinSwordMotion.clear(slot);
        runtime.motionSet().tektiteMotion.clear(slot);
        runtime.motionSet().leeverMotion.clear(slot);
        runtime.motionSet().antiFairyMotion.clear(slot);
        runtime.motionSet().sparkMotion.clear(slot);
        runtime.motionSet().zolGelMotion.clear(slot);
        runtime.motionSet().hidingZolMotion.clear(slot);
        runtime.motionSet().starMotion.clear(slot);
        runtime.motionSet().blooperMotion.clear(slot);
        runtime.motionSet().pincerMotion.clear(slot);
        runtime.motionSet().bushCrawlerMotion.clear(slot);
        runtime.motionSet().pokeyMotion.clear(slot);
        runtime.motionSet().piranhaMotion.clear(slot);
        runtime.motionSet().zoraMotion.clear(slot);
        runtime.motionSet().zombieMotion.clear(slot);
        runtime.motionSet().buzzBlobMotion.clear(slot);
        runtime.motionSet().sandCrabMotion.clear(slot);
        runtime.motionSet().dogMotion.clear(slot);
        runtime.motionSet().mimicMotion.clear(slot);
        runtime.motionSet().maskedMimicMotion.clear(slot);
        runtime.motionSet().miniMoldormMotion.clear(slot);
        runtime.motionSet().moldormMotion.clear(slot);
        runtime.motionSet().cuccoMotion.clear(slot);
        runtime.motionSet().spikeTrapMotion.clear(slot);
        runtime.motionSet().pairoddMotion.clear(slot);
        runtime.motionSet().pairoddProjectileMotion.clear(slot);
        runtime.motionSet().playerArrowMotion.clear(slot);
        runtime.motionSet().magicRodFireballMotion.clear(slot);
        runtime.bombInteractions().writePlayerArrowBombArrow(slot, false);
        if (runtime.bombInteractions().readLatestDroppedBombEntityIndex() == slot) {
            runtime.bombInteractions().writeLatestDroppedBombEntityIndex(-1);
        }
        if (runtime.projectiles().readLatestShotArrowEntityIndex() == slot) {
            runtime.projectiles().writeLatestShotArrowEntityIndex(-1);
        }
        runtime.motionSet().enemyProjectileMotion.clear(slot);
        runtime.motionSet().laserMotion.clear(slot);
        runtime.motionSet().waterTektiteMotion.clear(slot);
        runtime.motionSet().fishMotion.clear(slot);
        runtime.motionSet().crowMotion.clear(slot);
        runtime.motionSet().booBuddyMotion.clear(slot);
        runtime.motionSet().stalfosAggressiveMotion.clear(slot);
        runtime.motionSet().stalfosEvasiveMotion.clear(slot);
        runtime.motionSet().gibdoMotion.clear(slot);
        runtime.motionSet().goombaMotion.clear(slot);
        runtime.motionSet().snakeMotion.clear(slot);
        runtime.motionSet().wizrobeMotion.clear(slot);
        runtime.motionSet().wizrobeProjectileMotion.clear(slot);
        runtime.motionSet().peaHatMotion.clear(slot);
        runtime.motionSet().armosMotion.clear(slot);
        runtime.motionSet().followingNpcMotion.clear(slot);
        runtime.motionSet().ghiniMotion.clear(slot);
        runtime.motionSet().hardHatMotion.clear(slot);
        runtime.motionSet().polsVoiceMotion.clear(slot);
        runtime.motionSet().spikedBeetleMotion.clear(slot);
        runtime.motionSet().bowWowMotion.clear(slot);
        runtime.motionSet().moblinKingMotion.clear(slot);
        runtime.motionSet().thrownEntityMotion.clear(slot);
        if (runtime.liftThrow().readLiftedEntitySlot() == slot) {
            runtime.liftThrow().writeLiftedEntitySlot(-1);
            runtime.liftThrow().writeLiftedCarryState(0);
            runtime.liftThrow().writeLiftedEffectiveDirection(0);
        }
        runtime.slots()[slot] = RoomEntity.disabled(slot);
    }

    boolean decrementEnemyCombatCountdowns(int slot, boolean decrementIgnoreHits) {
        if (runtime.combatTables().readEnemyFlashCountdown(slot) > 0) {
            runtime.combatTables().postEnemyFlashCountdown(slot, -1);
        }
        if (decrementIgnoreHits
                && !runtime.motionSet().enemyRecoilMotion.isActive(slot)
                && runtime.combatTables().readEnemyIgnoreHitsCountdown(slot) > 0) {
            runtime.combatTables().postEnemyIgnoreHitsCountdown(slot, -1);
            return true;
        }
        return false;
    }

    void decrementEnemyStatusCountdowns(int slot) {
        if (runtime.physicsTables().readEnemyTransitionCountdown(slot) > 0) {
            runtime.physicsTables().postEnemyTransitionCountdown(slot, -1);
        }
        if (runtime.combatTables().readEnemyStunnedCountdown(slot) > 0) {
            runtime.combatTables().postEnemyStunnedCountdown(slot, -1);
        }
        if (runtime.combatTables().readDyingCountdown(slot) > 0) {
            runtime.combatTables().postDyingCountdown(slot, -1);
        }
        if (runtime.bombInteractions().readBombPrivateCountdown1(slot) > 0) {
            runtime.bombInteractions().postBombPrivateCountdown1(slot, -1);
        }
        if (runtime.bombInteractions().readBombPrivateCountdown3(slot) > 0) {
            runtime.bombInteractions().postBombPrivateCountdown3(slot, -1);
        }
        if (runtime.story().readOwlPrivateCountdown1(slot) > 0
                && runtime.backgroundMechanics().entityTimersInteractive()) {
            runtime.story().postOwlPrivateCountdown1(slot, -1);
        }
        runtime.motionSet().hinoxMotion.decrementPrivateCountdown1(slot);
        if (runtime.liftThrow().readLiftableRockSmashActive(slot)
                && runtime.liftThrow().readLiftableRockSmashCountdown(slot) > 0) {
            runtime.liftThrow().postLiftableRockSmashCountdown(slot, -1);
        }
        runtime.motionSet().blooperMotion.decrementPrivateCountdown3(slot);
        runtime.motionSet().wingedOctorokMotion.decrementPrivateCountdown2(slot);
    }

    public void requestPersistentClearMask(int mask) {
        runtime.roomVariables()
                .writePendingClearedEntityMask(runtime.roomVariables().readPendingClearedEntityMask() | (mask));
    }

    public void delayInteractions(int countdown) {
        runtime.roomVariables().writeEntityInteractionCountdown(countdown);
    }

    public void recordGelClinging(boolean clinging) {
        runtime.motionSet()
                .zolGelMotion
                .writeGelClingingToLink(runtime.motionSet().zolGelMotion.readGelClingingToLink() | (clinging));
    }

    public void setDroppedItemBySlot(int slot, int value) {
        runtime.bossRewards().writeDroppedItemBySlot(slot, value);
    }

    public void setDyingCountdown(int slot, int value) {
        runtime.combatTables().writeDyingCountdown(slot, value);
    }

    public void setDropPrivateCountdown3(int slot, int value) {
        runtime.bossRewards().writeDropPrivateCountdown3(slot, value);
    }

    public int warpState(int slot) {
        return runtime.warpState().readWarpState(slot);
    }

    public void setWarpState(int slot, int value) {
        runtime.warpState().writeWarpState(slot, value);
    }

    public int warpTransitionCountdown(int slot) {
        return runtime.warpState().readWarpTransitionCountdown(slot);
    }

    public void setWarpTransitionCountdown(int slot, int value) {
        runtime.warpState().writeWarpTransitionCountdown(slot, value);
    }

    public int droppablePrivateState3(int slot) {
        return runtime.bossRewards().readDroppablePrivateState3(slot);
    }
}
