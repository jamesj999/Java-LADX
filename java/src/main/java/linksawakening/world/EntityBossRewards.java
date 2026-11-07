package linksawakening.world;

import static linksawakening.world.EntityMotionMath.*;
import static linksawakening.world.EntitySnapshots.*;
import static linksawakening.world.EntityTypeRules.*;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import static linksawakening.world.RoomEntityRuntime.*;

/** Boss destruction and source-shaped reward production. */
final class EntityBossRewards {
    private final RoomEntityRuntime runtime;
    EntityBossRewards(RoomEntityRuntime runtime) { this.runtime = runtime; }

    void handleTerminalEnemyDeath(RoomEntity entity, IntSupplier randomByteSupplier) {
        boolean swallowedShield = entity.type() == runtime.ENTITY_LIKE_LIKE
            && runtime.likeLikeMotion.privateState1(entity.slot()) != 0;
        if (entity.type() == runtime.ENTITY_MOBLIN_KING) {
            // The mini-boss option reaches DidKillEnemy's room-event branch,
            // permanently opening the east rescue-room path.
            runtime.pendingRoomStatusMask |= 0x20;
        }
        if (entity.sourceLoadOrder() < 0 || (runtime.enemyDropResolver == null && !swallowedShield)) {
            if (entity.type() == runtime.ENTITY_MOBLIN_KING) {
                runtime.pendingClearedEntityMask |= persistentClearMask(entity);
            }
            runtime.lifecycle.disableEntityWithoutPersistence(entity.slot());
            return;
        }

        EnemyDropResolver.Result result;
        if (swallowedShield) {
            result = new EnemyDropResolver.Result(
                runtime.ENTITY_SWORD_SHIELD_PICKUP, runtime.enemyDropCounters, 0);
        } else {
            EnemyDropResolver.Context context = new EnemyDropResolver.Context(
                entity.type(), runtime.roomInspection.enemyDropHealthGroup(entity.type()), runtime.droppedItemBySlot[entity.slot()],
                runtime.enemyDropMaxHearts, runtime.enemyDropHealth, runtime.enemyDropBossBattle,
                runtime.enemyDropActivePowerUp, runtime.groundInteractionSideScrolling, runtime.enemyDropCounters,
                randomByteSupplier);
            result = runtime.enemyDropResolver.resolve(context);
            runtime.enemyDropCounters = result.counters();
        }

        int killIndex = runtime.killCount & 0xFF;
        runtime.killOrder[killIndex] = entity.sourceLoadOrder() & 0xFF;
        runtime.killCount = (runtime.killCount + 1) & 0xFF;
        runtime.pendingClearedEntityMask |= persistentClearMask(entity);
        if (result.dropped()) {
            runtime.pickups.spawnEnemyDrop(entity, result.itemType());
        }
        runtime.lifecycle.disableEntityWithoutPersistence(entity.slot());
    }

    void advanceRollingBonesDestruction(RoomEntity entity,
                                                 IntSupplier randomByteSupplier) {
        int slot = entity.slot();
        if (!runtime.rollingBonesDeathInitialized[slot]) {
            runtime.rollingBonesDeathInitialized[slot] = true;
            runtime.enemyTransitionCountdown[slot] = 0xFF;
            runtime.enemyFlashCountdown[slot] = 0xFF;
            return;
        }
        int countdown = runtime.enemyTransitionCountdown[slot];
        runtime.enemyFlashCountdown[slot] = countdown;
        if (countdown == 0) {
            // $06:6CC1 calls DidKillEnemy, including SpawnEnemyDrop. The
            // ROM health-group tables select the miniboss healing fairy.
            if (runtime.enemyDropResolver != null) {
                EnemyDropResolver.Result drop = runtime.enemyDropResolver.resolve(
                    new EnemyDropResolver.Context(entity.type(), runtime.roomInspection.enemyDropHealthGroup(entity.type()),
                        runtime.droppedItemBySlot[slot], runtime.enemyDropMaxHearts, runtime.enemyDropHealth,
                        runtime.enemyDropBossBattle, runtime.enemyDropActivePowerUp, runtime.groundInteractionSideScrolling,
                        runtime.enemyDropCounters, randomByteSupplier));
                runtime.enemyDropCounters = drop.counters();
                if (drop.dropped()) runtime.pickups.spawnEnemyDrop(entity, drop.itemType());
            }
            int killIndex = runtime.killCount & 0xFF;
            runtime.killOrder[killIndex] = entity.sourceLoadOrder() & 0xFF;
            runtime.killCount = (runtime.killCount + 1) & 0xFF;
            runtime.pendingClearedEntityMask |= persistentClearMask(entity);
            if (runtime.entityMapId != 0x07) {
                runtime.pendingRoomStatusMask |= 0x20;
            }
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
            return;
        }
        if (countdown < 0x80 && (countdown & 0x07) == 0) {
            int x = (entity.x() + ((randomByteSupplier.getAsInt() & 0x1F) - 0x10)) & 0xFF;
            int y = (entity.y() - entity.z()
                + ((randomByteSupplier.getAsInt() & 0x1F) - 0x14)) & 0xFF;
            runtime.transientVfxRequests.add(new TransientVfxRequest(TransientVfxType.POOF, x, y));
            runtime.pendingEntityEvents.add(new EntityCombatEvent(
                slot, entity.type(), 0, false,
                EntityCombatEvent.SoundChannel.NOISE, 0x13));
        }
    }

