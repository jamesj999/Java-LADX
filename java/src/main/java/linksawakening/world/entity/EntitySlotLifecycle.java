package linksawakening.world.entity;

import static linksawakening.world.entity.EntitySnapshots.withDeathPresentation;
import static linksawakening.world.entity.EntitySnapshots.withStatus;
import static linksawakening.world.entity.EntitySnapshots.withType;
import static linksawakening.world.entity.EntityTypeRules.initialHitboxFlags;

import java.util.List;
import java.util.Objects;
import java.util.function.IntUnaryOperator;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.AntiFairyMotion;
import linksawakening.world.entity.motion.ArmosKnightMotion;
import linksawakening.world.entity.motion.ArmosMotion;
import linksawakening.world.entity.motion.BlooperMotion;
import linksawakening.world.entity.motion.BomberMotion;
import linksawakening.world.entity.motion.BombiteMotion;
import linksawakening.world.entity.motion.BooBuddyMotion;
import linksawakening.world.entity.motion.BoomerangMotion;
import linksawakening.world.entity.motion.BowWowMotion;
import linksawakening.world.entity.motion.BushCrawlerMotion;
import linksawakening.world.entity.motion.ButterflyMotion;
import linksawakening.world.entity.motion.BuzzBlobMotion;
import linksawakening.world.entity.motion.ColorShellMotion;
import linksawakening.world.entity.motion.CrowMotion;
import linksawakening.world.entity.motion.CuccoMotion;
import linksawakening.world.entity.motion.DogMotion;
import linksawakening.world.entity.motion.EnemyProjectileMotion;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.FishMotion;
import linksawakening.world.entity.motion.FollowingNpcMotion;
import linksawakening.world.entity.motion.GenieMotion;
import linksawakening.world.entity.motion.GhiniMotion;
import linksawakening.world.entity.motion.GiantGopongaMotion;
import linksawakening.world.entity.motion.GibdoMotion;
import linksawakening.world.entity.motion.GoombaMotion;
import linksawakening.world.entity.motion.GopongaProjectileMotion;
import linksawakening.world.entity.motion.HardHatMotion;
import linksawakening.world.entity.motion.HidingZolMotion;
import linksawakening.world.entity.motion.HinoxMotion;
import linksawakening.world.entity.motion.HookshotBridgeMotion;
import linksawakening.world.entity.motion.HookshotChainMotion;
import linksawakening.world.entity.motion.KeeseMotion;
import linksawakening.world.entity.motion.LaserMotion;
import linksawakening.world.entity.motion.LeeverMotion;
import linksawakening.world.entity.motion.LikeLikeMotion;
import linksawakening.world.entity.motion.MadBomberMotion;
import linksawakening.world.entity.motion.MagicRodFireballMotion;
import linksawakening.world.entity.motion.MaskedMimicMotion;
import linksawakening.world.entity.motion.MimicMotion;
import linksawakening.world.entity.motion.MiniMoldormMotion;
import linksawakening.world.entity.motion.MoblinKingMotion;
import linksawakening.world.entity.motion.MoblinSwordMotion;
import linksawakening.world.entity.motion.MoldormMotion;
import linksawakening.world.entity.motion.PairoddMotion;
import linksawakening.world.entity.motion.PairoddProjectileMotion;
import linksawakening.world.entity.motion.PeaHatMotion;
import linksawakening.world.entity.motion.PincerMotion;
import linksawakening.world.entity.motion.PiranhaMotion;
import linksawakening.world.entity.motion.PlayerArrowMotion;
import linksawakening.world.entity.motion.PokeyMotion;
import linksawakening.world.entity.motion.PolsVoiceMotion;
import linksawakening.world.entity.motion.RoamingEnemyMotion;
import linksawakening.world.entity.motion.SandCrabMotion;
import linksawakening.world.entity.motion.SecretSeashellMotion;
import linksawakening.world.entity.motion.SideViewPlatformMotion;
import linksawakening.world.entity.motion.SideViewPotMotion;
import linksawakening.world.entity.motion.SnakeMotion;
import linksawakening.world.entity.motion.SparkMotion;
import linksawakening.world.entity.motion.SpikeTrapMotion;
import linksawakening.world.entity.motion.SpikedBeetleMotion;
import linksawakening.world.entity.motion.StalfosAggressiveMotion;
import linksawakening.world.entity.motion.StalfosEvasiveMotion;
import linksawakening.world.entity.motion.StarMotion;
import linksawakening.world.entity.motion.SwordBeamMotion;
import linksawakening.world.entity.motion.TektiteMotion;
import linksawakening.world.entity.motion.ThreeOfAKindMotion;
import linksawakening.world.entity.motion.ThrownEntityMotion;
import linksawakening.world.entity.motion.UnmaskedIronMaskMotion;
import linksawakening.world.entity.motion.UrchinMotion;
import linksawakening.world.entity.motion.WaterTektiteMotion;
import linksawakening.world.entity.motion.WingedOctorokMotion;
import linksawakening.world.entity.motion.WitchRatMotion;
import linksawakening.world.entity.motion.WizrobeMotion;
import linksawakening.world.entity.motion.WizrobeProjectileMotion;
import linksawakening.world.entity.motion.ZolGelMotion;
import linksawakening.world.entity.motion.ZombieMotion;
import linksawakening.world.entity.motion.ZoraMotion;

