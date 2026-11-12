package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.MoldormSegmentPosition;
import linksawakening.world.entity.event.TransientVfxRequest;

/** Boss destruction and source-shaped reward production. */
public final class EntityBossRewards {
    private int bossAgonySfxCountdown;
    private int bossAgonySourceSlot;
    private final int[] droppedItemBySlot = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] dropPrivateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] dropPrivateCountdown3 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] droppablePrivateState3 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] droppablePrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] enemyDropActive = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bossDeathProducerState = new int[EntityRoomLoader.MAX_ENTITIES];
    private EnemyDropResolver enemyDropResolver;
    private EnemyDropResolver.CounterState enemyDropCounters = new EnemyDropResolver.CounterState(0, 0);
    private int enemyDropMaxHearts = 3;
    private int enemyDropHealth = 6;
    private Boolean enemyDropLowHealth;
    private boolean enemyDropActivePowerUp;
    private boolean inBossBattle;
    private int killCount;
    private final int[] killOrder = new int[0x100];

    private EnemyDropResolver.Result resolveDrop(EnemyDropResolver.Context context) {
        return this.readEnemyDropLowHealth() == null
                ? this.readEnemyDropResolver().resolve(context)
                : this.readEnemyDropResolver().resolve(context, this.readEnemyDropLowHealth());
    }

    private final EntityBossRewardsServices runtime;

    EntityBossRewards(EntityBossRewardsServices runtime) {
        this.runtime = runtime;
    }

    /** ApplySwordDamagesToEnemy, $03:7361: preserve descending RNG order. */
    void cascadeGhiniDeath(RoomEntity killed, IntSupplier random) {
        if (killed.type() != 0x12) return;
        int[] drops = {0x2D, 0x2E, 0x38, 0x37};
        for (int slot = RoomEntity.SLOT_COUNT - 1; slot >= 0; slot--) {
            RoomEntity other = runtime.slots()[slot];
            if (slot == killed.slot()
                    || other == null
                    || (other.type() != 0x10 && other.type() != 0x11)
                    || runtime.motionSet().ghiniMotion.state(slot) != 0
                    || other.status() == EntityStatus.DISABLED) continue;
            runtime.slots()[slot] = withStatus(other, EntityStatus.DYING);
            runtime.combatTables().writeDyingCountdown(slot, 0x1F);
            this.writeDroppedItemBySlot(slot, drops[random.getAsInt() & 3]);
        }
        this.writeDroppedItemBySlot(killed.slot(), 0x2E);
    }

    void handleTerminalEnemyDeath(RoomEntity entity, IntSupplier randomByteSupplier) {
        boolean swallowedShield = entity.type() == EntityRomConstants.ENTITY_LIKE_LIKE
                && runtime.motionSet().likeLikeMotion.privateState1(entity.slot()) != 0;
        if (entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING) {
            // The mini-boss option reaches DidKillEnemy's room-event branch,
            // permanently opening the east rescue-room path.
            runtime.roomVariables()
                    .writePendingRoomStatusMask(runtime.roomVariables().readPendingRoomStatusMask() | (0x20));
        }
        if (entity.sourceLoadOrder() < 0 || (this.readEnemyDropResolver() == null && !swallowedShield)) {
            if (entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING) {
                runtime.roomVariables()
                        .writePendingClearedEntityMask(
                                runtime.roomVariables().readPendingClearedEntityMask() | (persistentClearMask(entity)));
            }
            runtime.lifecycle().disableEntityWithoutPersistence(entity.slot());
            return;
        }

        EnemyDropResolver.Result result;
        if (swallowedShield) {
            result = new EnemyDropResolver.Result(
                    EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP, this.readEnemyDropCounters(), 0);
        } else {
            EnemyDropResolver.Context context = new EnemyDropResolver.Context(
                    entity.type(),
                    runtime.roomInspection().enemyDropHealthGroup(entity.slot()),
                    this.readDroppedItemBySlot(entity.slot()),
                    this.readEnemyDropMaxHearts(),
                    this.readEnemyDropHealth(),
                    this.readInBossBattle(),
                    this.readEnemyDropActivePowerUp(),
                    runtime.physicsTables().readGroundInteractionSideScrolling(),
                    this.readEnemyDropCounters(),
                    randomByteSupplier);
            result = resolveDrop(context);
            this.writeEnemyDropCounters(result.counters());
        }

        int killIndex = this.readKillCount() & 0xFF;
        this.writeKillOrder(killIndex, entity.sourceLoadOrder() & 0xFF);
        this.writeKillCount((this.readKillCount() + 1) & 0xFF);
        runtime.roomVariables()
                .writePendingClearedEntityMask(
                        runtime.roomVariables().readPendingClearedEntityMask() | (persistentClearMask(entity)));
        if (result.dropped()) {
            runtime.pickups().spawnEnemyDrop(entity, result.itemType());
        }
        runtime.lifecycle().disableEntityWithoutPersistence(entity.slot());
    }

    public void advanceRollingBonesDestruction(RoomEntity entity, IntSupplier randomByteSupplier) {
        int slot = entity.slot();
        if (!runtime.motionSet().rollingBonesMotion.deathInitialized(slot)) {
            runtime.motionSet().rollingBonesMotion.setDeathInitialized(slot, true);
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xFF);
            runtime.combatTables().writeEnemyFlashCountdown(slot, 0xFF);
            return;
        }
        int countdown = runtime.physicsTables().readEnemyTransitionCountdown(slot);
        runtime.combatTables().writeEnemyFlashCountdown(slot, countdown);
        if (countdown == 0) {
            // $06:6CC1 calls DidKillEnemy, including SpawnEnemyDrop. The
            // ROM health-group tables select the miniboss healing fairy.
            if (this.readEnemyDropResolver() != null) {
                EnemyDropResolver.Result drop = resolveDrop(new EnemyDropResolver.Context(
                        entity.type(),
                        runtime.roomInspection().enemyDropHealthGroup(entity.slot()),
                        this.readDroppedItemBySlot(slot),
                        this.readEnemyDropMaxHearts(),
                        this.readEnemyDropHealth(),
                        this.readInBossBattle(),
                        this.readEnemyDropActivePowerUp(),
                        runtime.physicsTables().readGroundInteractionSideScrolling(),
                        this.readEnemyDropCounters(),
                        randomByteSupplier));
                this.writeEnemyDropCounters(drop.counters());
                if (drop.dropped()) runtime.pickups().spawnEnemyDrop(entity, drop.itemType());
            }
            int killIndex = this.readKillCount() & 0xFF;
            this.writeKillOrder(killIndex, entity.sourceLoadOrder() & 0xFF);
            this.writeKillCount((this.readKillCount() + 1) & 0xFF);
            runtime.roomVariables()
                    .writePendingClearedEntityMask(
                            runtime.roomVariables().readPendingClearedEntityMask() | (persistentClearMask(entity)));
            if (runtime.roomVariables().readEntityMapId() != 0x07) {
                runtime.roomVariables()
                        .writePendingRoomStatusMask(runtime.roomVariables().readPendingRoomStatusMask() | (0x20));
            }
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
            return;
        }
        if (countdown < 0x80 && (countdown & 0x07) == 0) {
            int x = (entity.x() + ((randomByteSupplier.getAsInt() & 0x1F) - 0x10)) & 0xFF;
            int y = (entity.y() - entity.z() + ((randomByteSupplier.getAsInt() & 0x1F) - 0x14)) & 0xFF;
            runtime.events().transientVfxRequests.add(new TransientVfxRequest(TransientVfxType.POOF, x, y));
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
        }
    }

    public void advanceMoblinKingDestruction(RoomEntity source, int frame, IntSupplier random) {
        int slot = source.slot();
        // Main resolves sword damage before dispatch; the ROM dispatches the
        // dying branch on the following frame, after this hit's active handler.
        boolean hitThisFrame = runtime.motionSet().moblinKingMotion.lethalHitFrame(slot) == frame + 1;
        runtime.motionSet().moblinKingMotion.setLethalHitFrame(slot, 0);
        if (hitThisFrame) return;
        if (runtime.spriteHandlers() != null)
            source = withDefinition(source, runtime.spriteHandlers().forMoblinKingState(12, -1, source.z() != 0), 0);
        runtime.slots()[slot] = withFlipAttribute(
                source,
                runtime.physicsTables().readBaseEntityFlipAttribute(slot)
                        ^ ((runtime.combatTables().readEnemyFlashCountdown(slot) << 2) & 0x10));
        if (this.readBossDeathProducerState(slot) == 0) {
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xA0);
            runtime.combatTables().writeEnemyFlashCountdown(slot, 0xFF);
            this.writeBossDeathProducerState(slot, 1);
        } else if (this.readBossDeathProducerState(slot) == 1) {
            if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xC0);
                runtime.combatTables().writeEnemyFlashCountdown(slot, 0xFF);
                this.writeBossDeathProducerState(slot, 2);
            }
        } else if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x0C));
            runtime.motionSet().moblinSwordMotion.writeSwordMoblinAlertingSoundCounter(4);
            handleTerminalEnemyDeath(source, random);
        } else if ((runtime.physicsTables().readEnemyTransitionCountdown(slot) & 7) == 0
                && runtime.backgroundMechanics().entityHandlerInteractive(false)) {
            int x = (source.x() + (random.getAsInt() & 0x1F) - 0x10) & 255;
            int y = (source.y() - source.z() + (random.getAsInt() & 0x1F) - 0x14) & 255;
            runtime.events().transientVfxRequests.add(new TransientVfxRequest(TransientVfxType.POOF, x, y));
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
        }
    }

    public void advanceMoldormDestruction(RoomEntity source, int frameCounter) {
        int slot = source.slot();
        int tailState = runtime.motionSet().moldormMotion.destructionTailState(slot);
        if (runtime.spriteHandlers() != null) {
            RoomEntity rendered = withDefinition(
                    source,
                    runtime.spriteHandlers()
                            .forMoldormState(
                                    runtime.motionSet().moldormMotion.headSpriteVariant(slot),
                                    source.x(),
                                    (source.y() - source.z()) & 0xFF,
                                    runtime.motionSet().moldormMotion.currentTailSegments(slot),
                                    tailState,
                                    frameCounter),
                    0);
            runtime.slots()[slot] = withFlipAttribute(
                    rendered,
                    runtime.physicsTables().readBaseEntityFlipAttribute(slot)
                            ^ ((runtime.combatTables().readEnemyFlashCountdown(slot) << 2) & 0x10));
        }

        switch (this.readBossDeathProducerState(slot)) {
            case 0 -> {
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x60);
                runtime.combatTables().writeEnemyFlashCountdown(slot, 0xFF);
                this.writeBossDeathProducerState(slot, 1);
            }
            case 1 -> {
                if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
                    runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xFF);
                    runtime.combatTables().writeEnemyFlashCountdown(slot, 0xFF);
                    this.writeBossDeathProducerState(slot, 2);
                }
            }
            case 2 -> {
                if ((runtime.physicsTables().readEnemyTransitionCountdown(slot) & 0x1F) != 0) {
                    return;
                }
                if (tailState == 4) {
                    runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x30);
                    this.writeBossDeathProducerState(slot, 3);
                    return;
                }
                MoldormSegmentPosition poofPosition = runtime.motionSet()
                        .moldormMotion
                        .currentTailSegments(slot)
                        .get(3 - tailState);
                createMoldormPoof(source, poofPosition.x(), poofPosition.y());
                runtime.motionSet().moldormMotion.setDestructionTailState(slot, tailState + 1);
            }
            default -> advanceMoldormBossExplosion(source);
        }
    }

    void advanceMoldormBossExplosion(RoomEntity source) {
        int slot = source.slot();
        int countdown = runtime.physicsTables().readEnemyTransitionCountdown(slot);
        if (countdown == 0) {
            spawnMoldormHeartContainer(source);
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x1A));
            return;
        }
        if ((countdown & 0x03) != 0) {
            return;
        }
        int index = (countdown >> 2) & 0x07;
        int[] xOffsets = {0, 6, 8, 6, 0, -6, -8, -6};
        int[] yOffsets = {-8, -6, 0, 6, 8, 6, 0, -6};
        createMoldormPoof(
                source, (source.x() + xOffsets[index]) & 0xFF, ((source.y() - source.z()) + yOffsets[index]) & 0xFF);
        if (countdown == 0x10) {
            runtime.motionSet().moldormMotion.setDestructionTailState(slot, 5);
        }
    }

    void spawnMoldormHeartContainer(RoomEntity source) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }
        int x = Math.max(0x18, Math.min(0x88, source.x()));
        int y = Math.max(0x20, Math.min(0x70, source.y()));
        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_HEART_CONTAINER);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        runtime.slots()[freeSlot] = new RoomEntity(
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
        runtime.initializeHealthGroup(freeSlot);
        runtime.pickups().resetEnemyDropState(freeSlot);
        runtime.motionSet()
                .enemyDropMotion
                .initialize(freeSlot, runtime.physicsTables().readGroundInteractionSideScrolling(), 0x10);
        this.writeEnemyDropActive(freeSlot, true);
        runtime.physicsTables().writeSlowTransitionCountdown(freeSlot, 0);
        runtime.physicsTables().writeSlowTimerInitialized(freeSlot, false);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(freeSlot, initialPhysicsFlags(EntityRomConstants.ENTITY_HEART_CONTAINER));
        runtime.combatTables()
                .writeEnemyHealth(freeSlot, runtime.initialHealth(EntityRomConstants.ENTITY_HEART_CONTAINER));
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.physicsTables().writeEntityGroundStatus(freeSlot, 0);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, -1);
        runtime.motionSet().enemyRecoilMotion.clear(freeSlot);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }

    void createMoldormPoof(RoomEntity source, int x, int y) {
        runtime.events().transientVfxRequests.add(new TransientVfxRequest(TransientVfxType.POOF, x, y));
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        source.slot(), source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
    }

    public void advanceCollectedHeartContainer(RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ) {
        int slot = entity.slot();
        if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 1) {
            runtime.events().pendingHeartContainerRewards.add(new HeartContainerRewardEvent(slot));
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
            return;
        }
        runtime.events().pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
        RoomEntity held =
                withPositionAndVariant(entity, linkEntityX, (linkEntityY - 0x0C) & 0xFF, entity.spriteVariant());
        runtime.slots()[slot] = withZ(held, linkZ & 0xFF);
    }

    void advanceBossKeyDropProducer(RoomEntity source, IntSupplier randomByteSupplier) {
        int slot = source.slot();
        if (runtime.physicsTables().readEnemyTransitionCountdown(slot) != 0) {
            return;
        }

        int state = this.readBossDeathProducerState(slot);
        if (source.type() == EntityRomConstants.ENTITY_DESERT_LANMOLA) {
            switch (state) {
                case 0 -> {
                    // func_006_5629: transition $60, then private state 2++.
                    this.writeBossDeathProducerState(slot, 1);
                    runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x60);
                    return;
                }
                case 1 -> {
                    // func_006_563A: transition $CF, then private state 2++.
                    this.writeBossDeathProducerState(slot, 2);
                    runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xCF);
                    return;
                }
                default -> {
                    // func_006_564B: variant $02, z from hMultiPurpose3,
                    // speedZ $10, and private countdown 1 $10.
                    runtime.pickups().spawnBossKeyDrop(source, 0x02, source.z(), 0x10, 0x10);
                    runtime.lifecycle().disableEntityWithoutPersistence(slot);
                    runtime.events()
                            .pendingEntityEvents
                            .add(new EntityCombatEvent(
                                    slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
                }
            }
            return;
        }

        if (source.type() == EntityRomConstants.ENTITY_ARMOS_KNIGHT
                || source.type() == EntityRomConstants.ENTITY_HINOX) {
            switch (state) {
                case 0 -> {
                    // ArmosKnightPrivateState0Handler: transition $A0 and
                    // the first full flash countdown.
                    this.writeBossDeathProducerState(slot, 1);
                    runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xA0);
                    runtime.combatTables().writeEnemyFlashCountdown(slot, 0xFF);
                    return;
                }
                case 1 -> {
                    // ArmosKnightPrivateState1Handler: the second flash
                    // begins only after the first countdown reaches zero.
                    this.writeBossDeathProducerState(slot, 2);
                    runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xC0);
                    runtime.combatTables().writeEnemyFlashCountdown(slot, 0xFF);
                    return;
                }
                default -> {
                    // The shared inactive handler emits the boss explosion,
                    // then calls DidKillEnemy. Armos has pre-forced the key;
                    // Hinox uses the ordinary drop resolver here.
                    if (source.type() == EntityRomConstants.ENTITY_ARMOS_KNIGHT) {
                        int killIndex = this.readKillCount() & 0xFF;
                        this.writeKillOrder(killIndex, source.sourceLoadOrder() & 0xFF);
                        this.writeKillCount((this.readKillCount() + 1) & 0xFF);
                        runtime.roomVariables()
                                .writePendingClearedEntityMask(
                                        runtime.roomVariables().readPendingClearedEntityMask()
                                                | (persistentClearMask(source)));
                        runtime.pickups().spawnEnemyDrop(source, EntityRomConstants.ENTITY_KEY_DROP_POINT);
                        runtime.lifecycle().disableEntityWithoutPersistence(slot);
                    } else {
                        handleTerminalEnemyDeath(source, randomByteSupplier);
                        // label_006_5355 -> label_006_6CC9 opens the Hinox
                        // room event except in Eagle's Tower and later maps.
                        if (runtime.roomVariables().readEntityMapId() >= 0
                                && runtime.roomVariables().readEntityMapId() < 0x06) {
                            runtime.roomVariables()
                                    .writePendingRoomStatusMask(
                                            runtime.roomVariables().readPendingRoomStatusMask() | (0x20));
                        }
                    }
                    runtime.events()
                            .pendingEntityEvents
                            .add(new EntityCombatEvent(
                                    slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x1A));
                }
            }
            return;
        }

        switch (state) {
            case 0 -> {
                // func_007_7EB6: transition $A0, then private state 2++.
                this.writeBossDeathProducerState(slot, 1);
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xA0);
            }
            case 1 -> {
                // func_007_7EC7: transition $C0, then private state 2++.
                this.writeBossDeathProducerState(slot, 2);
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xC0);
            }
            default -> {
                // func_007_7ED6: ConfigureNewEntity leaves z at zero;
                // the producer supplies speedZ $18 and countdown 1 $20.
                runtime.pickups().spawnBossKeyDrop(source, 0x00, 0x00, 0x18, 0x20);
                runtime.lifecycle().disableEntityWithoutPersistence(slot);
                runtime.events()
                        .pendingEntityEvents
                        .add(new EntityCombatEvent(
                                slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x1A));
            }
        }
    }

    int readBossAgonySfxCountdown() {
        return bossAgonySfxCountdown;
    }

    int writeBossAgonySfxCountdown(int value) {
        return bossAgonySfxCountdown = value;
    }

    int postBossAgonySfxCountdown(int delta) {
        int previous = bossAgonySfxCountdown;
        bossAgonySfxCountdown += delta;
        return previous;
    }

    int readBossAgonySourceSlot() {
        return bossAgonySourceSlot;
    }

    int writeBossAgonySourceSlot(int value) {
        return bossAgonySourceSlot = value;
    }

    int readDroppedItemBySlot(int slot) {
        return droppedItemBySlot[slot];
    }

    int writeDroppedItemBySlot(int slot, int value) {
        return droppedItemBySlot[slot] = value;
    }

    int[] storageDroppedItemBySlot() {
        return droppedItemBySlot;
    }

    int readDropPrivateCountdown1(int slot) {
        return dropPrivateCountdown1[slot];
    }

    int writeDropPrivateCountdown1(int slot, int value) {
        return dropPrivateCountdown1[slot] = value;
    }

    int postDropPrivateCountdown1(int slot, int delta) {
        int previous = dropPrivateCountdown1[slot];
        dropPrivateCountdown1[slot] += delta;
        return previous;
    }

    int readDropPrivateCountdown3(int slot) {
        return dropPrivateCountdown3[slot];
    }

    int writeDropPrivateCountdown3(int slot, int value) {
        return dropPrivateCountdown3[slot] = value;
    }

    int postDropPrivateCountdown3(int slot, int delta) {
        int previous = dropPrivateCountdown3[slot];
        dropPrivateCountdown3[slot] += delta;
        return previous;
    }

    int readDroppablePrivateState3(int slot) {
        return droppablePrivateState3[slot];
    }

    int writeDroppablePrivateState3(int slot, int value) {
        return droppablePrivateState3[slot] = value;
    }

    int readDroppablePrivateState4(int slot) {
        return droppablePrivateState4[slot];
    }

    int writeDroppablePrivateState4(int slot, int value) {
        return droppablePrivateState4[slot] = value;
    }

    boolean readEnemyDropActive(int slot) {
        return enemyDropActive[slot];
    }

    boolean writeEnemyDropActive(int slot, boolean value) {
        return enemyDropActive[slot] = value;
    }

    int readBossDeathProducerState(int slot) {
        return bossDeathProducerState[slot];
    }

    int writeBossDeathProducerState(int slot, int value) {
        return bossDeathProducerState[slot] = value;
    }

    EnemyDropResolver readEnemyDropResolver() {
        return enemyDropResolver;
    }

    EnemyDropResolver writeEnemyDropResolver(EnemyDropResolver value) {
        return enemyDropResolver = value;
    }

    EnemyDropResolver.CounterState readEnemyDropCounters() {
        return enemyDropCounters;
    }

    EnemyDropResolver.CounterState writeEnemyDropCounters(EnemyDropResolver.CounterState value) {
        return enemyDropCounters = value;
    }

    int readEnemyDropMaxHearts() {
        return enemyDropMaxHearts;
    }

    int writeEnemyDropMaxHearts(int value) {
        return enemyDropMaxHearts = value;
    }

    int readEnemyDropHealth() {
        return enemyDropHealth;
    }

    int writeEnemyDropHealth(int value) {
        return enemyDropHealth = value;
    }

    Boolean readEnemyDropLowHealth() {
        return enemyDropLowHealth;
    }

    Boolean writeEnemyDropLowHealth(Boolean value) {
        return enemyDropLowHealth = value;
    }

    boolean readEnemyDropActivePowerUp() {
        return enemyDropActivePowerUp;
    }

    boolean writeEnemyDropActivePowerUp(boolean value) {
        return enemyDropActivePowerUp = value;
    }

    boolean readInBossBattle() {
        return inBossBattle;
    }

    boolean writeInBossBattle(boolean value) {
        return inBossBattle = value;
    }

    int readKillCount() {
        return killCount;
    }

    int writeKillCount(int value) {
        return killCount = value;
    }

    int readKillOrder(int slot) {
        return killOrder[slot];
    }

    int writeKillOrder(int slot, int value) {
        return killOrder[slot] = value;
    }

    int[] storageKillOrder() {
        return killOrder;
    }
}