    void advanceMoblinKingDestruction(RoomEntity source, int frame, IntSupplier random) {
        int slot = source.slot();
        // Main resolves sword damage before dispatch; the ROM dispatches the
        // dying branch on the following frame, after this hit's active handler.
        boolean hitThisFrame = runtime.moblinKingLethalHitFrame[slot] == frame + 1;
        runtime.moblinKingLethalHitFrame[slot] = 0;
        if (hitThisFrame) return;
        if (runtime.spriteHandlers != null) source = withDefinition(source,
            runtime.spriteHandlers.forMoblinKingState(12, -1, source.z() != 0), 0);
        runtime.slots[slot] = withFlipAttribute(source, runtime.baseEntityFlipAttribute[slot]
            ^ ((runtime.enemyFlashCountdown[slot] << 2) & 0x10));
        if (runtime.bossDeathProducerState[slot] == 0) {
            runtime.enemyTransitionCountdown[slot] = 0xA0;
            runtime.enemyFlashCountdown[slot] = 0xFF;
            runtime.bossDeathProducerState[slot] = 1;
        } else if (runtime.bossDeathProducerState[slot] == 1) {
            if (runtime.enemyTransitionCountdown[slot] == 0) {
                runtime.enemyTransitionCountdown[slot] = 0xC0;
                runtime.enemyFlashCountdown[slot] = 0xFF;
                runtime.bossDeathProducerState[slot] = 2;
            }
        } else if (runtime.enemyTransitionCountdown[slot] == 0) {
            runtime.pendingEntityEvents.add(new EntityCombatEvent(slot, source.type(), 0, false,
                EntityCombatEvent.SoundChannel.NOISE, 0x0C));
            runtime.swordMoblinAlertingSoundCounter = 4;
            handleTerminalEnemyDeath(source, random);
        } else if ((runtime.enemyTransitionCountdown[slot] & 7) == 0
            && runtime.backgroundMechanics.entityHandlerInteractive(false)) {
            int x = (source.x() + (random.getAsInt() & 0x1F) - 0x10) & 255;
            int y = (source.y() - source.z() + (random.getAsInt() & 0x1F) - 0x14) & 255;
            runtime.transientVfxRequests.add(new TransientVfxRequest(TransientVfxType.POOF, x, y));
            runtime.pendingEntityEvents.add(new EntityCombatEvent(slot, source.type(), 0, false,
                EntityCombatEvent.SoundChannel.NOISE, 0x13));
        }
    }