/** ConfigureEntityHealth and the ROM slot status, countdown and persistence reset paths. */
public final class EntitySlotLifecycle {
    private final EntityWarpController warpState;
    private final EntityBombState bombInteractions;
    private final EntityBossState bossRewards;
    private final EntityCombatTables combatTables;
    private final EntityEnemyInspection enemyInspection;
    private final EntityEventChannels events;
    private final EntityLiftState liftThrow;
    private final EntityLinkState linkState;
    private final EntityMotionRegistry motions;
    private final EntityPhysicsTables physicsTables;
    private final EntityPickupState pickups;
    private final EntityProjectileState projectiles;
    private final EntityRewardState rewards;
    private final EntityRoomVariables roomVariables;
    private final EntityStoryState story;
    private final GenieEntityController.State genie;
    private final RomEnemyCombatTables enemyCombatTables;
    private final EntitySlots slots;
    private final SlimeEyeController.State slimeEye;
    // Optional ROM data, absent in ROM-free runtimes.
    private final IntUnaryOperator entityProperties;

    private final EntityRoomInspection inspection;
    private final EntitySlotPresentation slotPresentation;

    EntitySlotLifecycle(
            EntityWarpController warpState,
            EntityBombState bombInteractions,
            EntityBossState bossRewards,
            EntityEnemyInspection enemyInspection,
            EntityEventChannels events,
            EntityLiftState liftThrow,
            EntityLinkState linkState,
            EntityMotionRegistry motions,
            EntityPickupState pickups,
            EntityPresentationState presentationState,
            EntityProjectileState projectiles,
            EntityRewardState rewards,
            EntityRoomVariables roomVariables,
            EntityStoryState story,
            GenieEntityController.State genie,
            RomEnemyCombatTables enemyCombatTables,
            EntitySlots slots,
            SlimeEyeController.State slimeEye,
            IntUnaryOperator entityProperties,
            EntityRoomInspection inspection,
            EntitySlotPresentation slotPresentation) {
        EntityCombatTables combatTables = slots.combat();
        EntityPhysicsTables physicsTables = slots.physics();
        this.inspection = Objects.requireNonNull(inspection, "inspection");
        this.slotPresentation = Objects.requireNonNull(slotPresentation, "slotPresentation");

        this.warpState = Objects.requireNonNull(warpState, "warpState");
        this.bombInteractions = Objects.requireNonNull(bombInteractions, "bombInteractions");
        this.bossRewards = Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = Objects.requireNonNull(combatTables, "combatTables");
        this.enemyInspection = Objects.requireNonNull(enemyInspection, "enemyInspection");
        this.events = Objects.requireNonNull(events, "events");
        this.liftThrow = Objects.requireNonNull(liftThrow, "liftThrow");
        this.linkState = Objects.requireNonNull(linkState, "linkState");
        this.motions = Objects.requireNonNull(motions, "motions");
        this.physicsTables = Objects.requireNonNull(physicsTables, "physicsTables");
        this.pickups = Objects.requireNonNull(pickups, "pickups");
        Objects.requireNonNull(presentationState, "presentationState");
        this.projectiles = Objects.requireNonNull(projectiles, "projectiles");
        this.rewards = Objects.requireNonNull(rewards, "rewards");
        this.roomVariables = Objects.requireNonNull(roomVariables, "roomVariables");
        this.story = Objects.requireNonNull(story, "story");
        this.genie = Objects.requireNonNull(genie, "genie");
        this.enemyCombatTables = Objects.requireNonNull(enemyCombatTables, "combat rules");
        this.slots = Objects.requireNonNull(slots, "slots");
        this.slimeEye = Objects.requireNonNull(slimeEye, "slimeEye");
        this.entityProperties = Objects.requireNonNull(entityProperties, "entity options");
    }

    boolean initializeBossBattle(RoomEntity entity) {
        int options = this.physicsTables.readEntityOptions1Override(entity.slot()) >= 0
                ? this.physicsTables.readEntityOptions1Override(entity.slot())
                : entityProperties.applyAsInt(entity.type());
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
        if (this.slots.get(slot).status() == EntityStatus.ACTIVE) {
            initializeBossBattle(
                    this.slots.get(slot),
                    entityProperties.applyAsInt(this.slots.get(slot).type()));
        }
        this.combatTables.writeEnemyHealthGroup(
                slot, this.enemyCombatTables.healthGroup(this.slots.get(slot).type()));
    }

    public int transitionSequenceCounter() {
        return this.roomVariables.readTransitionSequenceCounter();
    }

    public int clearEntity(int slot) {
        if (slot < 0 || slot >= this.slots.size()) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        entityPersistentSlotResetClearBossState(this, slot);
        RoomEntity entity = this.slots.get(slot);
        entityPersistentSlotResetClearPrivateState(this, slot);
        if (!entity.loaded()) {
            return 0;
        }
        entityPersistentSlotResetClearSlotTables(this, slot);
        entityPersistentSlotResetClearWeaponState(this, slot, entity);
        entityPersistentSlotResetClearRoamingMotions(this, slot);
        entityPersistentSlotResetClearRemainingMotions(this, slot);
        if (this.liftThrow.readLiftedEntitySlot() == slot) {
            this.liftThrow.writeLiftedEntitySlot(-1);
            this.liftThrow.writeLiftedCarryState(0);
            this.liftThrow.writeLiftedEffectiveDirection(0);
        }
        this.slots.clear(slot);
        this.slotPresentation.updatePincerBodyOam(this.slots, this.motions);
        return entity.sourceLoadOrder() >= 0 && entity.sourceLoadOrder() < 8 ? 1 << entity.sourceLoadOrder() : 0;
    }

