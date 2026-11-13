package linksawakening.world.entity;

import static linksawakening.world.entity.EntitySnapshots.withDeathPresentation;
import static linksawakening.world.entity.EntitySnapshots.withStatus;
import static linksawakening.world.entity.EntitySnapshots.withType;
import static linksawakening.world.entity.EntityTypeRules.initialHitboxFlags;
import static linksawakening.world.entity.EntityTypeRules.initialPhysicsFlags;

import java.util.List;
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
    final RomEnemyCombatTables enemyCombatTables;
    final RoomEntity[] slots;
    final SlimeEyeController.State slimeEye;
    // Optional ROM data, absent in ROM-free runtimes.
    final java.util.function.IntUnaryOperator entityProperties;

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
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ java.util.function
                            .IntUnaryOperator
                    entityProperties,
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
        this.enemyCombatTables = java.util.Objects.requireNonNull(enemyCombatTables, "combat rules");
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
        this.slimeEye = java.util.Objects.requireNonNull(slimeEye, "slimeEye");
        this.entityProperties = java.util.Objects.requireNonNull(entityProperties, "entity options");
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
        if (this.slots[slot].status() == EntityStatus.ACTIVE) {
            initializeBossBattle(this.slots[slot], entityProperties.applyAsInt(this.slots[slot].type()));
        }
        this.combatTables.writeEnemyHealthGroup(slot, this.enemyCombatTables.healthGroup(this.slots[slot].type()));
    }

    public int transitionSequenceCounter() {
        return this.roomVariables.readTransitionSequenceCounter();
    }

    public int clearEntity(int slot) {
        if (slot < 0 || slot >= this.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        entityPersistentSlotResetClearBossState(this, slot);
        RoomEntity entity = this.slots[slot];
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
        this.slots[slot] = RoomEntity.disabled(slot);
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
        this.physicsTables.writeEnemyPhysicsFlags(slot, initialPhysicsFlags(EntityRomConstants.ENTITY_GEL));
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
        this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(slot);

        int freeSlot = EntitySlotAllocator.findFreeSlot(this.slots, this.combatTables, this.slots.length - 1);
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
            this.slots[freeSlot] = spawnedGel;
            this.initializeHealthGroup(freeSlot);
            this.physicsTables.writeEnemyPhysicsFlags(freeSlot, initialPhysicsFlags(EntityRomConstants.ENTITY_GEL));
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
            this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
            this.motions.get(ZolGelMotion.class, ZolGelMotion::new).prepareSpawnedGel(freeSlot);
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
        this.slots[slot] = RoomEntity.disabled(slot);
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
        this.roomVariables.writePendingClearedEntityMask(this.roomVariables.readPendingClearedEntityMask() | mask);
    }

    public void delayInteractions(int countdown) {
        this.roomVariables.writeEntityInteractionCountdown(countdown);
    }

    // Preserve the eager ROM clinging-state accumulator merge across slots.
    @SuppressWarnings("ShortCircuitBoolean")
    public void recordGelClinging(boolean clinging) {
        this.motions
                .get(ZolGelMotion.class, ZolGelMotion::new)
                .writeGelClingingToLink(
                        this.motions.get(ZolGelMotion.class, ZolGelMotion::new).readGelClingingToLink() | clinging);
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
}