    void advanceMoldormDestruction(RoomEntity source, int frameCounter) {
        int slot = source.slot();
        int tailState = runtime.moldormDestructionTailState[slot];
        if (runtime.spriteHandlers != null) {
            RoomEntity rendered = withDefinition(source, runtime.spriteHandlers.forMoldormState(
                runtime.moldormMotion.headSpriteVariant(slot), source.x(),
                (source.y() - source.z()) & 0xFF,
                runtime.moldormMotion.currentTailSegments(slot), tailState, frameCounter), 0);
            runtime.slots[slot] = withFlipAttribute(rendered,
                runtime.baseEntityFlipAttribute[slot]
                    ^ ((runtime.enemyFlashCountdown[slot] << 2) & 0x10));
        }

        switch (runtime.bossDeathProducerState[slot]) {
            case 0 -> {
                runtime.enemyTransitionCountdown[slot] = 0x60;
                runtime.enemyFlashCountdown[slot] = 0xFF;
                runtime.bossDeathProducerState[slot] = 1;
            }
            case 1 -> {
                if (runtime.enemyTransitionCountdown[slot] == 0) {
                    runtime.enemyTransitionCountdown[slot] = 0xFF;
                    runtime.enemyFlashCountdown[slot] = 0xFF;
                    runtime.bossDeathProducerState[slot] = 2;
                }
            }
            case 2 -> {
                if ((runtime.enemyTransitionCountdown[slot] & 0x1F) != 0) {
                    return;
                }
                if (tailState == 4) {
                    runtime.enemyTransitionCountdown[slot] = 0x30;
                    runtime.bossDeathProducerState[slot] = 3;
                    return;
                }
                MoldormMotion.TailPosition poofPosition =
                    runtime.moldormMotion.currentTailSegments(slot).get(3 - tailState);
                createMoldormPoof(source, poofPosition.x(), poofPosition.y());
                runtime.moldormDestructionTailState[slot] = tailState + 1;
            }
            default -> advanceMoldormBossExplosion(source);
        }
    }

    void advanceMoldormBossExplosion(RoomEntity source) {
        int slot = source.slot();
        int countdown = runtime.enemyTransitionCountdown[slot];
        if (countdown == 0) {
            spawnMoldormHeartContainer(source);
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
            runtime.pendingEntityEvents.add(new EntityCombatEvent(
                slot, source.type(), 0, false,
                EntityCombatEvent.SoundChannel.NOISE, 0x1A));
            return;
        }
        if ((countdown & 0x03) != 0) {
            return;
        }
        int index = (countdown >> 2) & 0x07;
        int[] xOffsets = {0, 6, 8, 6, 0, -6, -8, -6};
        int[] yOffsets = {-8, -6, 0, 6, 8, 6, 0, -6};
        createMoldormPoof(source,
            (source.x() + xOffsets[index]) & 0xFF,
            ((source.y() - source.z()) + yOffsets[index]) & 0xFF);
        if (countdown == 0x10) {
            runtime.moldormDestructionTailState[slot] = 5;
        }
    }