    public RoomEntity applyZolSplit(RoomEntity original, RoomEntity updated, ZolGelMotion.Split split) {
        EntitySpriteDefinition gelDefinition = this.inspection.spriteDefinitionFor(EntityRomConstants.ENTITY_GEL);
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
        this.physicsTables.initializeEnemyPhysicsFlags(slot, EntityRomConstants.ENTITY_GEL);
        this.combatTables.writeEnemyHitboxFlags(slot, initialHitboxFlags(EntityRomConstants.ENTITY_GEL));
        this.physicsTables.writeEntityOptions1Override(slot, -1);
        this.physicsTables.writeEnemyTransitionCountdown(slot, 0);
        this.combatTables.writeEnemyStunnedCountdown(slot, 0);
        this.combatTables.writeEnemyHealthGroup(
                slot, this.enemyCombatTables.healthGroup(EntityRomConstants.ENTITY_GEL));
        this.combatTables.writeEnemyHealth(slot, this.inspection.initialHealth(EntityRomConstants.ENTITY_GEL));
        this.combatTables.writeEnemyFlashCountdown(slot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(slot, 0);
        this.combatTables.writeDyingCountdown(slot, 0);
        this.combatTables.writePowerRecoilDeath(slot, false);
        var enemyRecoilMotion = this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(slot);

        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot >= 0) {
            this.physicsTables.writeBaseEntityFlipAttribute(
                    freeSlot, this.physicsTables.readBaseEntityFlipAttribute(original.slot()));
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
            this.slots.replace(freeSlot, spawnedGel);
            this.initializeHealthGroup(freeSlot);
            this.physicsTables.initializeEnemyPhysicsFlags(freeSlot, EntityRomConstants.ENTITY_GEL);
            this.combatTables.writeEnemyHitboxFlags(freeSlot, initialHitboxFlags(EntityRomConstants.ENTITY_GEL));
            this.physicsTables.writeEntityOptions1Override(freeSlot, -1);
            this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
            this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
            this.combatTables.writeDyingCountdown(freeSlot, 0);
            this.combatTables.writePowerRecoilDeath(freeSlot, false);
            this.combatTables.writeEnemyHealth(freeSlot, this.inspection.initialHealth(EntityRomConstants.ENTITY_GEL));
            this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
            // SpawnNewEntity sets the new entity's ignore-hits countdown to 1.
            this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
            enemyRecoilMotion.clear(freeSlot);
            var zolGelMotion = this.motions.get(ZolGelMotion.class, ZolGelMotion::new);
            zolGelMotion.prepareSpawnedGel(freeSlot);
        }
        return gel;
    }

    void finishBurning(RoomEntity entity) {
        int slot = entity.slot();
        this.physicsTables.writeEnemyTransitionCountdown(slot, 0);
        if (entity.type() == EntityRomConstants.ENTITY_GIBDO) {
            EntitySpriteDefinition definition =
                    this.inspection.spriteDefinitionFor(EntityRomConstants.ENTITY_STALFOS_EVASIVE);
            RoomEntity stalfos = withType(entity, EntityRomConstants.ENTITY_STALFOS_EVASIVE, definition);
            this.slots.replace(slot, withStatus(stalfos, EntityStatus.ACTIVE));
            this.initializeHealthGroup(slot); // $03:4C89 reconfigures attributes/health.
            this.combatTables.writeEnemyHealth(
                    slot, this.inspection.initialHealth(EntityRomConstants.ENTITY_STALFOS_EVASIVE));
            this.combatTables.writeEnemyFlashCountdown(slot, 0);
            this.combatTables.writeEnemyIgnoreHitsCountdown(slot, 0);
            this.physicsTables.writeEntityOptions1Override(slot, -1);
            this.physicsTables.writeEnemyPhysicsFlags(slot, EntityRomConstants.EVASIVE_PHYSICS_FLAGS);
            this.combatTables.writeDyingCountdown(slot, 0);
            this.combatTables.writePowerRecoilDeath(slot, false);
            var enemyRecoilMotion = this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
            enemyRecoilMotion.clear(slot);
            var stalfosEvasiveMotion = this.motions.get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new);
            stalfosEvasiveMotion.clear(slot);
            return;
        }

