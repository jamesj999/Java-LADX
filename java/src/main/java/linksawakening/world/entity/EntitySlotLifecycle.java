package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BowWowMotion;
import linksawakening.world.entity.motion.ZolGelMotion;

/** Slot clearing, status countdowns and persistence lifecycle. */
public final class EntitySlotLifecycle {
    final EntityWarpController warpState;
    final EntityBombState bombInteractions;
    final EntityBossState bossRewards;
    final EntityCombatTables combatTables;
    final EntityEnemyInspection enemyInspection;
    final EntityEventChannels events;
    final EntityLiftState liftThrow;
    final EntityLinkState linkState;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntityPickupState pickups;
    final EntityPresentationState presentationState;
    final EntityProjectileState projectiles;
    final EntityRewardState rewards;
    final EntityRoomVariables roomVariables;
    final EntityStoryState story;
    final GenieEntityController.State genie;
    // Optional ROM data, absent in ROM-free runtimes.
    final RomEnemyCombatTables enemyCombatTables;
    final RoomEntity[] slots;
    final SlimeEyeController.State slimeEye;
    // Optional ROM data, absent in ROM-free runtimes.
    final linksawakening.rom.EntityPropertyTables entityProperties;

    final EntityRoomInspection inspection;
    final EntitySlotPresentation slotPresentation;

    EntitySlotLifecycle(
            EntityWarpController warpState,
            EntityBombState bombInteractions,
            EntityBossState bossRewards,
            EntityCombatTables combatTables,
            EntityEnemyInspection enemyInspection,
            EntityEventChannels events,
            EntityLiftState liftThrow,
            EntityLinkState linkState,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables,
            EntityPickupState pickups,
            EntityPresentationState presentationState,
            EntityProjectileState projectiles,
            EntityRewardState rewards,
            EntityRoomVariables roomVariables,
            EntityStoryState story,
            GenieEntityController.State genie,
            RomEnemyCombatTables enemyCombatTables,
            RoomEntity[] slots,
            SlimeEyeController.State slimeEye,
            linksawakening.rom.EntityPropertyTables entityProperties,
            EntityRoomInspection inspection,
            EntitySlotPresentation slotPresentation) {
        this.inspection = java.util.Objects.requireNonNull(inspection, "inspection");
        this.slotPresentation = java.util.Objects.requireNonNull(slotPresentation, "slotPresentation");

        this.warpState = java.util.Objects.requireNonNull(warpState, "warpState");
        this.bombInteractions = java.util.Objects.requireNonNull(bombInteractions, "bombInteractions");
        this.bossRewards = java.util.Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.enemyInspection = java.util.Objects.requireNonNull(enemyInspection, "enemyInspection");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.liftThrow = java.util.Objects.requireNonNull(liftThrow, "liftThrow");
        this.linkState = java.util.Objects.requireNonNull(linkState, "linkState");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.pickups = java.util.Objects.requireNonNull(pickups, "pickups");
        this.presentationState = java.util.Objects.requireNonNull(presentationState, "presentationState");
        this.projectiles = java.util.Objects.requireNonNull(projectiles, "projectiles");
        this.rewards = java.util.Objects.requireNonNull(rewards, "rewards");
        this.roomVariables = java.util.Objects.requireNonNull(roomVariables, "roomVariables");
        this.story = java.util.Objects.requireNonNull(story, "story");
        this.genie = java.util.Objects.requireNonNull(genie, "genie");
        this.enemyCombatTables = enemyCombatTables;
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
        this.slimeEye = java.util.Objects.requireNonNull(slimeEye, "slimeEye");
        this.entityProperties = entityProperties;
    }

    boolean initializeBossBattle(RoomEntity entity) {
        int options = this.physicsTables.readEntityOptions1Override(entity.slot()) >= 0
                ? this.physicsTables.readEntityOptions1Override(entity.slot())
                : entityProperties == null ? 0 : entityProperties.entityOptions1(entity.type());
        return initializeBossBattle(entity, options);
    }

    private boolean initializeBossBattle(RoomEntity entity, int options) {
        if ((options & 0x80) == 0) return true;
        // $03:48BE-48C4 rejects an already defeated room boss.
        if ((this.roomVariables.readEntityRoomStatus() & 0x30) != 0) {
            disableEntityWithoutPersistence(entity.slot());
            return false;
        }
        this.bossRewards.writeInBossBattle(true);
        return true;
    }

    void initializeHealthGroup(int slot) {
        // ConfigureEntityHealth, $03:489B-489F, before setting initial HP.
        // Active spawns bypass the INIT dispatch; use their fresh ROM options,
        // not an override remaining from the previous occupant of this slot.
        if (this.slots[slot].status() == EntityStatus.ACTIVE) {
            initializeBossBattle(
                    this.slots[slot],
                    entityProperties == null ? 0 : entityProperties.entityOptions1(this.slots[slot].type()));
        }
        this.combatTables.writeEnemyHealthGroup(
                slot, this.enemyCombatTables == null ? 0 : this.enemyCombatTables.healthGroup(this.slots[slot].type()));
    }

    public int transitionSequenceCounter() {
        return this.roomVariables.readTransitionSequenceCounter();
    }

    public int clearEntity(int slot) {
        return EntitySlotLifecycle.entityPersistentSlotResetClearEntity(this, slot);
    }

    public RoomEntity applyZolSplit(RoomEntity original, RoomEntity updated, ZolGelMotion.Split split) {
        return EntitySlotLifecycle.zolSplitControllerApplyZolSplit(this, original, updated, split);
    }

