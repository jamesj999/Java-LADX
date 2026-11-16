package linksawakening.world.entity;

import static linksawakening.world.entity.EntitySnapshots.withFlipAttribute;
import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;
import static linksawakening.world.entity.EntitySnapshots.withStatus;
import static linksawakening.world.entity.EntitySnapshots.withZ;
import static linksawakening.world.entity.EntityTypeRules.initialPhysicsFlags;
import static linksawakening.world.entity.EntityTypeRules.persistentClearMask;

import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.MoldormSegmentPosition;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.BowWowMotion;
import linksawakening.world.entity.motion.EnemyDropMotion;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.GhiniMotion;
import linksawakening.world.entity.motion.LikeLikeMotion;
import linksawakening.world.entity.motion.MoblinKingMotion;
import linksawakening.world.entity.motion.MoblinSwordMotion;
import linksawakening.world.entity.motion.MoldormMotion;
import linksawakening.world.entity.motion.RollingBonesMotion;

/** Boss destruction and source-shaped reward production. */
public final class EntityBossRewards {

    EnemyDropResolver.Result resolveDrop(EnemyDropResolver.Context context) {
        return state.readEnemyDropLowHealth() == null
                ? state.readEnemyDropResolver().resolve(context)
                : state.readEnemyDropResolver().resolve(context, state.readEnemyDropLowHealth());
    }

    final EntityBossState state;
    final linksawakening.world.entity.EntitySpriteViews spriteHandlers;
    final EntityBackgroundMechanics backgroundMechanics;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntityPickupController pickups;
    final EntityRoomInspection roomInspection;
    final EntityRoomVariables roomVariables;
    final EntitySlotLifecycle lifecycle;
    final EntitySlotProperties slotProperties;
    final EntitySlots slots;

    final EntityLinkState linkState;

    EntityBossRewards(
            EntityBossState state,
            linksawakening.world.entity.EntitySpriteViews spriteHandlers,
            EntityBackgroundMechanics backgroundMechanics,
            EntityEventChannels events,
            EntityMotionRegistry motions,
            EntityPickupController pickups,
            EntityRoomInspection roomInspection,
            EntityRoomVariables roomVariables,
            EntitySlotLifecycle lifecycle,
            EntitySlotProperties slotProperties,
            EntitySlots slots,
            EntityLinkState linkState) {
        EntityCombatTables combatTables = slots.combat();
        EntityPhysicsTables physicsTables = slots.physics();
        this.linkState = java.util.Objects.requireNonNull(linkState, "linkState");

        this.state = java.util.Objects.requireNonNull(state, "state");
        this.spriteHandlers = java.util.Objects.requireNonNull(spriteHandlers, "sprite source");
        this.backgroundMechanics = java.util.Objects.requireNonNull(backgroundMechanics, "backgroundMechanics");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.pickups = java.util.Objects.requireNonNull(pickups, "pickups");
        this.roomInspection = java.util.Objects.requireNonNull(roomInspection, "roomInspection");
        this.roomVariables = java.util.Objects.requireNonNull(roomVariables, "roomVariables");
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
        this.slotProperties = java.util.Objects.requireNonNull(slotProperties, "slotProperties");
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
    }

    /** ApplySwordDamagesToEnemy, $03:7361: preserve descending RNG order. */
    void cascadeGhiniDeath(RoomEntity killed, IntSupplier random) {
        if (killed.type() != 0x12) return;
        int[] drops = {0x2D, 0x2E, 0x38, 0x37};
        for (int slot = RoomEntity.SLOT_COUNT - 1; slot >= 0; slot--) {
            RoomEntity other = this.slots.get(slot);
            var ghiniMotion = this.motions.get(GhiniMotion.class, GhiniMotion::new);
            if (slot == killed.slot()
                    || (other.type() != 0x10 && other.type() != 0x11)
                    || ghiniMotion.state(slot) != 0
                    || other.status() == EntityStatus.DISABLED) continue;
            this.slots.replace(slot, withStatus(other, EntityStatus.DYING));
            this.combatTables.writeDyingCountdown(slot, 0x1F);
            state.writeDroppedItemBySlot(slot, drops[random.getAsInt() & 3]);
        }
        state.writeDroppedItemBySlot(killed.slot(), 0x2E);
    }

