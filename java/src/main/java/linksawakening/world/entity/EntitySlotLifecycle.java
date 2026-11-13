package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
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
        return EntityPersistentSlotReset.clearEntity(this, slot);
    }

    public RoomEntity applyZolSplit(RoomEntity original, RoomEntity updated, ZolGelMotion.Split split) {
        return ZolSplitController.applyZolSplit(this, original, updated, split);
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
        EntityTransientSlotReset.disableEntityWithoutPersistence(this, slot);
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
}
