package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.function.IntSupplier;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;

/** Focused behavior using the state owned by EntityBossRewards. */
final class BossKeyDropBehavior {
    static void advanceBossKeyDropProducer(EntityBossRewards owner, RoomEntity source, IntSupplier randomByteSupplier) {
        var runtime = owner.runtime;
        int slot = source.slot();
        if (runtime.physicsTables().readEnemyTransitionCountdown(slot) != 0) {
            return;
        }

        int state = owner.readBossDeathProducerState(slot);
        if (source.type() == EntityRomConstants.ENTITY_DESERT_LANMOLA) {
            switch (state) {
                case 0 -> {
                    // func_006_5629: transition $60, then private state 2++.
                    owner.writeBossDeathProducerState(slot, 1);
                    runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x60);
                    return;
                }
                case 1 -> {
                    // func_006_563A: transition $CF, then private state 2++.
                    owner.writeBossDeathProducerState(slot, 2);
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
                    owner.writeBossDeathProducerState(slot, 1);
                    runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xA0);
                    runtime.combatTables().writeEnemyFlashCountdown(slot, 0xFF);
                    return;
                }
                case 1 -> {
                    // ArmosKnightPrivateState1Handler: the second flash
                    // begins only after the first countdown reaches zero.
                    owner.writeBossDeathProducerState(slot, 2);
                    runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xC0);
                    runtime.combatTables().writeEnemyFlashCountdown(slot, 0xFF);
                    return;
                }
                default -> {
                    // The shared inactive handler emits the boss explosion,
                    // then calls DidKillEnemy. Armos has pre-forced the key;
                    // Hinox uses the ordinary drop resolver here.
                    if (source.type() == EntityRomConstants.ENTITY_ARMOS_KNIGHT) {
                        int killIndex = owner.readKillCount() & 0xFF;
                        owner.writeKillOrder(killIndex, source.sourceLoadOrder() & 0xFF);
                        owner.writeKillCount((owner.readKillCount() + 1) & 0xFF);
                        runtime.roomVariables()
                                .writePendingClearedEntityMask(
                                        runtime.roomVariables().readPendingClearedEntityMask()
                                                | (persistentClearMask(source)));
                        runtime.pickups().spawnEnemyDrop(source, EntityRomConstants.ENTITY_KEY_DROP_POINT);
                        runtime.lifecycle().disableEntityWithoutPersistence(slot);
                    } else {
                        owner.handleTerminalEnemyDeath(source, randomByteSupplier);
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
                owner.writeBossDeathProducerState(slot, 1);
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xA0);
            }
            case 1 -> {
                // func_007_7EC7: transition $C0, then private state 2++.
                owner.writeBossDeathProducerState(slot, 2);
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
}