    void handleTerminalEnemyDeath(RoomEntity entity, IntSupplier randomByteSupplier) {
        var likeLikeMotion = this.motions.get(LikeLikeMotion.class, LikeLikeMotion::new);
        boolean swallowedShield = entity.type() == EntityRomConstants.ENTITY_LIKE_LIKE
                && likeLikeMotion.privateState1(entity.slot()) != 0;
        if (entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING) {
            // The mini-boss option reaches DidKillEnemy's room-event branch,
            // permanently opening the east rescue-room path.
            this.roomVariables.writePendingRoomStatusMask(this.roomVariables.readPendingRoomStatusMask() | 0x20);
        }
        if (entity.sourceLoadOrder() < 0
                || (!this.state.readEnemyDropResolver().recordsDefeats() && !swallowedShield)) {
            if (entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING) {
                this.roomVariables.writePendingClearedEntityMask(
                        this.roomVariables.readPendingClearedEntityMask() | persistentClearMask(entity));
            }
            this.lifecycle.disableEntityWithoutPersistence(entity.slot());
            return;
        }

        EnemyDropResolver.Result result;
        if (swallowedShield) {
            result = new EnemyDropResolver.Result(
                    EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP, this.state.readEnemyDropCounters(), 0);
        } else {
            EnemyDropResolver.Context context = new EnemyDropResolver.Context(
                    entity.type(),
                    this.roomInspection.enemyDropHealthGroup(entity.slot()),
                    this.state.readDroppedItemBySlot(entity.slot()),
                    this.state.readEnemyDropMaxHearts(),
                    this.state.readEnemyDropHealth(),
                    this.state.readInBossBattle(),
                    this.state.readEnemyDropActivePowerUp(),
                    this.physicsTables.readGroundInteractionSideScrolling(),
                    this.state.readEnemyDropCounters(),
                    randomByteSupplier);
            result = this.resolveDrop(context);
            this.state.writeEnemyDropCounters(result.counters());
        }

        int killIndex = this.state.readKillCount() & 0xFF;
        this.state.writeKillOrder(killIndex, entity.sourceLoadOrder() & 0xFF);
        this.state.writeKillCount((this.state.readKillCount() + 1) & 0xFF);
        this.roomVariables.writePendingClearedEntityMask(
                this.roomVariables.readPendingClearedEntityMask() | persistentClearMask(entity));
        if (result.dropped()) {
            this.pickups.spawnEnemyDrop(entity, result.itemType());
        }
        this.lifecycle.disableEntityWithoutPersistence(entity.slot());
    }