        this.physicsTables.writeEnemyPhysicsFlags(slot, 0x04);
        this.combatTables.writeDyingCountdown(slot, 0x1F);
        this.combatTables.writePowerRecoilDeath(slot, false);
        this.slots.replace(slot, withDeathPresentation(withStatus(entity, EntityStatus.DYING), 3, false));
        this.events.sound(slot, entity.type(), EntityCombatEvent.SoundChannel.NOISE, 0x13);
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
        entityTransientSlotResetClearBossState(this, slot);
        entityTransientSlotResetClearRoomAndRewardState(this, slot);
        entityTransientSlotResetClearPhysicsAndWeaponState(this, slot);
        entityTransientSlotResetClearRoamingMotions(this, slot);
        entityTransientSlotResetClearRemainingMotions(this, slot);
        if (this.liftThrow.readLiftedEntitySlot() == slot) {
            this.liftThrow.writeLiftedEntitySlot(-1);
            this.liftThrow.writeLiftedCarryState(0);
            this.liftThrow.writeLiftedEffectiveDirection(0);
        }
        this.slots.clear(slot);
    }

    boolean decrementEnemyCombatCountdowns(int slot, boolean decrementIgnoreHits) {
        if (this.combatTables.readEnemyFlashCountdown(slot) > 0) {
            this.combatTables.postEnemyFlashCountdown(slot, -1);
        }
        var enemyRecoilMotion = this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        if (decrementIgnoreHits
                && !enemyRecoilMotion.isActive(slot)
                && this.combatTables.readEnemyIgnoreHitsCountdown(slot) > 0) {
            this.combatTables.postEnemyIgnoreHitsCountdown(slot, -1);
            return true;
        }
        return false;
    }

    void decrementEnemyStatusCountdowns(int slot, boolean tickDyingCountdown) {
        if (this.physicsTables.readEnemyTransitionCountdown(slot) > 0) {
            this.physicsTables.postEnemyTransitionCountdown(slot, -1);
        }
        if (this.combatTables.readEnemyStunnedCountdown(slot) > 0) {
            this.combatTables.postEnemyStunnedCountdown(slot, -1);
        }
        if (tickDyingCountdown && this.combatTables.readDyingCountdown(slot) > 0) {
            this.combatTables.postDyingCountdown(slot, -1);
        }
        if (this.bombInteractions.readBombCollisionGraceCountdown(slot) > 0) {
            this.bombInteractions.postBombCollisionGraceCountdown(slot, -1);
        }
        if (this.bombInteractions.readBombLiftDelay(slot) > 0) {
            this.bombInteractions.postBombLiftDelay(slot, -1);
        }
        if (this.story.owlState().readOwlPrivateCountdown1(slot) > 0 && this.linkState.entityTimersInteractive()) {
            this.story.owlState().postOwlPrivateCountdown1(slot, -1);
        }
        var hinoxMotion = this.motions.get(HinoxMotion.class, HinoxMotion::new);
        hinoxMotion.decrementPrivateCountdown1(slot);
        if (this.liftThrow.readLiftableRockSmashActive(slot)
                && this.liftThrow.readLiftableRockSmashCountdown(slot) > 0) {
            this.liftThrow.postLiftableRockSmashCountdown(slot, -1);
        }
        var blooperMotion = this.motions.get(BlooperMotion.class, BlooperMotion::new);
        blooperMotion.decrementPrivateCountdown3(slot);
        var wingedOctorokMotion = this.motions.get(WingedOctorokMotion.class, WingedOctorokMotion::new);
        wingedOctorokMotion.decrementPrivateCountdown2(slot);
    }

    public void requestPersistentClearMask(int mask) {
        this.roomVariables.writePendingClearedEntityMask(this.roomVariables.readPendingClearedEntityMask() | mask);
    }

    public void delayInteractions(int countdown) {
        this.roomVariables.writeEntityInteractionCountdown(countdown);
    }

    // Preserve the eager ROM clinging-state accumulator merge across slots.
    @SuppressWarnings("ShortCircuitBoolean")
    public void recordGelClinging(boolean clinging) {
        var zolGelMotion = this.motions.get(ZolGelMotion.class, ZolGelMotion::new);
        zolGelMotion.writeGelClingingToLink(zolGelMotion.readGelClingingToLink() | clinging);
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

    private static void entityPersistentSlotResetClearBossState(EntitySlotLifecycle runtime, int slot) {
        runtime.slimeEye.clear(slot);
        runtime.bossRewards.resetEnemyDropState(slot, runtime.motions);
        var sideViewPlatformMotion = runtime.motions.get(SideViewPlatformMotion.class, SideViewPlatformMotion::new);
        sideViewPlatformMotion.clear(slot);
        var sideViewPotMotion = runtime.motions.get(SideViewPotMotion.class, SideViewPotMotion::new);
        sideViewPotMotion.clear(slot);
        var genieMotion = runtime.motions.get(GenieMotion.class, GenieMotion::new);
        genieMotion.clear(slot);
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
        runtime.bombInteractions.writeBombCollisionGraceCountdown(slot, 0);
        runtime.bombInteractions.writeBombLiftDelay(slot, 0);
        runtime.projectiles.writeMagicPowderState(slot, 0);
        runtime.projectiles.writePowderSpent(slot, 0);
        var secretSeashellMotion = runtime.motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new);
        secretSeashellMotion.clear(slot);
        runtime.physicsTables.writeEntityPrivateState4(slot, 0);
        runtime.physicsTables.writeEntityInertia(slot, 0);
        runtime.projectiles.writeMusicalNoteInertia(slot, 0);
        runtime.projectiles.writeMusicalNoteSpeedX(slot, 0);
        runtime.projectiles.writeMusicalNoteSpeedY(slot, 0);
        runtime.physicsTables.writeEntityUnknownJ(slot, 0);
        var unmaskedIronMaskMotion = runtime.motions.get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new);
        unmaskedIronMaskMotion.setMaskRemovalState(slot, 0);
        unmaskedIronMaskMotion.setMaskSourceHookshotSlot(slot, 0);
        runtime.liftThrow.writeLiftableRockSmashCountdown(slot, 0);
        runtime.liftThrow.writeLiftableRockSmashSourceVariant(slot, 0);
        runtime.liftThrow.writeLiftableRockSmashActive(slot, false);
        if (runtime.bombInteractions.readLatestDroppedBombEntityIndex() == slot) {
            runtime.bombInteractions.writeLatestDroppedBombEntityIndex(-1);
        }
        if (runtime.projectiles.readLatestShotArrowEntityIndex() == slot) {
            runtime.projectiles.writeLatestShotArrowEntityIndex(-1);
        }
        var bombiteMotion = runtime.motions.get(BombiteMotion.class, BombiteMotion::new);
        bombiteMotion.clear(slot);
        var blooperMotion = runtime.motions.get(BlooperMotion.class, BlooperMotion::new);
        blooperMotion.clear(slot);
        var pincerMotion = runtime.motions.get(PincerMotion.class, PincerMotion::new);
        pincerMotion.clear(slot);
        var bushCrawlerMotion = runtime.motions.get(BushCrawlerMotion.class, BushCrawlerMotion::new);
        bushCrawlerMotion.clear(slot);
        var pokeyMotion = runtime.motions.get(PokeyMotion.class, PokeyMotion::new);
        pokeyMotion.clear(slot);
        var zoraMotion = runtime.motions.get(ZoraMotion.class, ZoraMotion::new);
        zoraMotion.clear(slot);
        var zombieMotion = runtime.motions.get(ZombieMotion.class, ZombieMotion::new);
        zombieMotion.clear(slot);
        var buzzBlobMotion = runtime.motions.get(BuzzBlobMotion.class, BuzzBlobMotion::new);
        buzzBlobMotion.clear(slot);
        var sandCrabMotion = runtime.motions.get(SandCrabMotion.class, SandCrabMotion::new);
        sandCrabMotion.clear(slot);
        var dogMotion = runtime.motions.get(DogMotion.class, DogMotion::new);
        dogMotion.clear(slot);
        var urchinMotion = runtime.motions.get(UrchinMotion.class, UrchinMotion::new);
        urchinMotion.clear(slot);
        var witchRatMotion = runtime.motions.get(WitchRatMotion.class, WitchRatMotion::new);
        witchRatMotion.clear(slot);
        var mimicMotion = runtime.motions.get(MimicMotion.class, MimicMotion::new);
        mimicMotion.clear(slot);
        var maskedMimicMotion = runtime.motions.get(MaskedMimicMotion.class, MaskedMimicMotion::new);
        maskedMimicMotion.clear(slot);
        var miniMoldormMotion = runtime.motions.get(MiniMoldormMotion.class, MiniMoldormMotion::new);
        miniMoldormMotion.clear(slot);
        var moldormMotion = runtime.motions.get(MoldormMotion.class, MoldormMotion::new);
        moldormMotion.clear(slot);
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
        runtime.bombInteractions.writeBombOrigin(slot, 0);
        runtime.projectiles.writeMagicPowderState(slot, 0);
        runtime.projectiles.writePowderSpent(slot, 0);
        runtime.bombInteractions.writeBombCollisionGraceCountdown(slot, 0);
        runtime.bombInteractions.writeBombLiftDelay(slot, 0);
        runtime.bombInteractions.writePlacedBombMotionInitialized(slot, false);
        runtime.liftThrow.writeLedgeTransitionTimer(slot, 0);
        runtime.liftThrow.writeThrownMotionInitialized(slot, false);
    }

    private static void entityPersistentSlotResetClearWeaponState(
            EntitySlotLifecycle runtime, int slot, RoomEntity entity) {
        var hookshotChainMotion = runtime.motions.get(HookshotChainMotion.class, HookshotChainMotion::new);
        hookshotChainMotion.clear(slot);
        var hookshotBridgeMotion = runtime.motions.get(HookshotBridgeMotion.class, HookshotBridgeMotion::new);
        hookshotBridgeMotion.clear(slot);
        var boomerangMotion = runtime.motions.get(BoomerangMotion.class, BoomerangMotion::new);
        boomerangMotion.clear(slot);
        var magicRodFireballMotion = runtime.motions.get(MagicRodFireballMotion.class, MagicRodFireballMotion::new);
        magicRodFireballMotion.clear(slot);
        var swordBeamMotion = runtime.motions.get(SwordBeamMotion.class, SwordBeamMotion::new);
        swordBeamMotion.clear(slot);
        if (entity.type() == EntityRomConstants.ENTITY_HOOKSHOT_CHAIN) {
            runtime.slotPresentation.writeHookshotChainOam(List.of());
        }
    }

    private static void entityPersistentSlotResetClearRoamingMotions(EntitySlotLifecycle runtime, int slot) {
        runtime.physicsTables.writeBaseEntityFlipAttribute(slot, 0);
        runtime.physicsTables.writeEntityOptions1Override(slot, -1);
        var enemyRecoilMotion = runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(slot);
        var colorShellMotion = runtime.motions.get(ColorShellMotion.class, ColorShellMotion::new);
        colorShellMotion.clear(slot);
        var butterflyMotion = runtime.motions.get(ButterflyMotion.class, ButterflyMotion::new);
        butterflyMotion.clear(slot);
        var keeseMotion = runtime.motions.get(KeeseMotion.class, KeeseMotion::new);
        keeseMotion.clear(slot);
        var roamingEnemyMotion = runtime.motions.get(RoamingEnemyMotion.class, RoamingEnemyMotion::new);
        roamingEnemyMotion.clear(slot);
        var unmaskedIronMaskMotion = runtime.motions.get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new);
        unmaskedIronMaskMotion.clear(slot);
        var moblinSwordMotion = runtime.motions.get(MoblinSwordMotion.class, MoblinSwordMotion::new);
        moblinSwordMotion.clear(slot);
        var playerArrowMotion = runtime.motions.get(PlayerArrowMotion.class, PlayerArrowMotion::new);
        playerArrowMotion.clear(slot);
        var tektiteMotion = runtime.motions.get(TektiteMotion.class, TektiteMotion::new);
        tektiteMotion.clear(slot);
        var leeverMotion = runtime.motions.get(LeeverMotion.class, LeeverMotion::new);
        leeverMotion.clear(slot);
        var antiFairyMotion = runtime.motions.get(AntiFairyMotion.class, AntiFairyMotion::new);
        antiFairyMotion.clear(slot);
        var sparkMotion = runtime.motions.get(SparkMotion.class, SparkMotion::new);
        sparkMotion.clear(slot);
        var zolGelMotion = runtime.motions.get(ZolGelMotion.class, ZolGelMotion::new);
        zolGelMotion.clear(slot);
        var pokeyMotion = runtime.motions.get(PokeyMotion.class, PokeyMotion::new);
        pokeyMotion.clear(slot);
        var hidingZolMotion = runtime.motions.get(HidingZolMotion.class, HidingZolMotion::new);
        hidingZolMotion.clear(slot);
        var starMotion = runtime.motions.get(StarMotion.class, StarMotion::new);
        starMotion.clear(slot);
        var blooperMotion = runtime.motions.get(BlooperMotion.class, BlooperMotion::new);
        blooperMotion.clear(slot);
        var pincerMotion = runtime.motions.get(PincerMotion.class, PincerMotion::new);
        pincerMotion.clear(slot);
        var spikeTrapMotion = runtime.motions.get(SpikeTrapMotion.class, SpikeTrapMotion::new);
        spikeTrapMotion.clear(slot);
        var pairoddMotion = runtime.motions.get(PairoddMotion.class, PairoddMotion::new);
        pairoddMotion.clear(slot);
        var pairoddProjectileMotion = runtime.motions.get(PairoddProjectileMotion.class, PairoddProjectileMotion::new);
        pairoddProjectileMotion.clear(slot);
        var enemyProjectileMotion = runtime.motions.get(EnemyProjectileMotion.class, EnemyProjectileMotion::new);
        enemyProjectileMotion.clear(slot);
        var wingedOctorokMotion = runtime.motions.get(WingedOctorokMotion.class, WingedOctorokMotion::new);
        wingedOctorokMotion.clear(slot);
        var bushCrawlerMotion = runtime.motions.get(BushCrawlerMotion.class, BushCrawlerMotion::new);
        bushCrawlerMotion.clear(slot);
        var cuccoMotion = runtime.motions.get(CuccoMotion.class, CuccoMotion::new);
        cuccoMotion.clear(slot);
        var giantGopongaMotion = runtime.motions.get(GiantGopongaMotion.class, GiantGopongaMotion::new);
        giantGopongaMotion.clear(slot);
        var gopongaProjectileMotion = runtime.motions.get(GopongaProjectileMotion.class, GopongaProjectileMotion::new);
        gopongaProjectileMotion.clear(slot);
        var zoraMotion = runtime.motions.get(ZoraMotion.class, ZoraMotion::new);
        zoraMotion.clear(slot);
        var zombieMotion = runtime.motions.get(ZombieMotion.class, ZombieMotion::new);
        zombieMotion.clear(slot);
        var buzzBlobMotion = runtime.motions.get(BuzzBlobMotion.class, BuzzBlobMotion::new);
        buzzBlobMotion.clear(slot);
        var sandCrabMotion = runtime.motions.get(SandCrabMotion.class, SandCrabMotion::new);
        sandCrabMotion.clear(slot);
        var dogMotion = runtime.motions.get(DogMotion.class, DogMotion::new);
        dogMotion.clear(slot);
        var urchinMotion = runtime.motions.get(UrchinMotion.class, UrchinMotion::new);
        urchinMotion.clear(slot);
        var witchRatMotion = runtime.motions.get(WitchRatMotion.class, WitchRatMotion::new);
        witchRatMotion.clear(slot);
    }

    private static void entityPersistentSlotResetClearRemainingMotions(EntitySlotLifecycle runtime, int slot) {
        runtime.slotPresentation.writeWingedSwordAttackThisFrame(slot, false);
        runtime.slotPresentation.writeWingedStateTwoThisFrame(slot, false);
        var laserMotion = runtime.motions.get(LaserMotion.class, LaserMotion::new);
        laserMotion.clear(slot);
        var waterTektiteMotion = runtime.motions.get(WaterTektiteMotion.class, WaterTektiteMotion::new);
        waterTektiteMotion.clear(slot);
        var fishMotion = runtime.motions.get(FishMotion.class, FishMotion::new);
        fishMotion.clear(slot);
        var crowMotion = runtime.motions.get(CrowMotion.class, CrowMotion::new);
        crowMotion.clear(slot);
        var booBuddyMotion = runtime.motions.get(BooBuddyMotion.class, BooBuddyMotion::new);
        booBuddyMotion.clear(slot);
        var stalfosAggressiveMotion = runtime.motions.get(StalfosAggressiveMotion.class, StalfosAggressiveMotion::new);
        stalfosAggressiveMotion.clear(slot);
        var stalfosEvasiveMotion = runtime.motions.get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new);
        stalfosEvasiveMotion.clear(slot);
        var threeOfAKindMotion = runtime.motions.get(ThreeOfAKindMotion.class, ThreeOfAKindMotion::new);
        threeOfAKindMotion.clear(slot);
        var gibdoMotion = runtime.motions.get(GibdoMotion.class, GibdoMotion::new);
        gibdoMotion.clear(slot);
        var likeLikeMotion = runtime.motions.get(LikeLikeMotion.class, LikeLikeMotion::new);
        likeLikeMotion.clear(slot);
        var goombaMotion = runtime.motions.get(GoombaMotion.class, GoombaMotion::new);
        goombaMotion.clear(slot);
        var snakeMotion = runtime.motions.get(SnakeMotion.class, SnakeMotion::new);
        snakeMotion.clear(slot);
        var wizrobeMotion = runtime.motions.get(WizrobeMotion.class, WizrobeMotion::new);
        wizrobeMotion.clear(slot);
        var wizrobeProjectileMotion = runtime.motions.get(WizrobeProjectileMotion.class, WizrobeProjectileMotion::new);
        wizrobeProjectileMotion.clear(slot);
        var peaHatMotion = runtime.motions.get(PeaHatMotion.class, PeaHatMotion::new);
        peaHatMotion.clear(slot);
        var armosMotion = runtime.motions.get(ArmosMotion.class, ArmosMotion::new);
        armosMotion.clear(slot);
        var ghiniMotion = runtime.motions.get(GhiniMotion.class, GhiniMotion::new);
        ghiniMotion.clear(slot);
        var hardHatMotion = runtime.motions.get(HardHatMotion.class, HardHatMotion::new);
        hardHatMotion.clear(slot);
        var madBomberMotion = runtime.motions.get(MadBomberMotion.class, MadBomberMotion::new);
        madBomberMotion.clear(slot);
        var followingNpcMotion = runtime.motions.get(FollowingNpcMotion.class, FollowingNpcMotion::new);
        followingNpcMotion.clear(slot);
        var bowWowMotion = runtime.motions.get(BowWowMotion.class, BowWowMotion::new);
        bowWowMotion.clear(slot);
        var moblinKingMotion = runtime.motions.get(MoblinKingMotion.class, MoblinKingMotion::new);
        moblinKingMotion.clear(slot);
        var thrownEntityMotion = runtime.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new);
        thrownEntityMotion.clear(slot);
    }

    private static void entityTransientSlotResetClearBossState(EntitySlotLifecycle runtime, int slot) {
        runtime.slimeEye.clear(slot);
        runtime.bossRewards.resetEnemyDropState(slot, runtime.motions);
        var sideViewPlatformMotion = runtime.motions.get(SideViewPlatformMotion.class, SideViewPlatformMotion::new);
        sideViewPlatformMotion.clear(slot);
        var sideViewPotMotion = runtime.motions.get(SideViewPotMotion.class, SideViewPotMotion::new);
        sideViewPotMotion.clear(slot);
        var genieMotion = runtime.motions.get(GenieMotion.class, GenieMotion::new);
        genieMotion.clear(slot);
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
        var secretSeashellMotion = runtime.motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new);
        secretSeashellMotion.clear(slot);
        runtime.physicsTables.writeSlowTransitionCountdown(slot, 0);
        runtime.physicsTables.writeSlowTimerInitialized(slot, false);
        runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0);
        runtime.bossRewards.writeBossDeathProducerState(slot, 0);
        var moldormMotion = runtime.motions.get(MoldormMotion.class, MoldormMotion::new);
        moldormMotion.setDestructionTailState(slot, 0);
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
        var armosKnightMotion = runtime.motions.get(ArmosKnightMotion.class, ArmosKnightMotion::new);
        armosKnightMotion.clear(slot);
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
        runtime.bombInteractions.writeBombOrigin(slot, 0);
        runtime.projectiles.writeMagicPowderState(slot, 0);
        runtime.projectiles.writePowderSpent(slot, 0);
        runtime.bombInteractions.writeBombCollisionGraceCountdown(slot, 0);
        runtime.bombInteractions.writeBombLiftDelay(slot, 0);
        var unmaskedIronMaskMotion = runtime.motions.get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new);
        unmaskedIronMaskMotion.setMaskRemovalState(slot, 0);
        unmaskedIronMaskMotion.setMaskSourceHookshotSlot(slot, 0);
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
        var enemyRecoilMotion = runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(slot);
        var bomberMotion = runtime.motions.get(BomberMotion.class, BomberMotion::new);
        bomberMotion.clear(slot);
    }

    private static void entityTransientSlotResetClearRoamingMotions(EntitySlotLifecycle runtime, int slot) {
        var madBomberMotion = runtime.motions.get(MadBomberMotion.class, MadBomberMotion::new);
        madBomberMotion.clear(slot);
        var bombiteMotion = runtime.motions.get(BombiteMotion.class, BombiteMotion::new);
        bombiteMotion.clear(slot);
        var colorShellMotion = runtime.motions.get(ColorShellMotion.class, ColorShellMotion::new);
        colorShellMotion.clear(slot);
        var butterflyMotion = runtime.motions.get(ButterflyMotion.class, ButterflyMotion::new);
        butterflyMotion.clear(slot);
        var keeseMotion = runtime.motions.get(KeeseMotion.class, KeeseMotion::new);
        keeseMotion.clear(slot);
        var roamingEnemyMotion = runtime.motions.get(RoamingEnemyMotion.class, RoamingEnemyMotion::new);
        roamingEnemyMotion.clear(slot);
        var unmaskedIronMaskMotion = runtime.motions.get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new);
        unmaskedIronMaskMotion.clear(slot);
        var moblinSwordMotion = runtime.motions.get(MoblinSwordMotion.class, MoblinSwordMotion::new);
        moblinSwordMotion.clear(slot);
        var tektiteMotion = runtime.motions.get(TektiteMotion.class, TektiteMotion::new);
        tektiteMotion.clear(slot);
        var leeverMotion = runtime.motions.get(LeeverMotion.class, LeeverMotion::new);
        leeverMotion.clear(slot);
        var antiFairyMotion = runtime.motions.get(AntiFairyMotion.class, AntiFairyMotion::new);
        antiFairyMotion.clear(slot);
        var sparkMotion = runtime.motions.get(SparkMotion.class, SparkMotion::new);
        sparkMotion.clear(slot);
        var zolGelMotion = runtime.motions.get(ZolGelMotion.class, ZolGelMotion::new);
        zolGelMotion.clear(slot);
        var hidingZolMotion = runtime.motions.get(HidingZolMotion.class, HidingZolMotion::new);
        hidingZolMotion.clear(slot);
        var starMotion = runtime.motions.get(StarMotion.class, StarMotion::new);
        starMotion.clear(slot);
        var blooperMotion = runtime.motions.get(BlooperMotion.class, BlooperMotion::new);
        blooperMotion.clear(slot);
        var pincerMotion = runtime.motions.get(PincerMotion.class, PincerMotion::new);
        pincerMotion.clear(slot);
        var bushCrawlerMotion = runtime.motions.get(BushCrawlerMotion.class, BushCrawlerMotion::new);
        bushCrawlerMotion.clear(slot);
        var pokeyMotion = runtime.motions.get(PokeyMotion.class, PokeyMotion::new);
        pokeyMotion.clear(slot);
        var piranhaMotion = runtime.motions.get(PiranhaMotion.class, PiranhaMotion::new);
        piranhaMotion.clear(slot);
        var zoraMotion = runtime.motions.get(ZoraMotion.class, ZoraMotion::new);
        zoraMotion.clear(slot);
        var zombieMotion = runtime.motions.get(ZombieMotion.class, ZombieMotion::new);
        zombieMotion.clear(slot);
        var buzzBlobMotion = runtime.motions.get(BuzzBlobMotion.class, BuzzBlobMotion::new);
        buzzBlobMotion.clear(slot);
        var sandCrabMotion = runtime.motions.get(SandCrabMotion.class, SandCrabMotion::new);
        sandCrabMotion.clear(slot);
        var dogMotion = runtime.motions.get(DogMotion.class, DogMotion::new);
        dogMotion.clear(slot);
        var mimicMotion = runtime.motions.get(MimicMotion.class, MimicMotion::new);
        mimicMotion.clear(slot);
        var maskedMimicMotion = runtime.motions.get(MaskedMimicMotion.class, MaskedMimicMotion::new);
        maskedMimicMotion.clear(slot);
        var miniMoldormMotion = runtime.motions.get(MiniMoldormMotion.class, MiniMoldormMotion::new);
        miniMoldormMotion.clear(slot);
        var moldormMotion = runtime.motions.get(MoldormMotion.class, MoldormMotion::new);
        moldormMotion.clear(slot);
        var cuccoMotion = runtime.motions.get(CuccoMotion.class, CuccoMotion::new);
        cuccoMotion.clear(slot);
        var spikeTrapMotion = runtime.motions.get(SpikeTrapMotion.class, SpikeTrapMotion::new);
        spikeTrapMotion.clear(slot);
        var pairoddMotion = runtime.motions.get(PairoddMotion.class, PairoddMotion::new);
        pairoddMotion.clear(slot);
        var pairoddProjectileMotion = runtime.motions.get(PairoddProjectileMotion.class, PairoddProjectileMotion::new);
        pairoddProjectileMotion.clear(slot);
        var playerArrowMotion = runtime.motions.get(PlayerArrowMotion.class, PlayerArrowMotion::new);
        playerArrowMotion.clear(slot);
        var magicRodFireballMotion = runtime.motions.get(MagicRodFireballMotion.class, MagicRodFireballMotion::new);
        magicRodFireballMotion.clear(slot);
        runtime.bombInteractions.writePlayerArrowBombArrow(slot, false);
    }

    private static void entityTransientSlotResetClearRemainingMotions(EntitySlotLifecycle runtime, int slot) {
        if (runtime.bombInteractions.readLatestDroppedBombEntityIndex() == slot) {
            runtime.bombInteractions.writeLatestDroppedBombEntityIndex(-1);
        }
        if (runtime.projectiles.readLatestShotArrowEntityIndex() == slot) {
            runtime.projectiles.writeLatestShotArrowEntityIndex(-1);
        }
        var enemyProjectileMotion = runtime.motions.get(EnemyProjectileMotion.class, EnemyProjectileMotion::new);
        enemyProjectileMotion.clear(slot);
        var laserMotion = runtime.motions.get(LaserMotion.class, LaserMotion::new);
        laserMotion.clear(slot);
        var waterTektiteMotion = runtime.motions.get(WaterTektiteMotion.class, WaterTektiteMotion::new);
        waterTektiteMotion.clear(slot);
        var fishMotion = runtime.motions.get(FishMotion.class, FishMotion::new);
        fishMotion.clear(slot);
        var crowMotion = runtime.motions.get(CrowMotion.class, CrowMotion::new);
        crowMotion.clear(slot);
        var booBuddyMotion = runtime.motions.get(BooBuddyMotion.class, BooBuddyMotion::new);
        booBuddyMotion.clear(slot);
        var stalfosAggressiveMotion = runtime.motions.get(StalfosAggressiveMotion.class, StalfosAggressiveMotion::new);
        stalfosAggressiveMotion.clear(slot);
        var stalfosEvasiveMotion = runtime.motions.get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new);
        stalfosEvasiveMotion.clear(slot);
        var gibdoMotion = runtime.motions.get(GibdoMotion.class, GibdoMotion::new);
        gibdoMotion.clear(slot);
        var goombaMotion = runtime.motions.get(GoombaMotion.class, GoombaMotion::new);
        goombaMotion.clear(slot);
        var snakeMotion = runtime.motions.get(SnakeMotion.class, SnakeMotion::new);
        snakeMotion.clear(slot);
        var wizrobeMotion = runtime.motions.get(WizrobeMotion.class, WizrobeMotion::new);
        wizrobeMotion.clear(slot);
        var wizrobeProjectileMotion = runtime.motions.get(WizrobeProjectileMotion.class, WizrobeProjectileMotion::new);
        wizrobeProjectileMotion.clear(slot);
        var peaHatMotion = runtime.motions.get(PeaHatMotion.class, PeaHatMotion::new);
        peaHatMotion.clear(slot);
        var armosMotion = runtime.motions.get(ArmosMotion.class, ArmosMotion::new);
        armosMotion.clear(slot);
        var followingNpcMotion = runtime.motions.get(FollowingNpcMotion.class, FollowingNpcMotion::new);
        followingNpcMotion.clear(slot);
        var ghiniMotion = runtime.motions.get(GhiniMotion.class, GhiniMotion::new);
        ghiniMotion.clear(slot);
        var hardHatMotion = runtime.motions.get(HardHatMotion.class, HardHatMotion::new);
        hardHatMotion.clear(slot);
        var polsVoiceMotion = runtime.motions.get(PolsVoiceMotion.class, PolsVoiceMotion::new);
        polsVoiceMotion.clear(slot);
        var spikedBeetleMotion = runtime.motions.get(SpikedBeetleMotion.class, SpikedBeetleMotion::new);
        spikedBeetleMotion.clear(slot);
        var bowWowMotion = runtime.motions.get(BowWowMotion.class, BowWowMotion::new);
        bowWowMotion.clear(slot);
        var moblinKingMotion = runtime.motions.get(MoblinKingMotion.class, MoblinKingMotion::new);
        moblinKingMotion.clear(slot);
        var thrownEntityMotion = runtime.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new);
        thrownEntityMotion.clear(slot);
    }
    /** wEntitiesPrivateState3Table marks a droppable's initially hidden presentation. */
    public boolean dropStartsHidden(int slot) {
        return bossRewards.readDroppablePrivateState3(slot) != 0;
    }

    /** wEntitiesPrivateCountdown3Table preserves the squashed Goomba's drop/death window. */
    public void setDropDeathWindow(int slot, int countdown) {
        bossRewards.writeDropPrivateCountdown3(slot, countdown);
    }
}