    void finishBurning(RoomEntity entity) {
        int slot = entity.slot();
        this.physicsTables.writeEnemyTransitionCountdown(slot, 0);
        if (entity.type() == EntityRomConstants.ENTITY_GIBDO) {
            EntitySpriteDefinition definition =
                    this.inspection.spriteDefinitionFor(EntityRomConstants.ENTITY_STALFOS_EVASIVE);
            RoomEntity stalfos = withType(entity, EntityRomConstants.ENTITY_STALFOS_EVASIVE, definition);
            this.slots[slot] = withStatus(stalfos, EntityStatus.ACTIVE);
            this.initializeHealthGroup(slot); // $03:4C89 reconfigures attributes/health.
            this.combatTables.writeEnemyHealth(
                    slot, this.inspection.initialHealth(EntityRomConstants.ENTITY_STALFOS_EVASIVE));
            this.combatTables.writeEnemyFlashCountdown(slot, 0);
            this.combatTables.writeEnemyIgnoreHitsCountdown(slot, 0);
            this.physicsTables.writeEntityOptions1Override(slot, -1);
            this.physicsTables.writeEnemyPhysicsFlags(slot, EntityRomConstants.EVASIVE_PHYSICS_FLAGS);
            this.combatTables.writeDyingCountdown(slot, 0);
            this.combatTables.writePowerRecoilDeath(slot, false);
            this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(slot);
            this.motions
                    .get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new)
                    .clear(slot);
            return;
        }