    public void advanceRollingBonesDestruction(RoomEntity entity, IntSupplier randomByteSupplier) {
        int slot = entity.slot();
        var rollingBonesMotion = this.motions.get(RollingBonesMotion.class, RollingBonesMotion::new);
        if (!rollingBonesMotion.deathInitialized(slot)) {
            rollingBonesMotion.setDeathInitialized(slot, true);
            this.physicsTables.writeEnemyTransitionCountdown(slot, 0xFF);
            this.combatTables.writeEnemyFlashCountdown(slot, 0xFF);
            return;
        }
        int countdown = this.physicsTables.readEnemyTransitionCountdown(slot);
        this.combatTables.writeEnemyFlashCountdown(slot, countdown);
        if (countdown == 0) {
            // $06:6CC1 calls DidKillEnemy, including SpawnEnemyDrop. The
            // ROM health-group tables select the miniboss healing fairy.
            if (this.state.readEnemyDropResolver().recordsDefeats()) {
                EnemyDropResolver.Result drop = this.resolveDrop(new EnemyDropResolver.Context(
                        entity.type(),
                        this.roomInspection.enemyDropHealthGroup(entity.slot()),
                        this.state.readDroppedItemBySlot(slot),
                        this.state.readEnemyDropMaxHearts(),
                        this.state.readEnemyDropHealth(),
                        this.state.readInBossBattle(),
                        this.state.readEnemyDropActivePowerUp(),
                        this.physicsTables.readGroundInteractionSideScrolling(),
                        this.state.readEnemyDropCounters(),
                        randomByteSupplier));
                this.state.writeEnemyDropCounters(drop.counters());
                if (drop.dropped()) this.pickups.spawnEnemyDrop(entity, drop.itemType());
            }
            int killIndex = this.state.readKillCount() & 0xFF;
            this.state.writeKillOrder(killIndex, entity.sourceLoadOrder() & 0xFF);
            this.state.writeKillCount((this.state.readKillCount() + 1) & 0xFF);
            this.roomVariables.writePendingClearedEntityMask(
                    this.roomVariables.readPendingClearedEntityMask() | persistentClearMask(entity));
            if (this.roomVariables.readEntityMapId() != 0x07) {
                this.roomVariables.writePendingRoomStatusMask(this.roomVariables.readPendingRoomStatusMask() | 0x20);
            }
            this.lifecycle.disableEntityWithoutPersistence(slot);
            return;
        }
        if (countdown < 0x80 && (countdown & 0x07) == 0) {
            int x = (entity.x() + ((randomByteSupplier.getAsInt() & 0x1F) - 0x10)) & 0xFF;
            int y = (entity.y() - entity.z() + ((randomByteSupplier.getAsInt() & 0x1F) - 0x14)) & 0xFF;
            this.events.transientVfxRequests.add(new TransientVfxRequest(TransientVfxType.POOF, x, y));
            this.events.pendingEntityEvents.add(
                    new EntityCombatEvent(slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
        }
    }

    public void advanceMoblinKingDestruction(RoomEntity source, int frame, IntSupplier random) {
        int slot = source.slot();
        // Main resolves sword damage before dispatch; the ROM dispatches the
        // dying branch on the following frame, after this hit's active handler.
        var moblinKingMotion = this.motions.get(MoblinKingMotion.class, MoblinKingMotion::new);
        boolean hitThisFrame = moblinKingMotion.lethalHitFrame(slot) == frame + 1;
        moblinKingMotion.setLethalHitFrame(slot, 0);
        if (hitThisFrame) return;
        {
            source = this.spriteHandlers.present(
                    source, this.slotProperties.spriteViews().forMoblinKingState(12, -1, source.z() != 0), 0);
        }
        this.slots.replace(
                slot,
                withFlipAttribute(
                        source,
                        this.physicsTables.readBaseEntityFlipAttribute(slot)
                                ^ ((this.combatTables.readEnemyFlashCountdown(slot) << 2) & 0x10)));
        if (this.state.readBossDeathProducerState(slot) == 0) {
            this.physicsTables.writeEnemyTransitionCountdown(slot, 0xA0);
            this.combatTables.writeEnemyFlashCountdown(slot, 0xFF);
            this.state.writeBossDeathProducerState(slot, 1);
        } else if (this.state.readBossDeathProducerState(slot) == 1) {
            if (this.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
                this.physicsTables.writeEnemyTransitionCountdown(slot, 0xC0);
                this.combatTables.writeEnemyFlashCountdown(slot, 0xFF);
                this.state.writeBossDeathProducerState(slot, 2);
            }
        } else if (this.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
            this.events.pendingEntityEvents.add(
                    new EntityCombatEvent(slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x0C));
            var moblinSwordMotion = this.motions.get(MoblinSwordMotion.class, MoblinSwordMotion::new);
            moblinSwordMotion.writeSwordMoblinAlertingSoundCounter(4);
            this.handleTerminalEnemyDeath(source, random);
        } else if ((this.physicsTables.readEnemyTransitionCountdown(slot) & 7) == 0
                && this.backgroundMechanics.entityHandlerInteractive(false)) {
            int x = (source.x() + (random.getAsInt() & 0x1F) - 0x10) & 255;
            int y = (source.y() - source.z() + (random.getAsInt() & 0x1F) - 0x14) & 255;
            this.events.transientVfxRequests.add(new TransientVfxRequest(TransientVfxType.POOF, x, y));
            this.events.pendingEntityEvents.add(
                    new EntityCombatEvent(slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
        }
    }

    public void advanceMoldormDestruction(RoomEntity source, int frameCounter) {
        int slot = source.slot();
        int tailState =
                this.motions.get(MoldormMotion.class, MoldormMotion::new).destructionTailState(slot);
        {
            var moldormMotion = this.motions.get(MoldormMotion.class, MoldormMotion::new);
            RoomEntity rendered = this.spriteHandlers.present(
                    source,
                    this.spriteHandlers.forMoldormState(
                            moldormMotion.headSpriteVariant(slot),
                            source.x(),
                            (source.y() - source.z()) & 0xFF,
                            moldormMotion.currentTailSegments(slot),
                            tailState,
                            frameCounter),
                    0);
            this.slots.replace(
                    slot,
                    withFlipAttribute(
                            rendered,
                            this.physicsTables.readBaseEntityFlipAttribute(slot)
                                    ^ ((this.combatTables.readEnemyFlashCountdown(slot) << 2) & 0x10)));
        }

        switch (this.state.readBossDeathProducerState(slot)) {
            case 0 -> {
                this.physicsTables.writeEnemyTransitionCountdown(slot, 0x60);
                this.combatTables.writeEnemyFlashCountdown(slot, 0xFF);
                this.state.writeBossDeathProducerState(slot, 1);
            }
            case 1 -> {
                if (this.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
                    this.physicsTables.writeEnemyTransitionCountdown(slot, 0xFF);
                    this.combatTables.writeEnemyFlashCountdown(slot, 0xFF);
                    this.state.writeBossDeathProducerState(slot, 2);
                }
            }
            case 2 -> {
                if ((this.physicsTables.readEnemyTransitionCountdown(slot) & 0x1F) != 0) {
                    return;
                }
                if (tailState == 4) {
                    this.physicsTables.writeEnemyTransitionCountdown(slot, 0x30);
                    this.state.writeBossDeathProducerState(slot, 3);
                    return;
                }
                var moldormMotion = this.motions.get(MoldormMotion.class, MoldormMotion::new);
                MoldormSegmentPosition poofPosition =
                        moldormMotion.currentTailSegments(slot).get(3 - tailState);
                this.createMoldormPoof(source, poofPosition.x(), poofPosition.y());
                moldormMotion.setDestructionTailState(slot, tailState + 1);
            }
            default -> this.advanceMoldormBossExplosion(source);
        }
    }

    void advanceMoldormBossExplosion(RoomEntity source) {
        int slot = source.slot();
        int countdown = this.physicsTables.readEnemyTransitionCountdown(slot);
        if (countdown == 0) {
            this.spawnMoldormHeartContainer(source);
            this.lifecycle.disableEntityWithoutPersistence(slot);
            this.events.pendingEntityEvents.add(
                    new EntityCombatEvent(slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x1A));
            return;
        }
        if ((countdown & 0x03) != 0) {
            return;
        }
        int index = (countdown >> 2) & 0x07;
        int[] xOffsets = {0, 6, 8, 6, 0, -6, -8, -6};
        int[] yOffsets = {-8, -6, 0, 6, 8, 6, 0, -6};
        this.createMoldormPoof(
                source, (source.x() + xOffsets[index]) & 0xFF, ((source.y() - source.z()) + yOffsets[index]) & 0xFF);
        if (countdown == 0x10) {
            var moldormMotion = this.motions.get(MoldormMotion.class, MoldormMotion::new);
            moldormMotion.setDestructionTailState(slot, 5);
        }
    }

    void spawnMoldormHeartContainer(RoomEntity source) {
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return;
        }
        int x = Math.max(0x18, Math.min(0x88, source.x()));
        int y = Math.max(0x20, Math.min(0x70, source.y()));
        EntitySpriteDefinition definition =
                this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_HEART_CONTAINER);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        this.slots.replace(
                freeSlot,
                new RoomEntity(
                        freeSlot,
                        -1,
                        EntityRomConstants.ENTITY_HEART_CONTAINER,
                        x,
                        y,
                        EntityStatus.ACTIVE,
                        definition,
                        variant,
                        0,
                        0,
                        source.z(),
                        -1,
                        false));
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.pickups.resetEnemyDropState(freeSlot);
        var enemyDropMotion = this.motions.get(EnemyDropMotion.class, EnemyDropMotion::new);
        enemyDropMotion.initialize(freeSlot, this.physicsTables.readGroundInteractionSideScrolling(), 0x10);
        this.state.writeEnemyDropActive(freeSlot, true);
        this.physicsTables.writeSlowTransitionCountdown(freeSlot, 0);
        this.physicsTables.writeSlowTimerInitialized(freeSlot, false);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeDyingCountdown(freeSlot, 0);
        this.combatTables.writePowerRecoilDeath(freeSlot, false);
        this.physicsTables.writeEnemyPhysicsFlags(
                freeSlot, initialPhysicsFlags(EntityRomConstants.ENTITY_HEART_CONTAINER));
        this.combatTables.writeEnemyHealth(
                freeSlot, this.slotProperties.initialHealth(EntityRomConstants.ENTITY_HEART_CONTAINER));
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        this.physicsTables.writeEntityGroundStatus(freeSlot, 0);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEntityOptions1Override(freeSlot, -1);
        var enemyRecoilMotion = this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(freeSlot);
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }

    void createMoldormPoof(RoomEntity source, int x, int y) {
        this.events.transientVfxRequests.add(new TransientVfxRequest(TransientVfxType.POOF, x, y));
        this.events.pendingEntityEvents.add(new EntityCombatEvent(
                source.slot(), source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
    }

    public void advanceCollectedHeartContainer(RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ) {
        int slot = entity.slot();
        if (this.physicsTables.readEnemyTransitionCountdown(slot) == 1) {
            this.events.pendingHeartContainerRewards.add(new HeartContainerRewardEvent(slot));
            this.lifecycle.disableEntityWithoutPersistence(slot);
            return;
        }
        this.events.pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
        RoomEntity held =
                withPositionAndVariant(entity, linkEntityX, (linkEntityY - 0x0C) & 0xFF, entity.spriteVariant());
        this.slots.replace(slot, withZ(held, linkZ & 0xFF));
    }

    void advanceBossKeyDropProducer(RoomEntity source, IntSupplier randomByteSupplier) {
        this.bossKeyDropStep.run(source, randomByteSupplier);
    }

    final BossKeyDropStep bossKeyDropStep = new BossKeyDropStep(this);

    void handleBowWowTargetContact(
            RoomEntity source, BowWowMotion.TargetContact contact, IntSupplier randomByteSupplier) {
        int targetSlot = contact.slot();
        if (targetSlot < 0 || targetSlot >= this.slots.size()) {
            return;
        }
        RoomEntity target = this.slots.get(targetSlot);
        if (!target.loaded()) {
            return;
        }

        if (target.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            var bowWowMotion = this.motions.get(BowWowMotion.class, BowWowMotion::new);
            if (this.physicsTables.readEntityPrivateState4(targetSlot) == 0
                    || this.linkState.readDialogActive()
                    || bowWowMotion.privateCountdown2(source.slot()) != 0) {
                return;
            }
            bowWowMotion.setTransitionCountdown(source.slot(), 0);
            bowWowMotion.setPrivateCountdown2(source.slot(), 0x80);
            this.events.pendingDialogRequests.add(new DialogRequest(1, EntityRomConstants.DIALOG_BOW_WOW));
            return;
        }

        if (this.combatTables.readEnemyFlashCountdown(targetSlot) != 0) {
            return;
        }
        this.events.pendingEntityEvents.add(new EntityCombatEvent(
                source.slot(), source.type(), 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x03));
        if (target.type() == EntityRomConstants.ENTITY_KIKI_THE_MONKEY) {
            this.combatTables.writeEnemyFlashCountdown(targetSlot, 0x18);
            this.physicsTables.writeEntityInertia(
                    targetSlot, (this.physicsTables.readEntityInertia(targetSlot) + 1) & 0xFF);
            return;
        }
        this.handleTerminalEnemyDeath(target, randomByteSupplier);
    }

    static final class BossKeyDropStep {
        private final linksawakening.world.entity.EntityBossRewards owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity source;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private java.util.function.IntSupplier randomByteSupplier;
        private int slot;
        private int state;

        BossKeyDropStep(linksawakening.world.entity.EntityBossRewards owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        void run() {

            slot = source.slot();
            if (owner.physicsTables.readEnemyTransitionCountdown(slot) != 0) {
                return;
            }

            state = owner.state.readBossDeathProducerState(slot);
            if (source.type() == EntityRomConstants.ENTITY_DESERT_LANMOLA) {
                switch (state) {
                    case 0 -> {
                        if (!advanceState0()) return;
                    }
                    case 1 -> {
                        if (!advanceState1()) return;
                    }
                    default -> {
                        if (!advanceDefault()) return;
                    }
                }
                return;
            }

            if (source.type() == EntityRomConstants.ENTITY_ARMOS_KNIGHT
                    || source.type() == EntityRomConstants.ENTITY_HINOX) {
                switch (state) {
                    case 0 -> {
                        if (!advanceState02()) return;
                    }
                    case 1 -> {
                        if (!advanceState12()) return;
                    }
                    default -> {
                        if (!advanceDefault2()) return;
                    }
                }
                return;
            }

            switch (state) {
                case 0 -> {
                    if (!advanceState03()) return;
                }
                case 1 -> {
                    if (!advanceState13()) return;
                }
                default -> {
                    if (!advanceDefault3()) return;
                }
            }
        }

        private boolean advanceState0() {
            // func_006_5629: transition $60, then private state 2++.
            owner.state.writeBossDeathProducerState(slot, 1);
            owner.physicsTables.writeEnemyTransitionCountdown(slot, 0x60);
            return false;
        }

        private boolean advanceState1() {
            // func_006_563A: transition $CF, then private state 2++.
            owner.state.writeBossDeathProducerState(slot, 2);
            owner.physicsTables.writeEnemyTransitionCountdown(slot, 0xCF);
            return false;
        }

        private boolean advanceDefault() {
            // func_006_564B: variant $02, z from hMultiPurpose3,
            // speedZ $10, and private countdown 1 $10.
            owner.pickups.spawnBossKeyDrop(source, 0x02, source.z(), 0x10, 0x10);
            owner.lifecycle.disableEntityWithoutPersistence(slot);
            owner.events.pendingEntityEvents.add(
                    new EntityCombatEvent(slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));

            return true;
        }

        private boolean advanceState02() {
            // ArmosKnightPrivateState0Handler: transition $A0 and
            // the first full flash countdown.
            owner.state.writeBossDeathProducerState(slot, 1);
            owner.physicsTables.writeEnemyTransitionCountdown(slot, 0xA0);
            owner.combatTables.writeEnemyFlashCountdown(slot, 0xFF);
            return false;
        }

        private boolean advanceState12() {
            // ArmosKnightPrivateState1Handler: the second flash
            // begins only after the first countdown reaches zero.
            owner.state.writeBossDeathProducerState(slot, 2);
            owner.physicsTables.writeEnemyTransitionCountdown(slot, 0xC0);
            owner.combatTables.writeEnemyFlashCountdown(slot, 0xFF);
            return false;
        }

        private boolean advanceDefault2() {
            // The shared inactive handler emits the boss explosion,
            // then calls DidKillEnemy. Armos has pre-forced the key;
            // Hinox uses the ordinary drop resolver here.
            if (source.type() == EntityRomConstants.ENTITY_ARMOS_KNIGHT) {
                int killIndex = owner.state.readKillCount() & 0xFF;
                owner.state.writeKillOrder(killIndex, source.sourceLoadOrder() & 0xFF);
                owner.state.writeKillCount((owner.state.readKillCount() + 1) & 0xFF);
                owner.roomVariables.writePendingClearedEntityMask(
                        owner.roomVariables.readPendingClearedEntityMask() | persistentClearMask(source));
                owner.pickups.spawnEnemyDrop(source, EntityRomConstants.ENTITY_KEY_DROP_POINT);
                owner.lifecycle.disableEntityWithoutPersistence(slot);
            } else {
                owner.handleTerminalEnemyDeath(source, randomByteSupplier);
                // label_006_5355 -> label_006_6CC9 opens the Hinox
                // room event except in Eagle's Tower and later maps.
                if (owner.roomVariables.readEntityMapId() >= 0 && owner.roomVariables.readEntityMapId() < 0x06) {
                    owner.roomVariables.writePendingRoomStatusMask(
                            owner.roomVariables.readPendingRoomStatusMask() | 0x20);
                }
            }
            owner.events.pendingEntityEvents.add(
                    new EntityCombatEvent(slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x1A));

            return true;
        }

        private boolean advanceState03() {
            // func_007_7EB6: transition $A0, then private state 2++.
            owner.state.writeBossDeathProducerState(slot, 1);
            owner.physicsTables.writeEnemyTransitionCountdown(slot, 0xA0);

            return true;
        }

        private boolean advanceState13() {
            // func_007_7EC7: transition $C0, then private state 2++.
            owner.state.writeBossDeathProducerState(slot, 2);
            owner.physicsTables.writeEnemyTransitionCountdown(slot, 0xC0);

            return true;
        }

        private boolean advanceDefault3() {
            // func_007_7ED6: ConfigureNewEntity leaves z at zero;
            // the producer supplies speedZ $18 and countdown 1 $20.
            owner.pickups.spawnBossKeyDrop(source, 0x00, 0x00, 0x18, 0x20);
            owner.lifecycle.disableEntityWithoutPersistence(slot);
            owner.events.pendingEntityEvents.add(
                    new EntityCombatEvent(slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x1A));

            return true;
        }

        private boolean inUse;

        void run(linksawakening.world.entity.RoomEntity source, java.util.function.IntSupplier randomByteSupplier) {
            if (inUse) {
                new BossKeyDropStep(owner).run(source, randomByteSupplier);
                return;
            }
            inUse = true;

            slot = 0;
            state = 0;

            this.source = source;
            this.randomByteSupplier = randomByteSupplier;
            try {
                run();
            } finally {
                inUse = false;

                this.source = null;
                this.randomByteSupplier = null;
            }
        }
    }

    void resetRoomVariables() {
        state.writeInBossBattle(false);
    }
}
