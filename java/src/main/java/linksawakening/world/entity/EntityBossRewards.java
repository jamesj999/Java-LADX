package linksawakening.world.entity;

import static linksawakening.world.entity.EntitySnapshots.withDefinition;
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
    // Optional ROM data, absent in ROM-free runtimes.
    final linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers;
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
    final RoomEntity[] slots;

    final EntityLinkState linkState;

    EntityBossRewards(
            EntityBossState state,
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ linksawakening.entity
                            .EntitySpriteHandlerCatalog
                    spriteHandlers,
            EntityBackgroundMechanics backgroundMechanics,
            EntityCombatTables combatTables,
            EntityEventChannels events,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables,
            EntityPickupController pickups,
            EntityRoomInspection roomInspection,
            EntityRoomVariables roomVariables,
            EntitySlotLifecycle lifecycle,
            EntitySlotProperties slotProperties,
            RoomEntity[] slots,
            EntityLinkState linkState) {
        this.linkState = java.util.Objects.requireNonNull(linkState, "linkState");

        this.state = java.util.Objects.requireNonNull(state, "state");
        this.spriteHandlers = spriteHandlers;
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
            RoomEntity other = this.slots[slot];
            if (slot == killed.slot()
                    || (other.type() != 0x10 && other.type() != 0x11)
                    || this.motions.get(GhiniMotion.class, GhiniMotion::new).state(slot) != 0
                    || other.status() == EntityStatus.DISABLED) continue;
            this.slots[slot] = withStatus(other, EntityStatus.DYING);
            this.combatTables.writeDyingCountdown(slot, 0x1F);
            state.writeDroppedItemBySlot(slot, drops[random.getAsInt() & 3]);
        }
        state.writeDroppedItemBySlot(killed.slot(), 0x2E);
    }

    void handleTerminalEnemyDeath(RoomEntity entity, IntSupplier randomByteSupplier) {
        EntityBossRewards.bossDeathBehaviorHandleTerminalEnemyDeath(this, entity, randomByteSupplier);
    }

    public void advanceRollingBonesDestruction(RoomEntity entity, IntSupplier randomByteSupplier) {
        EntityBossRewards.bossDeathBehaviorAdvanceRollingBonesDestruction(this, entity, randomByteSupplier);
    }

    public void advanceMoblinKingDestruction(RoomEntity source, int frame, IntSupplier random) {
        EntityBossRewards.bossDeathBehaviorAdvanceMoblinKingDestruction(this, source, frame, random);
    }

    public void advanceMoldormDestruction(RoomEntity source, int frameCounter) {
        EntityBossRewards.moldormDeathBehaviorAdvanceMoldormDestruction(this, source, frameCounter);
    }

    void advanceMoldormBossExplosion(RoomEntity source) {
        EntityBossRewards.moldormDeathBehaviorAdvanceMoldormBossExplosion(this, source);
    }

    void spawnMoldormHeartContainer(RoomEntity source) {
        EntityBossRewards.moldormDeathBehaviorSpawnMoldormHeartContainer(this, source);
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
        this.slots[slot] = withZ(held, linkZ & 0xFF);
    }

    void advanceBossKeyDropProducer(RoomEntity source, IntSupplier randomByteSupplier) {
        EntityBossRewards.bossKeyDropBehaviorAdvanceBossKeyDropProducer(this, source, randomByteSupplier);
    }

    final BossKeyDropStep bossKeyDropStep = new BossKeyDropStep(this);

    void handleBowWowTargetContact(
            RoomEntity source, BowWowMotion.TargetContact contact, IntSupplier randomByteSupplier) {
        int targetSlot = contact.slot();
        if (targetSlot < 0 || targetSlot >= this.slots.length) {
            return;
        }
        RoomEntity target = this.slots[targetSlot];
        if (!target.loaded()) {
            return;
        }

        if (target.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            if (this.physicsTables.readEntityPrivateState4(targetSlot) == 0
                    || this.linkState.dialogActive
                    || this.motions.get(BowWowMotion.class, BowWowMotion::new).privateCountdown2(source.slot()) != 0) {
                return;
            }
            this.motions.get(BowWowMotion.class, BowWowMotion::new).setTransitionCountdown(source.slot(), 0);
            this.motions.get(BowWowMotion.class, BowWowMotion::new).setPrivateCountdown2(source.slot(), 0x80);
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

    static void bossDeathBehaviorHandleTerminalEnemyDeath(
            EntityBossRewards owner, RoomEntity entity, IntSupplier randomByteSupplier) {
        var runtime = owner;
        boolean swallowedShield = entity.type() == EntityRomConstants.ENTITY_LIKE_LIKE
                && runtime.motions
                                .get(LikeLikeMotion.class, LikeLikeMotion::new)
                                .privateState1(entity.slot())
                        != 0;
        if (entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING) {
            // The mini-boss option reaches DidKillEnemy's room-event branch,
            // permanently opening the east rescue-room path.
            runtime.roomVariables.writePendingRoomStatusMask(
                    runtime.roomVariables.readPendingRoomStatusMask() | (0x20));
        }
        if (entity.sourceLoadOrder() < 0 || (owner.state.readEnemyDropResolver() == null && !swallowedShield)) {
            if (entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING) {
                runtime.roomVariables.writePendingClearedEntityMask(
                        runtime.roomVariables.readPendingClearedEntityMask() | (persistentClearMask(entity)));
            }
            runtime.lifecycle.disableEntityWithoutPersistence(entity.slot());
            return;
        }

        EnemyDropResolver.Result result;
        if (swallowedShield) {
            result = new EnemyDropResolver.Result(
                    EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP, owner.state.readEnemyDropCounters(), 0);
        } else {
            EnemyDropResolver.Context context = new EnemyDropResolver.Context(
                    entity.type(),
                    runtime.roomInspection.enemyDropHealthGroup(entity.slot()),
                    owner.state.readDroppedItemBySlot(entity.slot()),
                    owner.state.readEnemyDropMaxHearts(),
                    owner.state.readEnemyDropHealth(),
                    owner.state.readInBossBattle(),
                    owner.state.readEnemyDropActivePowerUp(),
                    runtime.physicsTables.readGroundInteractionSideScrolling(),
                    owner.state.readEnemyDropCounters(),
                    randomByteSupplier);
            result = owner.resolveDrop(context);
            owner.state.writeEnemyDropCounters(result.counters());
        }

        int killIndex = owner.state.readKillCount() & 0xFF;
        owner.state.writeKillOrder(killIndex, entity.sourceLoadOrder() & 0xFF);
        owner.state.writeKillCount((owner.state.readKillCount() + 1) & 0xFF);
        runtime.roomVariables.writePendingClearedEntityMask(
                runtime.roomVariables.readPendingClearedEntityMask() | (persistentClearMask(entity)));
        if (result.dropped()) {
            runtime.pickups.spawnEnemyDrop(entity, result.itemType());
        }
        runtime.lifecycle.disableEntityWithoutPersistence(entity.slot());
    }

    static void bossDeathBehaviorAdvanceRollingBonesDestruction(
            EntityBossRewards owner, RoomEntity entity, IntSupplier randomByteSupplier) {
        var runtime = owner;
        int slot = entity.slot();
        if (!runtime.motions
                .get(RollingBonesMotion.class, RollingBonesMotion::new)
                .deathInitialized(slot)) {
            runtime.motions
                    .get(RollingBonesMotion.class, RollingBonesMotion::new)
                    .setDeathInitialized(slot, true);
            runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0xFF);
            runtime.combatTables.writeEnemyFlashCountdown(slot, 0xFF);
            return;
        }
        int countdown = runtime.physicsTables.readEnemyTransitionCountdown(slot);
        runtime.combatTables.writeEnemyFlashCountdown(slot, countdown);
        if (countdown == 0) {
            // $06:6CC1 calls DidKillEnemy, including SpawnEnemyDrop. The
            // ROM health-group tables select the miniboss healing fairy.
            if (owner.state.readEnemyDropResolver() != null) {
                EnemyDropResolver.Result drop = owner.resolveDrop(new EnemyDropResolver.Context(
                        entity.type(),
                        runtime.roomInspection.enemyDropHealthGroup(entity.slot()),
                        owner.state.readDroppedItemBySlot(slot),
                        owner.state.readEnemyDropMaxHearts(),
                        owner.state.readEnemyDropHealth(),
                        owner.state.readInBossBattle(),
                        owner.state.readEnemyDropActivePowerUp(),
                        runtime.physicsTables.readGroundInteractionSideScrolling(),
                        owner.state.readEnemyDropCounters(),
                        randomByteSupplier));
                owner.state.writeEnemyDropCounters(drop.counters());
                if (drop.dropped()) runtime.pickups.spawnEnemyDrop(entity, drop.itemType());
            }
            int killIndex = owner.state.readKillCount() & 0xFF;
            owner.state.writeKillOrder(killIndex, entity.sourceLoadOrder() & 0xFF);
            owner.state.writeKillCount((owner.state.readKillCount() + 1) & 0xFF);
            runtime.roomVariables.writePendingClearedEntityMask(
                    runtime.roomVariables.readPendingClearedEntityMask() | (persistentClearMask(entity)));
            if (runtime.roomVariables.readEntityMapId() != 0x07) {
                runtime.roomVariables.writePendingRoomStatusMask(
                        runtime.roomVariables.readPendingRoomStatusMask() | (0x20));
            }
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
            return;
        }
        if (countdown < 0x80 && (countdown & 0x07) == 0) {
            int x = (entity.x() + ((randomByteSupplier.getAsInt() & 0x1F) - 0x10)) & 0xFF;
            int y = (entity.y() - entity.z() + ((randomByteSupplier.getAsInt() & 0x1F) - 0x14)) & 0xFF;
            runtime.events.transientVfxRequests.add(new TransientVfxRequest(TransientVfxType.POOF, x, y));
            runtime.events.pendingEntityEvents.add(
                    new EntityCombatEvent(slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
        }
    }

    static void bossDeathBehaviorAdvanceMoblinKingDestruction(
            EntityBossRewards owner, RoomEntity source, int frame, IntSupplier random) {
        var runtime = owner;
        int slot = source.slot();
        // Main resolves sword damage before dispatch; the ROM dispatches the
        // dying branch on the following frame, after this hit's active handler.
        boolean hitThisFrame = runtime.motions
                        .get(MoblinKingMotion.class, MoblinKingMotion::new)
                        .lethalHitFrame(slot)
                == frame + 1;
        runtime.motions.get(MoblinKingMotion.class, MoblinKingMotion::new).setLethalHitFrame(slot, 0);
        if (hitThisFrame) return;
        if (runtime.spriteHandlers != null)
            source = withDefinition(
                    source, runtime.slotProperties.spriteViews().forMoblinKingState(12, -1, source.z() != 0), 0);
        runtime.slots[slot] = withFlipAttribute(
                source,
                runtime.physicsTables.readBaseEntityFlipAttribute(slot)
                        ^ ((runtime.combatTables.readEnemyFlashCountdown(slot) << 2) & 0x10));
        if (owner.state.readBossDeathProducerState(slot) == 0) {
            runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0xA0);
            runtime.combatTables.writeEnemyFlashCountdown(slot, 0xFF);
            owner.state.writeBossDeathProducerState(slot, 1);
        } else if (owner.state.readBossDeathProducerState(slot) == 1) {
            if (runtime.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
                runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0xC0);
                runtime.combatTables.writeEnemyFlashCountdown(slot, 0xFF);
                owner.state.writeBossDeathProducerState(slot, 2);
            }
        } else if (runtime.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
            runtime.events.pendingEntityEvents.add(
                    new EntityCombatEvent(slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x0C));
            runtime.motions
                    .get(MoblinSwordMotion.class, MoblinSwordMotion::new)
                    .writeSwordMoblinAlertingSoundCounter(4);
            owner.handleTerminalEnemyDeath(source, random);
        } else if ((runtime.physicsTables.readEnemyTransitionCountdown(slot) & 7) == 0
                && runtime.backgroundMechanics.entityHandlerInteractive(false)) {
            int x = (source.x() + (random.getAsInt() & 0x1F) - 0x10) & 255;
            int y = (source.y() - source.z() + (random.getAsInt() & 0x1F) - 0x14) & 255;
            runtime.events.transientVfxRequests.add(new TransientVfxRequest(TransientVfxType.POOF, x, y));
            runtime.events.pendingEntityEvents.add(
                    new EntityCombatEvent(slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
        }
    }

    static void bossKeyDropBehaviorAdvanceBossKeyDropProducer(
            EntityBossRewards owner, RoomEntity source, IntSupplier randomByteSupplier) {
        owner.bossKeyDropStep.run(owner, source, randomByteSupplier);
    }

    static void moldormDeathBehaviorAdvanceMoldormDestruction(
            EntityBossRewards owner, RoomEntity source, int frameCounter) {
        var runtime = owner;
        int slot = source.slot();
        int tailState =
                runtime.motions.get(MoldormMotion.class, MoldormMotion::new).destructionTailState(slot);
        if (runtime.spriteHandlers != null) {
            RoomEntity rendered = withDefinition(
                    source,
                    runtime.spriteHandlers.forMoldormState(
                            runtime.motions
                                    .get(MoldormMotion.class, MoldormMotion::new)
                                    .headSpriteVariant(slot),
                            source.x(),
                            (source.y() - source.z()) & 0xFF,
                            runtime.motions
                                    .get(MoldormMotion.class, MoldormMotion::new)
                                    .currentTailSegments(slot),
                            tailState,
                            frameCounter),
                    0);
            runtime.slots[slot] = withFlipAttribute(
                    rendered,
                    runtime.physicsTables.readBaseEntityFlipAttribute(slot)
                            ^ ((runtime.combatTables.readEnemyFlashCountdown(slot) << 2) & 0x10));
        }

        switch (owner.state.readBossDeathProducerState(slot)) {
            case 0 -> {
                runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0x60);
                runtime.combatTables.writeEnemyFlashCountdown(slot, 0xFF);
                owner.state.writeBossDeathProducerState(slot, 1);
            }
            case 1 -> {
                if (runtime.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
                    runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0xFF);
                    runtime.combatTables.writeEnemyFlashCountdown(slot, 0xFF);
                    owner.state.writeBossDeathProducerState(slot, 2);
                }
            }
            case 2 -> {
                if ((runtime.physicsTables.readEnemyTransitionCountdown(slot) & 0x1F) != 0) {
                    return;
                }
                if (tailState == 4) {
                    runtime.physicsTables.writeEnemyTransitionCountdown(slot, 0x30);
                    owner.state.writeBossDeathProducerState(slot, 3);
                    return;
                }
                MoldormSegmentPosition poofPosition = runtime.motions
                        .get(MoldormMotion.class, MoldormMotion::new)
                        .currentTailSegments(slot)
                        .get(3 - tailState);
                owner.createMoldormPoof(source, poofPosition.x(), poofPosition.y());
                runtime.motions
                        .get(MoldormMotion.class, MoldormMotion::new)
                        .setDestructionTailState(slot, tailState + 1);
            }
            default -> owner.advanceMoldormBossExplosion(source);
        }
    }

    static void moldormDeathBehaviorAdvanceMoldormBossExplosion(EntityBossRewards owner, RoomEntity source) {
        var runtime = owner;
        int slot = source.slot();
        int countdown = runtime.physicsTables.readEnemyTransitionCountdown(slot);
        if (countdown == 0) {
            owner.spawnMoldormHeartContainer(source);
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
            runtime.events.pendingEntityEvents.add(
                    new EntityCombatEvent(slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x1A));
            return;
        }
        if ((countdown & 0x03) != 0) {
            return;
        }
        int index = (countdown >> 2) & 0x07;
        int[] xOffsets = {0, 6, 8, 6, 0, -6, -8, -6};
        int[] yOffsets = {-8, -6, 0, 6, 8, 6, 0, -6};
        owner.createMoldormPoof(
                source, (source.x() + xOffsets[index]) & 0xFF, ((source.y() - source.z()) + yOffsets[index]) & 0xFF);
        if (countdown == 0x10) {
            runtime.motions.get(MoldormMotion.class, MoldormMotion::new).setDestructionTailState(slot, 5);
        }
    }

    static void moldormDeathBehaviorSpawnMoldormHeartContainer(EntityBossRewards owner, RoomEntity source) {
        var runtime = owner;
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
        if (freeSlot < 0) {
            return;
        }
        int x = Math.max(0x18, Math.min(0x88, source.x()));
        int y = Math.max(0x20, Math.min(0x70, source.y()));
        EntitySpriteDefinition definition =
                runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_HEART_CONTAINER);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        runtime.slots[freeSlot] = new RoomEntity(
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
                false);
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.pickups.resetEnemyDropState(freeSlot);
        runtime.motions
                .get(EnemyDropMotion.class, EnemyDropMotion::new)
                .initialize(freeSlot, runtime.physicsTables.readGroundInteractionSideScrolling(), 0x10);
        owner.state.writeEnemyDropActive(freeSlot, true);
        runtime.physicsTables.writeSlowTransitionCountdown(freeSlot, 0);
        runtime.physicsTables.writeSlowTimerInitialized(freeSlot, false);
        runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeDyingCountdown(freeSlot, 0);
        runtime.combatTables.writePowerRecoilDeath(freeSlot, false);
        runtime.physicsTables.writeEnemyPhysicsFlags(
                freeSlot, initialPhysicsFlags(EntityRomConstants.ENTITY_HEART_CONTAINER));
        runtime.combatTables.writeEnemyHealth(
                freeSlot, runtime.slotProperties.initialHealth(EntityRomConstants.ENTITY_HEART_CONTAINER));
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.physicsTables.writeEntityGroundStatus(freeSlot, 0);
        runtime.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables.writeEntityOptions1Override(freeSlot, -1);
        runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
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
                        owner.roomVariables.readPendingClearedEntityMask() | (persistentClearMask(source)));
                owner.pickups.spawnEnemyDrop(source, EntityRomConstants.ENTITY_KEY_DROP_POINT);
                owner.lifecycle.disableEntityWithoutPersistence(slot);
            } else {
                owner.handleTerminalEnemyDeath(source, randomByteSupplier);
                // label_006_5355 -> label_006_6CC9 opens the Hinox
                // room event except in Eagle's Tower and later maps.
                if (owner.roomVariables.readEntityMapId() >= 0 && owner.roomVariables.readEntityMapId() < 0x06) {
                    owner.roomVariables.writePendingRoomStatusMask(
                            owner.roomVariables.readPendingRoomStatusMask() | (0x20));
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

        void run(
                linksawakening.world.entity.EntityBossRewards owner,
                linksawakening.world.entity.RoomEntity source,
                java.util.function.IntSupplier randomByteSupplier) {
            if (inUse) {
                new BossKeyDropStep(owner).run(owner, source, randomByteSupplier);
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
}