        this.physicsTables.writeEnemyPhysicsFlags(slot, 0x04);
        this.combatTables.writeDyingCountdown(slot, 0x1F);
        this.combatTables.writePowerRecoilDeath(slot, false);
        this.slots[slot] = withDeathPresentation(withStatus(entity, EntityStatus.DYING), 3, false);
        this.events.pendingEntityEvents.add(
                new EntityCombatEvent(slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
    }

    void decrementSlowTransitionCountdown(int slot, int frameCounter) {
        if ((frameCounter & 0x03) == 0
                && this.physicsTables.readSlowTimerInitialized(slot)
                && this.physicsTables.readSlowTransitionCountdown(slot) > 0) {
            this.physicsTables.postSlowTransitionCountdown(slot, -1);
        }
    }

    boolean shouldDisappear(RoomEntity entity) {
        return this.enemyInspection.shouldDisappear(entity);
    }

    public void disableEntityWithoutPersistence(int slot) {
        EntitySlotLifecycle.entityTransientSlotResetDisableEntityWithoutPersistence(this, slot);
    }

    boolean decrementEnemyCombatCountdowns(int slot, boolean decrementIgnoreHits) {
        if (this.combatTables.readEnemyFlashCountdown(slot) > 0) {
            this.combatTables.postEnemyFlashCountdown(slot, -1);
        }
        if (decrementIgnoreHits
                && !this.motions
                        .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                        .isActive(slot)
                && this.combatTables.readEnemyIgnoreHitsCountdown(slot) > 0) {
            this.combatTables.postEnemyIgnoreHitsCountdown(slot, -1);
            return true;
        }
        return false;
    }

    void decrementEnemyStatusCountdowns(int slot) {
        if (this.physicsTables.readEnemyTransitionCountdown(slot) > 0) {
            this.physicsTables.postEnemyTransitionCountdown(slot, -1);
        }
        if (this.combatTables.readEnemyStunnedCountdown(slot) > 0) {
            this.combatTables.postEnemyStunnedCountdown(slot, -1);
        }
        if (this.combatTables.readDyingCountdown(slot) > 0) {
            this.combatTables.postDyingCountdown(slot, -1);
        }
        if (this.bombInteractions.readBombPrivateCountdown1(slot) > 0) {
            this.bombInteractions.postBombPrivateCountdown1(slot, -1);
        }
        if (this.bombInteractions.readBombPrivateCountdown3(slot) > 0) {
            this.bombInteractions.postBombPrivateCountdown3(slot, -1);
        }
        if (this.story.owlState().readOwlPrivateCountdown1(slot) > 0 && this.linkState.entityTimersInteractive()) {
            this.story.owlState().postOwlPrivateCountdown1(slot, -1);
        }
        this.motions.get(HinoxMotion.class, HinoxMotion::new).decrementPrivateCountdown1(slot);
        if (this.liftThrow.readLiftableRockSmashActive(slot)
                && this.liftThrow.readLiftableRockSmashCountdown(slot) > 0) {
            this.liftThrow.postLiftableRockSmashCountdown(slot, -1);
        }
        this.motions.get(BlooperMotion.class, BlooperMotion::new).decrementPrivateCountdown3(slot);
        this.motions.get(WingedOctorokMotion.class, WingedOctorokMotion::new).decrementPrivateCountdown2(slot);
    }

    public void requestPersistentClearMask(int mask) {
        this.roomVariables.writePendingClearedEntityMask(this.roomVariables.readPendingClearedEntityMask() | (mask));
    }

    public void delayInteractions(int countdown) {
        this.roomVariables.writeEntityInteractionCountdown(countdown);
    }

    public void recordGelClinging(boolean clinging) {
        this.motions
                .get(ZolGelMotion.class, ZolGelMotion::new)
                .writeGelClingingToLink(
                        this.motions.get(ZolGelMotion.class, ZolGelMotion::new).readGelClingingToLink() | (clinging));
    }

    public void setDroppedItemBySlot(int slot, int value) {
        this.bossRewards.writeDroppedItemBySlot(slot, value);
    }

    public void setDyingCountdown(int slot, int value) {
        this.combatTables.writeDyingCountdown(slot, value);
    }

    public void setDropPrivateCountdown3(int slot, int value) {
        this.bossRewards.writeDropPrivateCountdown3(slot, value);
    }

    public int warpState(int slot) {
        return this.warpState.readWarpState(slot);
    }

    public void setWarpState(int slot, int value) {
        this.warpState.writeWarpState(slot, value);
    }

    public int warpTransitionCountdown(int slot) {
        return this.warpState.readWarpTransitionCountdown(slot);
    }

    public void setWarpTransitionCountdown(int slot, int value) {
        this.warpState.writeWarpTransitionCountdown(slot, value);
    }

    public int droppablePrivateState3(int slot) {
        return this.bossRewards.readDroppablePrivateState3(slot);
    }

    static int entityPersistentSlotResetClearEntity(EntitySlotLifecycle runtime, int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        entityPersistentSlotResetClearBossState(runtime, slot);
        RoomEntity entity = runtime.slots[slot];
        entityPersistentSlotResetClearPrivateState(runtime, slot);
        if (!entity.loaded()) {
            return 0;
        }
        entityPersistentSlotResetClearSlotTables(runtime, slot);
        entityPersistentSlotResetClearWeaponState(runtime, slot, entity);
        entityPersistentSlotResetClearRoamingMotions(runtime, slot);
        entityPersistentSlotResetClearRemainingMotions(runtime, slot);
        if (runtime.liftThrow.readLiftedEntitySlot() == slot) {
            runtime.liftThrow.writeLiftedEntitySlot(-1);
            runtime.liftThrow.writeLiftedCarryState(0);
            runtime.liftThrow.writeLiftedEffectiveDirection(0);
        }
        runtime.slots[slot] = RoomEntity.disabled(slot);
        runtime.slotPresentation.updatePincerBodyOam(runtime.slots, runtime.motions);
        return entity.sourceLoadOrder() >= 0 && entity.sourceLoadOrder() < 8 ? 1 << entity.sourceLoadOrder() : 0;
    }

    private static void entityPersistentSlotResetClearBossState(EntitySlotLifecycle runtime, int slot) {
        runtime.slimeEye.clear(slot);
        runtime.bossRewards.resetEnemyDropState(slot, runtime.motions);
        runtime.motions
                .get(SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                .clear(slot);
        runtime.motions.get(SideViewPotMotion.class, SideViewPotMotion::new).clear(slot);
        runtime.motions.get(GenieMotion.class, GenieMotion::new).clear(slot);
        if (runtime.genie.genieControllerSlot() == slot) {
            runtime.genie.setGenieControllerSlot(-1);
        }
        runtime.genie.setGenieActiveState(slot, 0);
        runtime.genie.setGeniePrivateState1(slot, 0);
        runtime.genie.setGeniePrivateState2(slot, 0);
        runtime.genie.setGeniePrivateState3(slot, 0);
        runtime.genie.setGeniePrivateState4(slot, 0);
        runtime.genie.setGeniePrivateCountdown1(slot, 0);
        runtime.genie.setGeniePrivateCountdown2(slot, 0);
        runtime.genie.setGenieDirection(slot, 0);
        runtime.genie.setGenieDeathInitialized(slot, false);
        runtime.genie.setGenieSpeedX(slot, 0);
        runtime.genie.setGenieSpeedY(slot, 0);
        runtime.genie.setGenieSpeedZ(slot, 0);
        runtime.genie.setGenieSpeedXAccumulator(slot, 0);
        runtime.genie.setGenieSpeedYAccumulator(slot, 0);
        runtime.genie.setGenieSpeedZAccumulator(slot, 0);
    }

    private static void entityPersistentSlotResetClearPrivateState(EntitySlotLifecycle runtime, int slot) {
        runtime.bombInteractions.setBombFinalPresentationPending(slot, false);
        runtime.combatTables.writeEnemyHitboxFlags(slot, 0);
        runtime.bombInteractions.writePlayerArrowBombArrow(slot, false);
        runtime.bombInteractions.writeBombPrivateCountdown1(slot, 0);
        runtime.bombInteractions.writeBombPrivateCountdown3(slot, 0);
        runtime.projectiles.writeMagicPowderState(slot, 0);
        runtime.projectiles.writeMagicPowderPrivateState4(slot, 0);
        runtime.motions
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .clear(slot);
        runtime.physicsTables.writeEntityPrivateState4(slot, 0);
        runtime.physicsTables.writeEntityInertia(slot, 0);
        runtime.projectiles.writeMusicalNoteInertia(slot, 0);
        runtime.projectiles.writeMusicalNoteSpeedX(slot, 0);
        runtime.projectiles.writeMusicalNoteSpeedY(slot, 0);
        runtime.physicsTables.writeEntityUnknownJ(slot, 0);
        runtime.motions
                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                .setMaskRemovalState(slot, 0);
        runtime.motions
                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                .setMaskSourceHookshotSlot(slot, 0);
        runtime.liftThrow.writeLiftableRockSmashCountdown(slot, 0);
        runtime.liftThrow.writeLiftableRockSmashSourceVariant(slot, 0);
        runtime.liftThrow.writeLiftableRockSmashActive(slot, false);
        if (runtime.bombInteractions.readLatestDroppedBombEntityIndex() == slot) {
            runtime.bombInteractions.writeLatestDroppedBombEntityIndex(-1);
        }
        if (runtime.projectiles.readLatestShotArrowEntityIndex() == slot) {
            runtime.projectiles.writeLatestShotArrowEntityIndex(-1);
        }
        runtime.motions.get(BombiteMotion.class, BombiteMotion::new).clear(slot);
        runtime.motions.get(BlooperMotion.class, BlooperMotion::new).clear(slot);
        runtime.motions.get(PincerMotion.class, PincerMotion::new).clear(slot);
        runtime.motions.get(BushCrawlerMotion.class, BushCrawlerMotion::new).clear(slot);
        runtime.motions.get(PokeyMotion.class, PokeyMotion::new).clear(slot);
        runtime.motions.get(ZoraMotion.class, ZoraMotion::new).clear(slot);
        runtime.motions.get(ZombieMotion.class, ZombieMotion::new).clear(slot);
        runtime.motions.get(BuzzBlobMotion.class, BuzzBlobMotion::new).clear(slot);
        runtime.motions.get(SandCrabMotion.class, SandCrabMotion::new).clear(slot);
        runtime.motions.get(DogMotion.class, DogMotion::new).clear(slot);
        runtime.motions.get(UrchinMotion.class, UrchinMotion::new).clear(slot);
        runtime.motions.get(WitchRatMotion.class, WitchRatMotion::new).clear(slot);
        runtime.motions.get(MimicMotion.class, MimicMotion::new).clear(slot);
        runtime.motions.get(MaskedMimicMotion.class, MaskedMimicMotion::new).clear(slot);
        runtime.motions.get(MiniMoldormMotion.class, MiniMoldormMotion::new).clear(slot);
        runtime.motions.get(MoldormMotion.class, MoldormMotion::new).clear(slot);
    }

    private static void entityPersistentSlotResetClearSlotTables(EntitySlotLifecycle runtime, int slot) {
        runtime.physicsTables.writeSlowTransitionCountdown(slot, 0);
        runtime.physicsTables.writeSlowTimerInitialized(slot, false);
        runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0);
        runtime.combatTables.writeEnemyStunnedCountdown(slot, 0);
        runtime.combatTables.writeDyingCountdown(slot, 0);
        runtime.combatTables.writePowerRecoilDeath(slot, false);
        runtime.physicsTables.writeEnemyPhysicsFlags(slot, 0);
        runtime.combatTables.writeEnemyHealth(slot, 0);
        runtime.combatTables.writeEnemyHealthGroup(slot, 0);
        runtime.combatTables.writeEnemyFlashCountdown(slot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(slot, 0);
        runtime.pickups.writeChestSpeedY(slot, 0);
        runtime.pickups.writeChestSpeedYAccumulator(slot, 0);
        runtime.pickups.writeChestInertia(slot, 0);
        runtime.pickups.writeChestItemBySlot(slot, 0);
        runtime.story.owlState().writeOwlPrivateCountdown1(slot, 0);
        runtime.physicsTables.writeEntityGroundStatus(slot, 0);
        runtime.physicsTables.writeFallingTargetX(slot, 0);
        runtime.physicsTables.writeFallingTargetY(slot, 0);
        runtime.physicsTables.writeFallingSpeedX(slot, 0);
        runtime.physicsTables.writeFallingSpeedY(slot, 0);
        runtime.physicsTables.writeFallingSpeedXAccumulator(slot, 0);
        runtime.physicsTables.writeFallingSpeedYAccumulator(slot, 0);
        runtime.physicsTables.writeFallingVisualYOffset(slot, 0);
        runtime.liftThrow.writeLiftedPhase(slot, 0);
        runtime.liftThrow.writeLiftedSourceDirection(slot, 0);
        runtime.liftThrow.writeLiftedStateInitialized(slot, false);
        runtime.liftThrow.writeThrownDirection(slot, 0xFF);
        runtime.bombInteractions.writeBombDirection(slot, 0xFF);
        runtime.bombInteractions.writeBombPrivateState4(slot, 0);
        runtime.projectiles.writeMagicPowderState(slot, 0);
        runtime.projectiles.writeMagicPowderPrivateState4(slot, 0);
        runtime.bombInteractions.writeBombPrivateCountdown1(slot, 0);
        runtime.bombInteractions.writeBombPrivateCountdown3(slot, 0);
        runtime.bombInteractions.writePlacedBombMotionInitialized(slot, false);
        runtime.liftThrow.writeLedgeTransitionTimer(slot, 0);
        runtime.liftThrow.writeThrownMotionInitialized(slot, false);
    }

    private static void entityPersistentSlotResetClearWeaponState(
            EntitySlotLifecycle runtime, int slot, RoomEntity entity) {
        runtime.motions.get(HookshotChainMotion.class, HookshotChainMotion::new).clear(slot);
        runtime.motions
                .get(HookshotBridgeMotion.class, HookshotBridgeMotion::new)
                .clear(slot);
        runtime.motions.get(BoomerangMotion.class, BoomerangMotion::new).clear(slot);
        runtime.motions
                .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                .clear(slot);
        runtime.motions.get(SwordBeamMotion.class, SwordBeamMotion::new).clear(slot);
        if (entity.type() == EntityRomConstants.ENTITY_HOOKSHOT_CHAIN) {
            runtime.slotPresentation.writeHookshotChainOam(List.of());
        }
    }

    private static void entityPersistentSlotResetClearRoamingMotions(EntitySlotLifecycle runtime, int slot) {
        runtime.physicsTables.writeBaseEntityFlipAttribute(slot, 0);
        runtime.physicsTables.writeEntityOptions1Override(slot, -1);
        runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(slot);
        runtime.motions.get(ColorShellMotion.class, ColorShellMotion::new).clear(slot);
        runtime.motions.get(ButterflyMotion.class, ButterflyMotion::new).clear(slot);
        runtime.motions.get(KeeseMotion.class, KeeseMotion::new).clear(slot);
        runtime.motions.get(RoamingEnemyMotion.class, RoamingEnemyMotion::new).clear(slot);
        runtime.motions
                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                .clear(slot);
        runtime.motions.get(MoblinSwordMotion.class, MoblinSwordMotion::new).clear(slot);
        runtime.motions.get(PlayerArrowMotion.class, PlayerArrowMotion::new).clear(slot);
        runtime.motions.get(TektiteMotion.class, TektiteMotion::new).clear(slot);
        runtime.motions.get(LeeverMotion.class, LeeverMotion::new).clear(slot);
        runtime.motions.get(AntiFairyMotion.class, AntiFairyMotion::new).clear(slot);
        runtime.motions.get(SparkMotion.class, SparkMotion::new).clear(slot);
        runtime.motions.get(ZolGelMotion.class, ZolGelMotion::new).clear(slot);
        runtime.motions.get(PokeyMotion.class, PokeyMotion::new).clear(slot);
        runtime.motions.get(HidingZolMotion.class, HidingZolMotion::new).clear(slot);
        runtime.motions.get(StarMotion.class, StarMotion::new).clear(slot);
        runtime.motions.get(BlooperMotion.class, BlooperMotion::new).clear(slot);
        runtime.motions.get(PincerMotion.class, PincerMotion::new).clear(slot);
        runtime.motions.get(SpikeTrapMotion.class, SpikeTrapMotion::new).clear(slot);
        runtime.motions.get(PairoddMotion.class, PairoddMotion::new).clear(slot);
        runtime.motions
                .get(PairoddProjectileMotion.class, PairoddProjectileMotion::new)
                .clear(slot);
        runtime.motions
                .get(EnemyProjectileMotion.class, EnemyProjectileMotion::new)
                .clear(slot);
        runtime.motions.get(WingedOctorokMotion.class, WingedOctorokMotion::new).clear(slot);
        runtime.motions.get(BushCrawlerMotion.class, BushCrawlerMotion::new).clear(slot);
        runtime.motions.get(CuccoMotion.class, CuccoMotion::new).clear(slot);
        runtime.motions.get(GiantGopongaMotion.class, GiantGopongaMotion::new).clear(slot);
        runtime.motions
                .get(GopongaProjectileMotion.class, GopongaProjectileMotion::new)
                .clear(slot);
        runtime.motions.get(ZoraMotion.class, ZoraMotion::new).clear(slot);
        runtime.motions.get(ZombieMotion.class, ZombieMotion::new).clear(slot);
        runtime.motions.get(BuzzBlobMotion.class, BuzzBlobMotion::new).clear(slot);
        runtime.motions.get(SandCrabMotion.class, SandCrabMotion::new).clear(slot);
        runtime.motions.get(DogMotion.class, DogMotion::new).clear(slot);
        runtime.motions.get(UrchinMotion.class, UrchinMotion::new).clear(slot);
        runtime.motions.get(WitchRatMotion.class, WitchRatMotion::new).clear(slot);
    }

    private static void entityPersistentSlotResetClearRemainingMotions(EntitySlotLifecycle runtime, int slot) {
        runtime.slotPresentation.writeWingedSwordAttackThisFrame(slot, false);
        runtime.slotPresentation.writeWingedStateTwoThisFrame(slot, false);
        runtime.motions.get(LaserMotion.class, LaserMotion::new).clear(slot);
        runtime.motions.get(WaterTektiteMotion.class, WaterTektiteMotion::new).clear(slot);
        runtime.motions.get(FishMotion.class, FishMotion::new).clear(slot);
        runtime.motions.get(CrowMotion.class, CrowMotion::new).clear(slot);
        runtime.motions.get(BooBuddyMotion.class, BooBuddyMotion::new).clear(slot);
        runtime.motions
                .get(StalfosAggressiveMotion.class, StalfosAggressiveMotion::new)
                .clear(slot);
        runtime.motions
                .get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new)
                .clear(slot);
        runtime.motions.get(ThreeOfAKindMotion.class, ThreeOfAKindMotion::new).clear(slot);
        runtime.motions.get(GibdoMotion.class, GibdoMotion::new).clear(slot);
        runtime.motions.get(LikeLikeMotion.class, LikeLikeMotion::new).clear(slot);
        runtime.motions.get(GoombaMotion.class, GoombaMotion::new).clear(slot);
        runtime.motions.get(SnakeMotion.class, SnakeMotion::new).clear(slot);
        runtime.motions.get(WizrobeMotion.class, WizrobeMotion::new).clear(slot);
        runtime.motions
                .get(WizrobeProjectileMotion.class, WizrobeProjectileMotion::new)
                .clear(slot);
        runtime.motions.get(PeaHatMotion.class, PeaHatMotion::new).clear(slot);
        runtime.motions.get(ArmosMotion.class, ArmosMotion::new).clear(slot);
        runtime.motions.get(GhiniMotion.class, GhiniMotion::new).clear(slot);
        runtime.motions.get(HardHatMotion.class, HardHatMotion::new).clear(slot);
        runtime.motions.get(MadBomberMotion.class, MadBomberMotion::new).clear(slot);
        runtime.motions.get(FollowingNpcMotion.class, FollowingNpcMotion::new).clear(slot);
        runtime.motions.get(BowWowMotion.class, BowWowMotion::new).clear(slot);
        runtime.motions.get(MoblinKingMotion.class, MoblinKingMotion::new).clear(slot);
        runtime.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new).clear(slot);
    }

