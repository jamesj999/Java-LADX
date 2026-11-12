package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;

/** Per-invocation scratch for BossKeyDropBehavior.advanceBossKeyDropProducer; values are assigned in source order. */
final class BossKeyDropStep {
    private linksawakening.world.entity.EntityBossRewards owner;
    private linksawakening.world.entity.RoomEntity source;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.EntityBossRewardsServices runtime;
    private int slot;
    private int state;

    BossKeyDropStep(
            linksawakening.world.entity.EntityBossRewards owner,
            linksawakening.world.entity.RoomEntity source,
            java.util.function.IntSupplier randomByteSupplier) {
        this.owner = owner;
        this.source = source;
        this.randomByteSupplier = randomByteSupplier;
    }

    void run() {
        runtime = owner.runtime;
        slot = source.slot();
        if (runtime.physicsTables().readEnemyTransitionCountdown(slot) != 0) {
            return;
        }

        state = owner.readBossDeathProducerState(slot);
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
        owner.writeBossDeathProducerState(slot, 1);
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x60);
        return false;
    }

    private boolean advanceState1() {
        // func_006_563A: transition $CF, then private state 2++.
        owner.writeBossDeathProducerState(slot, 2);
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xCF);
        return false;
    }

    private boolean advanceDefault() {
        // func_006_564B: variant $02, z from hMultiPurpose3,
        // speedZ $10, and private countdown 1 $10.
        runtime.pickups().spawnBossKeyDrop(source, 0x02, source.z(), 0x10, 0x10);
        runtime.lifecycle().disableEntityWithoutPersistence(slot);
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));

        return true;
    }

    private boolean advanceState02() {
        // ArmosKnightPrivateState0Handler: transition $A0 and
        // the first full flash countdown.
        owner.writeBossDeathProducerState(slot, 1);
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xA0);
        runtime.combatTables().writeEnemyFlashCountdown(slot, 0xFF);
        return false;
    }

    private boolean advanceState12() {
        // ArmosKnightPrivateState1Handler: the second flash
        // begins only after the first countdown reaches zero.
        owner.writeBossDeathProducerState(slot, 2);
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xC0);
        runtime.combatTables().writeEnemyFlashCountdown(slot, 0xFF);
        return false;
    }

    private boolean advanceDefault2() {
        // The shared inactive handler emits the boss explosion,
        // then calls DidKillEnemy. Armos has pre-forced the key;
        // Hinox uses the ordinary drop resolver here.
        if (source.type() == EntityRomConstants.ENTITY_ARMOS_KNIGHT) {
            int killIndex = owner.readKillCount() & 0xFF;
            owner.writeKillOrder(killIndex, source.sourceLoadOrder() & 0xFF);
            owner.writeKillCount((owner.readKillCount() + 1) & 0xFF);
            runtime.roomVariables()
                    .writePendingClearedEntityMask(
                            runtime.roomVariables().readPendingClearedEntityMask() | (persistentClearMask(source)));
            runtime.pickups().spawnEnemyDrop(source, EntityRomConstants.ENTITY_KEY_DROP_POINT);
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
        } else {
            owner.handleTerminalEnemyDeath(source, randomByteSupplier);
            // label_006_5355 -> label_006_6CC9 opens the Hinox
            // room event except in Eagle's Tower and later maps.
            if (runtime.roomVariables().readEntityMapId() >= 0
                    && runtime.roomVariables().readEntityMapId() < 0x06) {
                runtime.roomVariables()
                        .writePendingRoomStatusMask(runtime.roomVariables().readPendingRoomStatusMask() | (0x20));
            }
        }
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x1A));

        return true;
    }

    private boolean advanceState03() {
        // func_007_7EB6: transition $A0, then private state 2++.
        owner.writeBossDeathProducerState(slot, 1);
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xA0);

        return true;
    }

    private boolean advanceState13() {
        // func_007_7EC7: transition $C0, then private state 2++.
        owner.writeBossDeathProducerState(slot, 2);
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xC0);

        return true;
    }

    private boolean advanceDefault3() {
        // func_007_7ED6: ConfigureNewEntity leaves z at zero;
        // the producer supplies speedZ $18 and countdown 1 $20.
        runtime.pickups().spawnBossKeyDrop(source, 0x00, 0x00, 0x18, 0x20);
        runtime.lifecycle().disableEntityWithoutPersistence(slot);
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x1A));

        return true;
    }
}
