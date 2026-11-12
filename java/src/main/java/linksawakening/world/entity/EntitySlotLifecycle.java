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
import linksawakening.world.entity.motion.*;
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
        return collectIfNeeded(new EntityPickupFrame(
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
                0));
    }

    EntityPickupEvent collectIfNeeded(EntityPickupFrame inputs) {
        int frameCounter = inputs.frameCounter();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        boolean linkAirborne = inputs.linkAirborne();
        boolean linkInteractive = inputs.linkInteractive();
        int linkDirection = inputs.linkDirection();
        int linkZ = inputs.linkZ();
        boolean swordActive = inputs.swordActive();
        int swordX = inputs.swordX();
        int swordWidth = inputs.swordWidth();
        int swordY = inputs.swordY();
        int swordHeight = inputs.swordHeight();
        if (!linkInteractive) {
            return null;
        }
        int romDirection = romDirectionForJavaDirection(linkDirection);

        for (int index = runtime.slots().length - 1; index >= 0; index--) {
            RoomEntity entity = runtime.slots()[index];
            if (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) {
                runtime.motions()
                        .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
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
                            && (runtime.motions()
                                                    .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                                                    .privateState3(entity.slot())
                                            != 0
                                    || runtime.motions()
                                                    .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                                                    .privateCountdown1(entity.slot())
                                            != 0))
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
        runtime.motions()
                .get(SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                .clear(slot);
        runtime.motions().get(SideViewPotMotion.class, SideViewPotMotion::new).clear(slot);
        runtime.motions().get(GenieMotion.class, GenieMotion::new).clear(slot);
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
        runtime.motions()
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .clear(slot);
        runtime.physicsTables().writeEntityPrivateState4(slot, 0);
        runtime.physicsTables().writeEntityInertia(slot, 0);
        runtime.projectiles().writeMusicalNoteInertia(slot, 0);
        runtime.projectiles().writeMusicalNoteSpeedX(slot, 0);
        runtime.projectiles().writeMusicalNoteSpeedY(slot, 0);
        runtime.physicsTables().writeEntityUnknownJ(slot, 0);
        runtime.motions()
                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                .setMaskRemovalState(slot, 0);
        runtime.motions()
                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                .setMaskSourceHookshotSlot(slot, 0);
        runtime.liftThrow().writeLiftableRockSmashCountdown(slot, 0);
        runtime.liftThrow().writeLiftableRockSmashSourceVariant(slot, 0);
        runtime.liftThrow().writeLiftableRockSmashActive(slot, false);
        if (runtime.bombInteractions().readLatestDroppedBombEntityIndex() == slot) {
            runtime.bombInteractions().writeLatestDroppedBombEntityIndex(-1);
        }
        if (runtime.projectiles().readLatestShotArrowEntityIndex() == slot) {
            runtime.projectiles().writeLatestShotArrowEntityIndex(-1);
        }
        runtime.motions().get(BombiteMotion.class, BombiteMotion::new).clear(slot);
        runtime.motions().get(BlooperMotion.class, BlooperMotion::new).clear(slot);
        runtime.motions().get(PincerMotion.class, PincerMotion::new).clear(slot);
        runtime.motions().get(BushCrawlerMotion.class, BushCrawlerMotion::new).clear(slot);
        runtime.motions().get(PokeyMotion.class, PokeyMotion::new).clear(slot);
        runtime.motions().get(ZoraMotion.class, ZoraMotion::new).clear(slot);
        runtime.motions().get(ZombieMotion.class, ZombieMotion::new).clear(slot);
        runtime.motions().get(BuzzBlobMotion.class, BuzzBlobMotion::new).clear(slot);
        runtime.motions().get(SandCrabMotion.class, SandCrabMotion::new).clear(slot);
        runtime.motions().get(DogMotion.class, DogMotion::new).clear(slot);
        runtime.motions().get(UrchinMotion.class, UrchinMotion::new).clear(slot);
        runtime.motions().get(WitchRatMotion.class, WitchRatMotion::new).clear(slot);
        runtime.motions().get(MimicMotion.class, MimicMotion::new).clear(slot);
        runtime.motions().get(MaskedMimicMotion.class, MaskedMimicMotion::new).clear(slot);
        runtime.motions().get(MiniMoldormMotion.class, MiniMoldormMotion::new).clear(slot);
        runtime.motions().get(MoldormMotion.class, MoldormMotion::new).clear(slot);
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
        runtime.motions()
                .get(HookshotChainMotion.class, HookshotChainMotion::new)
                .clear(slot);
        runtime.motions()
                .get(HookshotBridgeMotion.class, HookshotBridgeMotion::new)
                .clear(slot);
        runtime.motions().get(BoomerangMotion.class, BoomerangMotion::new).clear(slot);
        runtime.motions()
                .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                .clear(slot);
        runtime.motions().get(SwordBeamMotion.class, SwordBeamMotion::new).clear(slot);
        if (entity.type() == EntityRomConstants.ENTITY_HOOKSHOT_CHAIN) {
            runtime.slotProperties().writeHookshotChainOam(List.of());
        }
        runtime.physicsTables().writeBaseEntityFlipAttribute(slot, 0);
        runtime.physicsTables().writeEntityOptions1Override(slot, -1);
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(slot);
        runtime.motions().get(ColorShellMotion.class, ColorShellMotion::new).clear(slot);
        runtime.motions().get(ButterflyMotion.class, ButterflyMotion::new).clear(slot);
        runtime.motions().get(KeeseMotion.class, KeeseMotion::new).clear(slot);
        runtime.motions().get(RoamingEnemyMotion.class, RoamingEnemyMotion::new).clear(slot);
        runtime.motions()
                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                .clear(slot);
        runtime.motions().get(MoblinSwordMotion.class, MoblinSwordMotion::new).clear(slot);
        runtime.motions().get(PlayerArrowMotion.class, PlayerArrowMotion::new).clear(slot);
        runtime.motions().get(TektiteMotion.class, TektiteMotion::new).clear(slot);
        runtime.motions().get(LeeverMotion.class, LeeverMotion::new).clear(slot);
        runtime.motions().get(AntiFairyMotion.class, AntiFairyMotion::new).clear(slot);
        runtime.motions().get(SparkMotion.class, SparkMotion::new).clear(slot);
        runtime.motions().get(ZolGelMotion.class, ZolGelMotion::new).clear(slot);
        runtime.motions().get(PokeyMotion.class, PokeyMotion::new).clear(slot);
        runtime.motions().get(HidingZolMotion.class, HidingZolMotion::new).clear(slot);
        runtime.motions().get(StarMotion.class, StarMotion::new).clear(slot);
        runtime.motions().get(BlooperMotion.class, BlooperMotion::new).clear(slot);
        runtime.motions().get(PincerMotion.class, PincerMotion::new).clear(slot);
        runtime.motions().get(SpikeTrapMotion.class, SpikeTrapMotion::new).clear(slot);
        runtime.motions().get(PairoddMotion.class, PairoddMotion::new).clear(slot);
        runtime.motions()
                .get(PairoddProjectileMotion.class, PairoddProjectileMotion::new)
                .clear(slot);
        runtime.motions()
                .get(EnemyProjectileMotion.class, EnemyProjectileMotion::new)
                .clear(slot);
        runtime.motions()
                .get(WingedOctorokMotion.class, WingedOctorokMotion::new)
                .clear(slot);
        runtime.motions().get(BushCrawlerMotion.class, BushCrawlerMotion::new).clear(slot);
        runtime.motions().get(CuccoMotion.class, CuccoMotion::new).clear(slot);
        runtime.motions().get(GiantGopongaMotion.class, GiantGopongaMotion::new).clear(slot);
        runtime.motions()
                .get(GopongaProjectileMotion.class, GopongaProjectileMotion::new)
                .clear(slot);
        runtime.motions().get(ZoraMotion.class, ZoraMotion::new).clear(slot);
        runtime.motions().get(ZombieMotion.class, ZombieMotion::new).clear(slot);
        runtime.motions().get(BuzzBlobMotion.class, BuzzBlobMotion::new).clear(slot);
        runtime.motions().get(SandCrabMotion.class, SandCrabMotion::new).clear(slot);
        runtime.motions().get(DogMotion.class, DogMotion::new).clear(slot);
        runtime.motions().get(UrchinMotion.class, UrchinMotion::new).clear(slot);
        runtime.motions().get(WitchRatMotion.class, WitchRatMotion::new).clear(slot);
        runtime.slotProperties().writeWingedSwordAttackThisFrame(slot, false);
        runtime.slotProperties().writeWingedStateTwoThisFrame(slot, false);
        runtime.motions().get(LaserMotion.class, LaserMotion::new).clear(slot);
        runtime.motions().get(WaterTektiteMotion.class, WaterTektiteMotion::new).clear(slot);
        runtime.motions().get(FishMotion.class, FishMotion::new).clear(slot);
        runtime.motions().get(CrowMotion.class, CrowMotion::new).clear(slot);
        runtime.motions().get(BooBuddyMotion.class, BooBuddyMotion::new).clear(slot);
        runtime.motions()
                .get(StalfosAggressiveMotion.class, StalfosAggressiveMotion::new)
                .clear(slot);
        runtime.motions()
                .get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new)
                .clear(slot);
        runtime.motions().get(ThreeOfAKindMotion.class, ThreeOfAKindMotion::new).clear(slot);
        runtime.motions().get(GibdoMotion.class, GibdoMotion::new).clear(slot);
        runtime.motions().get(LikeLikeMotion.class, LikeLikeMotion::new).clear(slot);
        runtime.motions().get(GoombaMotion.class, GoombaMotion::new).clear(slot);
        runtime.motions().get(SnakeMotion.class, SnakeMotion::new).clear(slot);
        runtime.motions().get(WizrobeMotion.class, WizrobeMotion::new).clear(slot);
        runtime.motions()
                .get(WizrobeProjectileMotion.class, WizrobeProjectileMotion::new)
                .clear(slot);
        runtime.motions().get(PeaHatMotion.class, PeaHatMotion::new).clear(slot);
        runtime.motions().get(ArmosMotion.class, ArmosMotion::new).clear(slot);
        runtime.motions().get(GhiniMotion.class, GhiniMotion::new).clear(slot);
        runtime.motions().get(HardHatMotion.class, HardHatMotion::new).clear(slot);
        runtime.motions().get(MadBomberMotion.class, MadBomberMotion::new).clear(slot);
        runtime.motions().get(FollowingNpcMotion.class, FollowingNpcMotion::new).clear(slot);
        runtime.motions().get(BowWowMotion.class, BowWowMotion::new).clear(slot);
        runtime.motions().get(MoblinKingMotion.class, MoblinKingMotion::new).clear(slot);
        runtime.motions().get(ThrownEntityMotion.class, ThrownEntityMotion::new).clear(slot);
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
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(slot);

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
            runtime.motions()
                    .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                    .clear(freeSlot);
            runtime.motions().get(ZolGelMotion.class, ZolGelMotion::new).prepareSpawnedGel(freeSlot);
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
                    || runtime.motions()
                                    .get(BowWowMotion.class, BowWowMotion::new)
                                    .privateCountdown2(source.slot())
                            != 0) {
                return;
            }
            runtime.motions().get(BowWowMotion.class, BowWowMotion::new).setTransitionCountdown(source.slot(), 0);
            runtime.motions().get(BowWowMotion.class, BowWowMotion::new).setPrivateCountdown2(source.slot(), 0x80);
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
            runtime.motions()
                    .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                    .clear(slot);
            runtime.motions()
                    .get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new)
                    .clear(slot);
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
        runtime.motions()
                .get(SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                .clear(slot);
        runtime.motions().get(SideViewPotMotion.class, SideViewPotMotion::new).clear(slot);
        runtime.motions().get(GenieMotion.class, GenieMotion::new).clear(slot);
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
        runtime.motions()
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .clear(slot);
        runtime.physicsTables().writeSlowTransitionCountdown(slot, 0);
        runtime.physicsTables().writeSlowTimerInitialized(slot, false);
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0);
        runtime.bossRewards().writeBossDeathProducerState(slot, 0);
        runtime.motions().get(MoldormMotion.class, MoldormMotion::new).setDestructionTailState(slot, 0);
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
        runtime.motions().get(ArmosKnightMotion.class, ArmosKnightMotion::new).clear(slot);
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
        runtime.motions()
                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                .setMaskRemovalState(slot, 0);
        runtime.motions()
                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                .setMaskSourceHookshotSlot(slot, 0);
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
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(slot);
        runtime.motions().get(BomberMotion.class, BomberMotion::new).clear(slot);
        runtime.motions().get(MadBomberMotion.class, MadBomberMotion::new).clear(slot);
        runtime.motions().get(BombiteMotion.class, BombiteMotion::new).clear(slot);
        runtime.motions().get(ColorShellMotion.class, ColorShellMotion::new).clear(slot);
        runtime.motions().get(ButterflyMotion.class, ButterflyMotion::new).clear(slot);
        runtime.motions().get(KeeseMotion.class, KeeseMotion::new).clear(slot);
        runtime.motions().get(RoamingEnemyMotion.class, RoamingEnemyMotion::new).clear(slot);
        runtime.motions()
                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                .clear(slot);
        runtime.motions().get(MoblinSwordMotion.class, MoblinSwordMotion::new).clear(slot);
        runtime.motions().get(TektiteMotion.class, TektiteMotion::new).clear(slot);
        runtime.motions().get(LeeverMotion.class, LeeverMotion::new).clear(slot);
        runtime.motions().get(AntiFairyMotion.class, AntiFairyMotion::new).clear(slot);
        runtime.motions().get(SparkMotion.class, SparkMotion::new).clear(slot);
        runtime.motions().get(ZolGelMotion.class, ZolGelMotion::new).clear(slot);
        runtime.motions().get(HidingZolMotion.class, HidingZolMotion::new).clear(slot);
        runtime.motions().get(StarMotion.class, StarMotion::new).clear(slot);
        runtime.motions().get(BlooperMotion.class, BlooperMotion::new).clear(slot);
        runtime.motions().get(PincerMotion.class, PincerMotion::new).clear(slot);
        runtime.motions().get(BushCrawlerMotion.class, BushCrawlerMotion::new).clear(slot);
        runtime.motions().get(PokeyMotion.class, PokeyMotion::new).clear(slot);
        runtime.motions().get(PiranhaMotion.class, PiranhaMotion::new).clear(slot);
        runtime.motions().get(ZoraMotion.class, ZoraMotion::new).clear(slot);
        runtime.motions().get(ZombieMotion.class, ZombieMotion::new).clear(slot);
        runtime.motions().get(BuzzBlobMotion.class, BuzzBlobMotion::new).clear(slot);
        runtime.motions().get(SandCrabMotion.class, SandCrabMotion::new).clear(slot);
        runtime.motions().get(DogMotion.class, DogMotion::new).clear(slot);
        runtime.motions().get(MimicMotion.class, MimicMotion::new).clear(slot);
        runtime.motions().get(MaskedMimicMotion.class, MaskedMimicMotion::new).clear(slot);
        runtime.motions().get(MiniMoldormMotion.class, MiniMoldormMotion::new).clear(slot);
        runtime.motions().get(MoldormMotion.class, MoldormMotion::new).clear(slot);
        runtime.motions().get(CuccoMotion.class, CuccoMotion::new).clear(slot);
        runtime.motions().get(SpikeTrapMotion.class, SpikeTrapMotion::new).clear(slot);
        runtime.motions().get(PairoddMotion.class, PairoddMotion::new).clear(slot);
        runtime.motions()
                .get(PairoddProjectileMotion.class, PairoddProjectileMotion::new)
                .clear(slot);
        runtime.motions().get(PlayerArrowMotion.class, PlayerArrowMotion::new).clear(slot);
        runtime.motions()
                .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                .clear(slot);
        runtime.bombInteractions().writePlayerArrowBombArrow(slot, false);
        if (runtime.bombInteractions().readLatestDroppedBombEntityIndex() == slot) {
            runtime.bombInteractions().writeLatestDroppedBombEntityIndex(-1);
        }
        if (runtime.projectiles().readLatestShotArrowEntityIndex() == slot) {
            runtime.projectiles().writeLatestShotArrowEntityIndex(-1);
        }
        runtime.motions()
                .get(EnemyProjectileMotion.class, EnemyProjectileMotion::new)
                .clear(slot);
        runtime.motions().get(LaserMotion.class, LaserMotion::new).clear(slot);
        runtime.motions().get(WaterTektiteMotion.class, WaterTektiteMotion::new).clear(slot);
        runtime.motions().get(FishMotion.class, FishMotion::new).clear(slot);
        runtime.motions().get(CrowMotion.class, CrowMotion::new).clear(slot);
        runtime.motions().get(BooBuddyMotion.class, BooBuddyMotion::new).clear(slot);
        runtime.motions()
                .get(StalfosAggressiveMotion.class, StalfosAggressiveMotion::new)
                .clear(slot);
        runtime.motions()
                .get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new)
                .clear(slot);
        runtime.motions().get(GibdoMotion.class, GibdoMotion::new).clear(slot);
        runtime.motions().get(GoombaMotion.class, GoombaMotion::new).clear(slot);
        runtime.motions().get(SnakeMotion.class, SnakeMotion::new).clear(slot);
        runtime.motions().get(WizrobeMotion.class, WizrobeMotion::new).clear(slot);
        runtime.motions()
                .get(WizrobeProjectileMotion.class, WizrobeProjectileMotion::new)
                .clear(slot);
        runtime.motions().get(PeaHatMotion.class, PeaHatMotion::new).clear(slot);
        runtime.motions().get(ArmosMotion.class, ArmosMotion::new).clear(slot);
        runtime.motions().get(FollowingNpcMotion.class, FollowingNpcMotion::new).clear(slot);
        runtime.motions().get(GhiniMotion.class, GhiniMotion::new).clear(slot);
        runtime.motions().get(HardHatMotion.class, HardHatMotion::new).clear(slot);
        runtime.motions().get(PolsVoiceMotion.class, PolsVoiceMotion::new).clear(slot);
        runtime.motions().get(SpikedBeetleMotion.class, SpikedBeetleMotion::new).clear(slot);
        runtime.motions().get(BowWowMotion.class, BowWowMotion::new).clear(slot);
        runtime.motions().get(MoblinKingMotion.class, MoblinKingMotion::new).clear(slot);
        runtime.motions().get(ThrownEntityMotion.class, ThrownEntityMotion::new).clear(slot);
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
                && !runtime.motions()
                        .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                        .isActive(slot)
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
        runtime.motions().get(HinoxMotion.class, HinoxMotion::new).decrementPrivateCountdown1(slot);
        if (runtime.liftThrow().readLiftableRockSmashActive(slot)
                && runtime.liftThrow().readLiftableRockSmashCountdown(slot) > 0) {
            runtime.liftThrow().postLiftableRockSmashCountdown(slot, -1);
        }
        runtime.motions().get(BlooperMotion.class, BlooperMotion::new).decrementPrivateCountdown3(slot);
        runtime.motions()
                .get(WingedOctorokMotion.class, WingedOctorokMotion::new)
                .decrementPrivateCountdown2(slot);
    }

    public void requestPersistentClearMask(int mask) {
        runtime.roomVariables()
                .writePendingClearedEntityMask(runtime.roomVariables().readPendingClearedEntityMask() | (mask));
    }

    public void delayInteractions(int countdown) {
        runtime.roomVariables().writeEntityInteractionCountdown(countdown);
    }

    public void recordGelClinging(boolean clinging) {
        runtime.motions()
                .get(ZolGelMotion.class, ZolGelMotion::new)
                .writeGelClingingToLink(runtime.motions()
                                .get(ZolGelMotion.class, ZolGelMotion::new)
                                .readGelClingingToLink()
                        | (clinging));
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