    static void entityTransientSlotResetDisableEntityWithoutPersistence(EntitySlotLifecycle runtime, int slot) {
        entityTransientSlotResetClearBossState(runtime, slot);
        entityTransientSlotResetClearRoomAndRewardState(runtime, slot);
        entityTransientSlotResetClearPhysicsAndWeaponState(runtime, slot);
        entityTransientSlotResetClearRoamingMotions(runtime, slot);
        entityTransientSlotResetClearRemainingMotions(runtime, slot);
        if (runtime.liftThrow.readLiftedEntitySlot() == slot) {
            runtime.liftThrow.writeLiftedEntitySlot(-1);
            runtime.liftThrow.writeLiftedCarryState(0);
            runtime.liftThrow.writeLiftedEffectiveDirection(0);
        }
        runtime.slots[slot] = RoomEntity.disabled(slot);
    }

    private static void entityTransientSlotResetClearBossState(EntitySlotLifecycle runtime, int slot) {
        runtime.slimeEye.clear(slot);
        runtime.bossRewards.resetEnemyDropState(slot, runtime.motions);
        runtime.motions
                .get(SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                .clear(slot);
        runtime.motions.get(SideViewPotMotion.class, SideViewPotMotion::new).clear(slot);
        runtime.motions.get(GenieMotion.class, GenieMotion::new).clear(slot);
        if (runtime.genie.genieControllerSlot() == slot) {
            runtime.genie.setGenieControllerSlot(-1);
        }
        runtime.genie.setGenieActiveState(slot, 0);
        runtime.genie.setGeniePrivateState1(slot, 0);
        runtime.genie.setGeniePrivateState2(slot, 0);
        runtime.genie.setGeniePrivateState3(slot, 0);
        runtime.genie.setGeniePrivateState4(slot, 0);
        runtime.genie.setGeniePrivateCountdown1(slot, 0);
        runtime.genie.setGeniePrivateCountdown2(slot, 0);
        runtime.genie.setGenieDirection(slot, 0);
        runtime.genie.setGenieDeathInitialized(slot, false);
        runtime.genie.setGenieSpeedX(slot, 0);
        runtime.genie.setGenieSpeedY(slot, 0);
        runtime.genie.setGenieSpeedZ(slot, 0);
        runtime.genie.setGenieSpeedXAccumulator(slot, 0);
        runtime.genie.setGenieSpeedYAccumulator(slot, 0);
        runtime.genie.setGenieSpeedZAccumulator(slot, 0);
    }

    private static void entityTransientSlotResetClearRoomAndRewardState(EntitySlotLifecycle runtime, int slot) {
        runtime.motions
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .clear(slot);
        runtime.physicsTables.writeSlowTransitionCountdown(slot, 0);
        runtime.physicsTables.writeSlowTimerInitialized(slot, false);
        runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0);
        runtime.bossRewards.writeBossDeathProducerState(slot, 0);
        runtime.motions.get(MoldormMotion.class, MoldormMotion::new).setDestructionTailState(slot, 0);
        runtime.combatTables.writeEnemyStunnedCountdown(slot, 0);
        runtime.combatTables.writeDyingCountdown(slot, 0);
        runtime.combatTables.writePowerRecoilDeath(slot, false);
        runtime.physicsTables.writeEnemyPhysicsFlags(slot, 0);
        runtime.combatTables.writeEnemyHealth(slot, 0);
        runtime.combatTables.writeEnemyHealthGroup(slot, 0);
        runtime.combatTables.writeEnemyFlashCountdown(slot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(slot, 0);
        runtime.pickups.writeChestSpeedY(slot, 0);
        runtime.pickups.writeChestSpeedYAccumulator(slot, 0);
        runtime.pickups.writeChestInertia(slot, 0);
        runtime.pickups.writeChestItemBySlot(slot, 0);
        runtime.combatTables.writeEnemyHitboxFlags(slot, 0);
        runtime.rewards.writeInstrumentParticleKind(slot, 0);
        runtime.rewards.writeInstrumentParticleSpeedX(slot, 0);
        runtime.rewards.writeInstrumentParticleSpeedY(slot, 0);
        runtime.rewards.writeInstrumentParticleXAccumulator(slot, 0);
        runtime.rewards.writeInstrumentParticleYAccumulator(slot, 0);
        runtime.motions.get(ArmosKnightMotion.class, ArmosKnightMotion::new).clear(slot);
    }

    private static void entityTransientSlotResetClearPhysicsAndWeaponState(EntitySlotLifecycle runtime, int slot) {
        runtime.physicsTables.writeEntityGroundStatus(slot, 0);
        runtime.physicsTables.writeFallingTargetX(slot, 0);
        runtime.physicsTables.writeFallingTargetY(slot, 0);
        runtime.physicsTables.writeFallingSpeedX(slot, 0);
        runtime.physicsTables.writeFallingSpeedY(slot, 0);
        runtime.physicsTables.writeFallingSpeedXAccumulator(slot, 0);
        runtime.physicsTables.writeFallingSpeedYAccumulator(slot, 0);
        runtime.physicsTables.writeFallingVisualYOffset(slot, 0);
        runtime.liftThrow.writeLiftedPhase(slot, 0);
        runtime.liftThrow.writeLiftedSourceDirection(slot, 0);
        runtime.liftThrow.writeLiftedStateInitialized(slot, false);
        runtime.liftThrow.writeThrownDirection(slot, 0xFF);
        runtime.bombInteractions.writeBombDirection(slot, 0xFF);
        runtime.bombInteractions.writeBombPrivateState4(slot, 0);
        runtime.projectiles.writeMagicPowderState(slot, 0);
        runtime.projectiles.writeMagicPowderPrivateState4(slot, 0);
        runtime.bombInteractions.writeBombPrivateCountdown1(slot, 0);
        runtime.bombInteractions.writeBombPrivateCountdown3(slot, 0);
        runtime.motions
                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                .setMaskRemovalState(slot, 0);
        runtime.motions
                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                .setMaskSourceHookshotSlot(slot, 0);
        runtime.liftThrow.writeLiftableRockSmashCountdown(slot, 0);
        runtime.liftThrow.writeLiftableRockSmashSourceVariant(slot, 0);
        runtime.liftThrow.writeLiftableRockSmashActive(slot, false);
        runtime.bombInteractions.setBombFinalPresentationPending(slot, false);
        runtime.bombInteractions.writePlacedBombMotionInitialized(slot, false);
        runtime.bombInteractions.writeEnemyBombMotionInitialized(slot, false);
        runtime.liftThrow.writeLedgeTransitionTimer(slot, 0);
        runtime.liftThrow.writeThrownMotionInitialized(slot, false);
        runtime.physicsTables.writeBaseEntityFlipAttribute(slot, 0);
        runtime.physicsTables.writeEntityOptions1Override(slot, -1);
        runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(slot);
        runtime.motions.get(BomberMotion.class, BomberMotion::new).clear(slot);
    }

    private static void entityTransientSlotResetClearRoamingMotions(EntitySlotLifecycle runtime, int slot) {
        runtime.motions.get(MadBomberMotion.class, MadBomberMotion::new).clear(slot);
        runtime.motions.get(BombiteMotion.class, BombiteMotion::new).clear(slot);
        runtime.motions.get(ColorShellMotion.class, ColorShellMotion::new).clear(slot);
        runtime.motions.get(ButterflyMotion.class, ButterflyMotion::new).clear(slot);
        runtime.motions.get(KeeseMotion.class, KeeseMotion::new).clear(slot);
        runtime.motions.get(RoamingEnemyMotion.class, RoamingEnemyMotion::new).clear(slot);
        runtime.motions
                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                .clear(slot);
        runtime.motions.get(MoblinSwordMotion.class, MoblinSwordMotion::new).clear(slot);
        runtime.motions.get(TektiteMotion.class, TektiteMotion::new).clear(slot);
        runtime.motions.get(LeeverMotion.class, LeeverMotion::new).clear(slot);
        runtime.motions.get(AntiFairyMotion.class, AntiFairyMotion::new).clear(slot);
        runtime.motions.get(SparkMotion.class, SparkMotion::new).clear(slot);
        runtime.motions.get(ZolGelMotion.class, ZolGelMotion::new).clear(slot);
        runtime.motions.get(HidingZolMotion.class, HidingZolMotion::new).clear(slot);
        runtime.motions.get(StarMotion.class, StarMotion::new).clear(slot);
        runtime.motions.get(BlooperMotion.class, BlooperMotion::new).clear(slot);
        runtime.motions.get(PincerMotion.class, PincerMotion::new).clear(slot);
        runtime.motions.get(BushCrawlerMotion.class, BushCrawlerMotion::new).clear(slot);
        runtime.motions.get(PokeyMotion.class, PokeyMotion::new).clear(slot);
        runtime.motions.get(PiranhaMotion.class, PiranhaMotion::new).clear(slot);
        runtime.motions.get(ZoraMotion.class, ZoraMotion::new).clear(slot);
        runtime.motions.get(ZombieMotion.class, ZombieMotion::new).clear(slot);
        runtime.motions.get(BuzzBlobMotion.class, BuzzBlobMotion::new).clear(slot);
        runtime.motions.get(SandCrabMotion.class, SandCrabMotion::new).clear(slot);
        runtime.motions.get(DogMotion.class, DogMotion::new).clear(slot);
        runtime.motions.get(MimicMotion.class, MimicMotion::new).clear(slot);
        runtime.motions.get(MaskedMimicMotion.class, MaskedMimicMotion::new).clear(slot);
        runtime.motions.get(MiniMoldormMotion.class, MiniMoldormMotion::new).clear(slot);
        runtime.motions.get(MoldormMotion.class, MoldormMotion::new).clear(slot);
        runtime.motions.get(CuccoMotion.class, CuccoMotion::new).clear(slot);
        runtime.motions.get(SpikeTrapMotion.class, SpikeTrapMotion::new).clear(slot);
        runtime.motions.get(PairoddMotion.class, PairoddMotion::new).clear(slot);
        runtime.motions
                .get(PairoddProjectileMotion.class, PairoddProjectileMotion::new)
                .clear(slot);
        runtime.motions.get(PlayerArrowMotion.class, PlayerArrowMotion::new).clear(slot);
        runtime.motions
                .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                .clear(slot);
        runtime.bombInteractions.writePlayerArrowBombArrow(slot, false);
    }

    private static void entityTransientSlotResetClearRemainingMotions(EntitySlotLifecycle runtime, int slot) {
        if (runtime.bombInteractions.readLatestDroppedBombEntityIndex() == slot) {
            runtime.bombInteractions.writeLatestDroppedBombEntityIndex(-1);
        }
        if (runtime.projectiles.readLatestShotArrowEntityIndex() == slot) {
            runtime.projectiles.writeLatestShotArrowEntityIndex(-1);
        }
        runtime.motions
                .get(EnemyProjectileMotion.class, EnemyProjectileMotion::new)
                .clear(slot);
        runtime.motions.get(LaserMotion.class, LaserMotion::new).clear(slot);
        runtime.motions.get(WaterTektiteMotion.class, WaterTektiteMotion::new).clear(slot);
        runtime.motions.get(FishMotion.class, FishMotion::new).clear(slot);
        runtime.motions.get(CrowMotion.class, CrowMotion::new).clear(slot);
        runtime.motions.get(BooBuddyMotion.class, BooBuddyMotion::new).clear(slot);
        runtime.motions
                .get(StalfosAggressiveMotion.class, StalfosAggressiveMotion::new)
                .clear(slot);
        runtime.motions
                .get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new)
                .clear(slot);
        runtime.motions.get(GibdoMotion.class, GibdoMotion::new).clear(slot);
        runtime.motions.get(GoombaMotion.class, GoombaMotion::new).clear(slot);
        runtime.motions.get(SnakeMotion.class, SnakeMotion::new).clear(slot);
        runtime.motions.get(WizrobeMotion.class, WizrobeMotion::new).clear(slot);
        runtime.motions
                .get(WizrobeProjectileMotion.class, WizrobeProjectileMotion::new)
                .clear(slot);
        runtime.motions.get(PeaHatMotion.class, PeaHatMotion::new).clear(slot);
        runtime.motions.get(ArmosMotion.class, ArmosMotion::new).clear(slot);
        runtime.motions.get(FollowingNpcMotion.class, FollowingNpcMotion::new).clear(slot);
        runtime.motions.get(GhiniMotion.class, GhiniMotion::new).clear(slot);
        runtime.motions.get(HardHatMotion.class, HardHatMotion::new).clear(slot);
        runtime.motions.get(PolsVoiceMotion.class, PolsVoiceMotion::new).clear(slot);
        runtime.motions.get(SpikedBeetleMotion.class, SpikedBeetleMotion::new).clear(slot);
        runtime.motions.get(BowWowMotion.class, BowWowMotion::new).clear(slot);
        runtime.motions.get(MoblinKingMotion.class, MoblinKingMotion::new).clear(slot);
        runtime.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new).clear(slot);
    }

    static RoomEntity zolSplitControllerApplyZolSplit(
            EntitySlotLifecycle runtime, RoomEntity original, RoomEntity updated, ZolGelMotion.Split split) {
        EntitySpriteDefinition gelDefinition = runtime.inspection.spriteDefinitionFor(EntityRomConstants.ENTITY_GEL);
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
        runtime.physicsTables.writeEnemyPhysicsFlags(slot, initialPhysicsFlags(EntityRomConstants.ENTITY_GEL));
        runtime.combatTables.writeEnemyHitboxFlags(slot, initialHitboxFlags(EntityRomConstants.ENTITY_GEL));
        runtime.physicsTables.writeEntityOptions1Override(slot, -1);
        runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0);
        runtime.combatTables.writeEnemyStunnedCountdown(slot, 0);
        runtime.combatTables.writeEnemyHealthGroup(
                slot,
                runtime.enemyCombatTables == null
                        ? 0
                        : runtime.enemyCombatTables.healthGroup(EntityRomConstants.ENTITY_GEL));
        runtime.combatTables.writeEnemyHealth(slot, runtime.inspection.initialHealth(EntityRomConstants.ENTITY_GEL));
        runtime.combatTables.writeEnemyFlashCountdown(slot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(slot, 0);
        runtime.combatTables.writeDyingCountdown(slot, 0);
        runtime.combatTables.writePowerRecoilDeath(slot, false);
        runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(slot);

        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
        if (freeSlot >= 0) {
            runtime.physicsTables.writeBaseEntityFlipAttribute(
                    freeSlot, runtime.physicsTables.readBaseEntityFlipAttribute(original.slot()));
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
            runtime.physicsTables.writeEnemyPhysicsFlags(freeSlot, initialPhysicsFlags(EntityRomConstants.ENTITY_GEL));
            runtime.combatTables.writeEnemyHitboxFlags(freeSlot, initialHitboxFlags(EntityRomConstants.ENTITY_GEL));
            runtime.physicsTables.writeEntityOptions1Override(freeSlot, -1);
            runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
            runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
            runtime.combatTables.writeDyingCountdown(freeSlot, 0);
            runtime.combatTables.writePowerRecoilDeath(freeSlot, false);
            runtime.combatTables.writeEnemyHealth(
                    freeSlot, runtime.inspection.initialHealth(EntityRomConstants.ENTITY_GEL));
            runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
            // SpawnNewEntity sets the new entity's ignore-hits countdown to 1.
            runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
            runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
            runtime.motions.get(ZolGelMotion.class, ZolGelMotion::new).prepareSpawnedGel(freeSlot);
        }
        return gel;
    }
}
