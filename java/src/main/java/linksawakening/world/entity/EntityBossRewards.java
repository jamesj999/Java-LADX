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
    private EnemyDropResolver.Result resolveDrop(EnemyDropResolver.Context context) {
        return runtime.entityState.enemyDropLowHealth == null
                ? runtime.entityState.enemyDropResolver.resolve(context)
                : runtime.entityState.enemyDropResolver.resolve(context, runtime.entityState.enemyDropLowHealth);
    }

    private final RoomEntityRuntime runtime;

    EntityBossRewards(RoomEntityRuntime runtime) {
        this.runtime = runtime;
    }

    /** ApplySwordDamagesToEnemy, $03:7361: preserve descending RNG order. */
    void cascadeGhiniDeath(RoomEntity killed, IntSupplier random) {
        if (killed.type() != 0x12) return;
        int[] drops = {0x2D, 0x2E, 0x38, 0x37};
        for (int slot = RoomEntity.SLOT_COUNT - 1; slot >= 0; slot--) {
            RoomEntity other = runtime.slots[slot];
            if (slot == killed.slot()
                    || other == null
                    || (other.type() != 0x10 && other.type() != 0x11)
                    || runtime.entityState.ghiniMotion.state(slot) != 0
                    || other.status() == EntityStatus.DISABLED) continue;
            runtime.slots[slot] = withStatus(other, EntityStatus.DYING);
            runtime.entityState.dyingCountdown[slot] = 0x1F;
            runtime.entityState.droppedItemBySlot[slot] = drops[random.getAsInt() & 3];
        }
        runtime.entityState.droppedItemBySlot[killed.slot()] = 0x2E;
    }

    void handleTerminalEnemyDeath(RoomEntity entity, IntSupplier randomByteSupplier) {
        boolean swallowedShield = entity.type() == EntityRomConstants.ENTITY_LIKE_LIKE
                && runtime.entityState.likeLikeMotion.privateState1(entity.slot()) != 0;
        if (entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING) {
            // The mini-boss option reaches DidKillEnemy's room-event branch,
            // permanently opening the east rescue-room path.
            runtime.entityState.pendingRoomStatusMask |= 0x20;
        }
        if (entity.sourceLoadOrder() < 0 || (runtime.entityState.enemyDropResolver == null && !swallowedShield)) {
            if (entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING) {
                runtime.entityState.pendingClearedEntityMask |= persistentClearMask(entity);
            }
            runtime.lifecycle.disableEntityWithoutPersistence(entity.slot());
            return;
        }

        EnemyDropResolver.Result result;
        if (swallowedShield) {
            result = new EnemyDropResolver.Result(
                    EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP, runtime.entityState.enemyDropCounters, 0);
        } else {
            EnemyDropResolver.Context context = new EnemyDropResolver.Context(
                    entity.type(),
                    runtime.roomInspection.enemyDropHealthGroup(entity.slot()),
                    runtime.entityState.droppedItemBySlot[entity.slot()],
                    runtime.entityState.enemyDropMaxHearts,
                    runtime.entityState.enemyDropHealth,
                    runtime.entityState.inBossBattle,
                    runtime.entityState.enemyDropActivePowerUp,
                    runtime.entityState.groundInteractionSideScrolling,
                    runtime.entityState.enemyDropCounters,
                    randomByteSupplier);
            result = resolveDrop(context);
            runtime.entityState.enemyDropCounters = result.counters();
        }

        int killIndex = runtime.entityState.killCount & 0xFF;
        runtime.entityState.killOrder[killIndex] = entity.sourceLoadOrder() & 0xFF;
        runtime.entityState.killCount = (runtime.entityState.killCount + 1) & 0xFF;
        runtime.entityState.pendingClearedEntityMask |= persistentClearMask(entity);
        if (result.dropped()) {
            runtime.pickups.spawnEnemyDrop(entity, result.itemType());
        }
        runtime.lifecycle.disableEntityWithoutPersistence(entity.slot());
    }

    public void advanceRollingBonesDestruction(RoomEntity entity, IntSupplier randomByteSupplier) {
        int slot = entity.slot();
        if (!runtime.entityState.rollingBonesDeathInitialized[slot]) {
            runtime.entityState.rollingBonesDeathInitialized[slot] = true;
            runtime.entityState.enemyTransitionCountdown[slot] = 0xFF;
            runtime.entityState.enemyFlashCountdown[slot] = 0xFF;
            return;
        }
        int countdown = runtime.entityState.enemyTransitionCountdown[slot];
        runtime.entityState.enemyFlashCountdown[slot] = countdown;
        if (countdown == 0) {
            // $06:6CC1 calls DidKillEnemy, including SpawnEnemyDrop. The
            // ROM health-group tables select the miniboss healing fairy.
            if (runtime.entityState.enemyDropResolver != null) {
                EnemyDropResolver.Result drop = resolveDrop(new EnemyDropResolver.Context(
                        entity.type(),
                        runtime.roomInspection.enemyDropHealthGroup(entity.slot()),
                        runtime.entityState.droppedItemBySlot[slot],
                        runtime.entityState.enemyDropMaxHearts,
                        runtime.entityState.enemyDropHealth,
                        runtime.entityState.inBossBattle,
                        runtime.entityState.enemyDropActivePowerUp,
                        runtime.entityState.groundInteractionSideScrolling,
                        runtime.entityState.enemyDropCounters,
                        randomByteSupplier));
                runtime.entityState.enemyDropCounters = drop.counters();
                if (drop.dropped()) runtime.pickups.spawnEnemyDrop(entity, drop.itemType());
            }
            int killIndex = runtime.entityState.killCount & 0xFF;
            runtime.entityState.killOrder[killIndex] = entity.sourceLoadOrder() & 0xFF;
            runtime.entityState.killCount = (runtime.entityState.killCount + 1) & 0xFF;
            runtime.entityState.pendingClearedEntityMask |= persistentClearMask(entity);
            if (runtime.entityState.entityMapId != 0x07) {
                runtime.entityState.pendingRoomStatusMask |= 0x20;
            }
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
            return;
        }
        if (countdown < 0x80 && (countdown & 0x07) == 0) {
            int x = (entity.x() + ((randomByteSupplier.getAsInt() & 0x1F) - 0x10)) & 0xFF;
            int y = (entity.y() - entity.z() + ((randomByteSupplier.getAsInt() & 0x1F) - 0x14)) & 0xFF;
            runtime.entityState.events.transientVfxRequests.add(new TransientVfxRequest(TransientVfxType.POOF, x, y));
            runtime.entityState.events.pendingEntityEvents.add(
                    new EntityCombatEvent(slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
        }
    }

    public void advanceMoblinKingDestruction(RoomEntity source, int frame, IntSupplier random) {
        int slot = source.slot();
        // Main resolves sword damage before dispatch; the ROM dispatches the
        // dying branch on the following frame, after this hit's active handler.
        boolean hitThisFrame = runtime.entityState.moblinKingLethalHitFrame[slot] == frame + 1;
        runtime.entityState.moblinKingLethalHitFrame[slot] = 0;
        if (hitThisFrame) return;
        if (runtime.spriteHandlers != null)
            source = withDefinition(source, runtime.spriteHandlers.forMoblinKingState(12, -1, source.z() != 0), 0);
        runtime.slots[slot] = withFlipAttribute(
                source,
                runtime.entityState.baseEntityFlipAttribute[slot]
                        ^ ((runtime.entityState.enemyFlashCountdown[slot] << 2) & 0x10));
        if (runtime.entityState.bossDeathProducerState[slot] == 0) {
            runtime.entityState.enemyTransitionCountdown[slot] = 0xA0;
            runtime.entityState.enemyFlashCountdown[slot] = 0xFF;
            runtime.entityState.bossDeathProducerState[slot] = 1;
        } else if (runtime.entityState.bossDeathProducerState[slot] == 1) {
            if (runtime.entityState.enemyTransitionCountdown[slot] == 0) {
                runtime.entityState.enemyTransitionCountdown[slot] = 0xC0;
                runtime.entityState.enemyFlashCountdown[slot] = 0xFF;
                runtime.entityState.bossDeathProducerState[slot] = 2;
            }
        } else if (runtime.entityState.enemyTransitionCountdown[slot] == 0) {
            runtime.entityState.events.pendingEntityEvents.add(
                    new EntityCombatEvent(slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x0C));
            runtime.entityState.swordMoblinAlertingSoundCounter = 4;
            handleTerminalEnemyDeath(source, random);
        } else if ((runtime.entityState.enemyTransitionCountdown[slot] & 7) == 0
                && runtime.backgroundMechanics.entityHandlerInteractive(false)) {
            int x = (source.x() + (random.getAsInt() & 0x1F) - 0x10) & 255;
            int y = (source.y() - source.z() + (random.getAsInt() & 0x1F) - 0x14) & 255;
            runtime.entityState.events.transientVfxRequests.add(new TransientVfxRequest(TransientVfxType.POOF, x, y));
            runtime.entityState.events.pendingEntityEvents.add(
                    new EntityCombatEvent(slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
        }
    }

    public void advanceMoldormDestruction(RoomEntity source, int frameCounter) {
        int slot = source.slot();
        int tailState = runtime.entityState.moldormDestructionTailState[slot];
        if (runtime.spriteHandlers != null) {
            RoomEntity rendered = withDefinition(
                    source,
                    runtime.spriteHandlers.forMoldormState(
                            runtime.entityState.moldormMotion.headSpriteVariant(slot),
                            source.x(),
                            (source.y() - source.z()) & 0xFF,
                            runtime.entityState.moldormMotion.currentTailSegments(slot),
                            tailState,
                            frameCounter),
                    0);
            runtime.slots[slot] = withFlipAttribute(
                    rendered,
                    runtime.entityState.baseEntityFlipAttribute[slot]
                            ^ ((runtime.entityState.enemyFlashCountdown[slot] << 2) & 0x10));
        }

        switch (runtime.entityState.bossDeathProducerState[slot]) {
            case 0 -> {
                runtime.entityState.enemyTransitionCountdown[slot] = 0x60;
                runtime.entityState.enemyFlashCountdown[slot] = 0xFF;
                runtime.entityState.bossDeathProducerState[slot] = 1;
            }
            case 1 -> {
                if (runtime.entityState.enemyTransitionCountdown[slot] == 0) {
                    runtime.entityState.enemyTransitionCountdown[slot] = 0xFF;
                    runtime.entityState.enemyFlashCountdown[slot] = 0xFF;
                    runtime.entityState.bossDeathProducerState[slot] = 2;
                }
            }
            case 2 -> {
                if ((runtime.entityState.enemyTransitionCountdown[slot] & 0x1F) != 0) {
                    return;
                }
                if (tailState == 4) {
                    runtime.entityState.enemyTransitionCountdown[slot] = 0x30;
                    runtime.entityState.bossDeathProducerState[slot] = 3;
                    return;
                }
                MoldormSegmentPosition poofPosition = runtime.entityState
                        .moldormMotion
                        .currentTailSegments(slot)
                        .get(3 - tailState);
                createMoldormPoof(source, poofPosition.x(), poofPosition.y());
                runtime.entityState.moldormDestructionTailState[slot] = tailState + 1;
            }
            default -> advanceMoldormBossExplosion(source);
        }
    }

    void advanceMoldormBossExplosion(RoomEntity source) {
        int slot = source.slot();
        int countdown = runtime.entityState.enemyTransitionCountdown[slot];
        if (countdown == 0) {
            spawnMoldormHeartContainer(source);
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
            runtime.entityState.events.pendingEntityEvents.add(
                    new EntityCombatEvent(slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x1A));
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
            runtime.entityState.moldormDestructionTailState[slot] = 5;
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
        runtime.initializeHealthGroup(freeSlot);
        runtime.pickups.resetEnemyDropState(freeSlot);
        runtime.entityState.enemyDropMotion.initialize(
                freeSlot, runtime.entityState.groundInteractionSideScrolling, 0x10);
        runtime.entityState.enemyDropActive[freeSlot] = true;
        runtime.entityState.slowTransitionCountdown[freeSlot] = 0;
        runtime.entityState.slowTimerInitialized[freeSlot] = false;
        runtime.entityState.enemyTransitionCountdown[freeSlot] = 0;
        runtime.entityState.enemyStunnedCountdown[freeSlot] = 0;
        runtime.entityState.dyingCountdown[freeSlot] = 0;
        runtime.entityState.powerRecoilDeath[freeSlot] = false;
        runtime.entityState.enemyPhysicsFlags[freeSlot] =
                initialPhysicsFlags(EntityRomConstants.ENTITY_HEART_CONTAINER);
        runtime.entityState.enemyHealth[freeSlot] = runtime.initialHealth(EntityRomConstants.ENTITY_HEART_CONTAINER);
        runtime.entityState.enemyFlashCountdown[freeSlot] = 0;
        runtime.entityState.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.entityState.entityGroundStatus[freeSlot] = 0;
        runtime.entityState.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityState.entityOptions1Override[freeSlot] = -1;
        runtime.entityState.enemyRecoilMotion.clear(freeSlot);
        runtime.entityState.dynamicEntitySpawnedThisFrame[freeSlot] = true;
    }

    void createMoldormPoof(RoomEntity source, int x, int y) {
        runtime.entityState.events.transientVfxRequests.add(new TransientVfxRequest(TransientVfxType.POOF, x, y));
        runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                source.slot(), source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
    }

    public void advanceCollectedHeartContainer(RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ) {
        int slot = entity.slot();
        if (runtime.entityState.enemyTransitionCountdown[slot] == 1) {
            runtime.entityState.events.pendingHeartContainerRewards.add(new HeartContainerRewardEvent(slot));
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
            return;
        }
        runtime.entityState.events.pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
        RoomEntity held =
                withPositionAndVariant(entity, linkEntityX, (linkEntityY - 0x0C) & 0xFF, entity.spriteVariant());
        runtime.slots[slot] = withZ(held, linkZ & 0xFF);
    }

    void advanceBossKeyDropProducer(RoomEntity source, IntSupplier randomByteSupplier) {
        int slot = source.slot();
        if (runtime.entityState.enemyTransitionCountdown[slot] != 0) {
            return;
        }

        int state = runtime.entityState.bossDeathProducerState[slot];
        if (source.type() == EntityRomConstants.ENTITY_DESERT_LANMOLA) {
            switch (state) {
                case 0 -> {
                    // func_006_5629: transition $60, then private state 2++.
                    runtime.entityState.bossDeathProducerState[slot] = 1;
                    runtime.entityState.enemyTransitionCountdown[slot] = 0x60;
                    return;
                }
                case 1 -> {
                    // func_006_563A: transition $CF, then private state 2++.
                    runtime.entityState.bossDeathProducerState[slot] = 2;
                    runtime.entityState.enemyTransitionCountdown[slot] = 0xCF;
                    return;
                }
                default -> {
                    // func_006_564B: variant $02, z from hMultiPurpose3,
                    // speedZ $10, and private countdown 1 $10.
                    runtime.pickups.spawnBossKeyDrop(source, 0x02, source.z(), 0x10, 0x10);
                    runtime.lifecycle.disableEntityWithoutPersistence(slot);
                    runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
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
                    runtime.entityState.bossDeathProducerState[slot] = 1;
                    runtime.entityState.enemyTransitionCountdown[slot] = 0xA0;
                    runtime.entityState.enemyFlashCountdown[slot] = 0xFF;
                    return;
                }
                case 1 -> {
                    // ArmosKnightPrivateState1Handler: the second flash
                    // begins only after the first countdown reaches zero.
                    runtime.entityState.bossDeathProducerState[slot] = 2;
                    runtime.entityState.enemyTransitionCountdown[slot] = 0xC0;
                    runtime.entityState.enemyFlashCountdown[slot] = 0xFF;
                    return;
                }
                default -> {
                    // The shared inactive handler emits the boss explosion,
                    // then calls DidKillEnemy. Armos has pre-forced the key;
                    // Hinox uses the ordinary drop resolver here.
                    if (source.type() == EntityRomConstants.ENTITY_ARMOS_KNIGHT) {
                        int killIndex = runtime.entityState.killCount & 0xFF;
                        runtime.entityState.killOrder[killIndex] = source.sourceLoadOrder() & 0xFF;
                        runtime.entityState.killCount = (runtime.entityState.killCount + 1) & 0xFF;
                        runtime.entityState.pendingClearedEntityMask |= persistentClearMask(source);
                        runtime.pickups.spawnEnemyDrop(source, EntityRomConstants.ENTITY_KEY_DROP_POINT);
                        runtime.lifecycle.disableEntityWithoutPersistence(slot);
                    } else {
                        handleTerminalEnemyDeath(source, randomByteSupplier);
                        // label_006_5355 -> label_006_6CC9 opens the Hinox
                        // room event except in Eagle's Tower and later maps.
                        if (runtime.entityState.entityMapId >= 0 && runtime.entityState.entityMapId < 0x06) {
                            runtime.entityState.pendingRoomStatusMask |= 0x20;
                        }
                    }
                    runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                            slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x1A));
                }
            }
            return;
        }

        switch (state) {
            case 0 -> {
                // func_007_7EB6: transition $A0, then private state 2++.
                runtime.entityState.bossDeathProducerState[slot] = 1;
                runtime.entityState.enemyTransitionCountdown[slot] = 0xA0;
            }
            case 1 -> {
                // func_007_7EC7: transition $C0, then private state 2++.
                runtime.entityState.bossDeathProducerState[slot] = 2;
                runtime.entityState.enemyTransitionCountdown[slot] = 0xC0;
            }
            default -> {
                // func_007_7ED6: ConfigureNewEntity leaves z at zero;
                // the producer supplies speedZ $18 and countdown 1 $20.
                runtime.pickups.spawnBossKeyDrop(source, 0x00, 0x00, 0x18, 0x20);
                runtime.lifecycle.disableEntityWithoutPersistence(slot);
                runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                        slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x1A));
            }
        }
    }
}