    void spawnMoldormHeartContainer(RoomEntity source) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }
        int x = Math.max(0x18, Math.min(0x88, source.x()));
        int y = Math.max(0x20, Math.min(0x70, source.y()));
        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_HEART_CONTAINER);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        runtime.slots[freeSlot] = new RoomEntity(freeSlot, -1, runtime.ENTITY_HEART_CONTAINER,
            x, y, EntityStatus.ACTIVE, definition, variant, 0, 0, source.z(), -1, false);
        runtime.pickups.resetEnemyDropState(freeSlot);
        runtime.enemyDropMotion.initialize(freeSlot, runtime.groundInteractionSideScrolling, 0x10);
        runtime.enemyDropActive[freeSlot] = true;
        runtime.slowTransitionCountdown[freeSlot] = 0;
        runtime.slowTimerInitialized[freeSlot] = false;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        runtime.enemyPhysicsFlags[freeSlot] = initialPhysicsFlags(runtime.ENTITY_HEART_CONTAINER);
        runtime.enemyHealth[freeSlot] = runtime.initialHealth(runtime.ENTITY_HEART_CONTAINER);
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.entityGroundStatus[freeSlot] = 0;
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = -1;
        runtime.enemyRecoilMotion.clear(freeSlot);
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
    }

    void createMoldormPoof(RoomEntity source, int x, int y) {
        runtime.transientVfxRequests.add(new TransientVfxRequest(TransientVfxType.POOF, x, y));
        runtime.pendingEntityEvents.add(new EntityCombatEvent(
            source.slot(), source.type(), 0, false,
            EntityCombatEvent.SoundChannel.NOISE, 0x13));
    }

    void advanceCollectedHeartContainer(RoomEntity entity,
                                                 int linkEntityX, int linkEntityY,
                                                 int linkZ) {
        int slot = entity.slot();
        if (runtime.enemyTransitionCountdown[slot] == 1) {
            runtime.pendingHeartContainerRewards.add(new HeartContainerRewardEvent(slot));
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
            return;
        }
        runtime.pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
        RoomEntity held = withPositionAndVariant(entity, linkEntityX,
            (linkEntityY - 0x0C) & 0xFF, entity.spriteVariant());
        runtime.slots[slot] = withZ(held, linkZ & 0xFF);
    }

    void advanceBossKeyDropProducer(RoomEntity source,
                                             IntSupplier randomByteSupplier) {
        int slot = source.slot();
        if (runtime.enemyTransitionCountdown[slot] != 0) {
            return;
        }

        int state = runtime.bossDeathProducerState[slot];
        if (source.type() == runtime.ENTITY_DESERT_LANMOLA) {
            switch (state) {
                case 0 -> {
                    // func_006_5629: transition $60, then private state 2++.
                    runtime.bossDeathProducerState[slot] = 1;
                    runtime.enemyTransitionCountdown[slot] = 0x60;
                    return;
                }
                case 1 -> {
                    // func_006_563A: transition $CF, then private state 2++.
                    runtime.bossDeathProducerState[slot] = 2;
                    runtime.enemyTransitionCountdown[slot] = 0xCF;
                    return;
                }
                default -> {
                    // func_006_564B: variant $02, z from hMultiPurpose3,
                    // speedZ $10, and private countdown 1 $10.
                    runtime.pickups.spawnBossKeyDrop(source, 0x02, source.z(), 0x10, 0x10);
                    runtime.lifecycle.disableEntityWithoutPersistence(slot);
                    runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        slot, source.type(), 0, false,
                        EntityCombatEvent.SoundChannel.NOISE, 0x13));
                }
            }
            return;
        }

        if (source.type() == runtime.ENTITY_ARMOS_KNIGHT || source.type() == runtime.ENTITY_HINOX) {
            switch (state) {
                case 0 -> {
                    // ArmosKnightPrivateState0Handler: transition $A0 and
                    // the first full flash countdown.
                    runtime.bossDeathProducerState[slot] = 1;
                    runtime.enemyTransitionCountdown[slot] = 0xA0;
                    runtime.enemyFlashCountdown[slot] = 0xFF;
                    return;
                }
                case 1 -> {
                    // ArmosKnightPrivateState1Handler: the second flash
                    // begins only after the first countdown reaches zero.
                    runtime.bossDeathProducerState[slot] = 2;
                    runtime.enemyTransitionCountdown[slot] = 0xC0;
                    runtime.enemyFlashCountdown[slot] = 0xFF;
                    return;
                }
                default -> {
                    // The shared inactive handler emits the boss explosion,
                    // then calls DidKillEnemy. Armos has pre-forced the key;
                    // Hinox uses the ordinary drop resolver here.
                    if (source.type() == runtime.ENTITY_ARMOS_KNIGHT) {
                        int killIndex = runtime.killCount & 0xFF;
                        runtime.killOrder[killIndex] = source.sourceLoadOrder() & 0xFF;
                        runtime.killCount = (runtime.killCount + 1) & 0xFF;
                        runtime.pendingClearedEntityMask |= persistentClearMask(source);
                        runtime.pickups.spawnEnemyDrop(source, runtime.ENTITY_KEY_DROP_POINT);
                        runtime.lifecycle.disableEntityWithoutPersistence(slot);
                    } else {
                        handleTerminalEnemyDeath(source, randomByteSupplier);
                        // label_006_5355 -> label_006_6CC9 opens the Hinox
                        // room event except in Eagle's Tower and later maps.
                        if (runtime.entityMapId >= 0 && runtime.entityMapId < 0x06) {
                            runtime.pendingRoomStatusMask |= 0x20;
                        }
                    }
                    runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        slot, source.type(), 0, false,
                        EntityCombatEvent.SoundChannel.NOISE, 0x1A));
                }
            }
            return;
        }

        switch (state) {
            case 0 -> {
                // func_007_7EB6: transition $A0, then private state 2++.
                runtime.bossDeathProducerState[slot] = 1;
                runtime.enemyTransitionCountdown[slot] = 0xA0;
            }
            case 1 -> {
                // func_007_7EC7: transition $C0, then private state 2++.
                runtime.bossDeathProducerState[slot] = 2;
                runtime.enemyTransitionCountdown[slot] = 0xC0;
            }
            default -> {
                // func_007_7ED6: ConfigureNewEntity leaves z at zero;
                // the producer supplies speedZ $18 and countdown 1 $20.
                runtime.pickups.spawnBossKeyDrop(source, 0x00, 0x00, 0x18, 0x20);
                runtime.lifecycle.disableEntityWithoutPersistence(slot);
                runtime.pendingEntityEvents.add(new EntityCombatEvent(
                    slot, source.type(), 0, false,
                    EntityCombatEvent.SoundChannel.NOISE, 0x1A));
            }
        }
    }
}
