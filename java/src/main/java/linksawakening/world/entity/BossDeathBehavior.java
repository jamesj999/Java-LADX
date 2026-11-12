package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.function.IntSupplier;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;

/** Focused behavior using the state owned by EntityBossRewards. */
final class BossDeathBehavior {
    static void handleTerminalEnemyDeath(EntityBossRewards owner, RoomEntity entity, IntSupplier randomByteSupplier) {
        var runtime = owner.runtime;
        boolean swallowedShield = entity.type() == EntityRomConstants.ENTITY_LIKE_LIKE
                && runtime.motions()
                                .get(LikeLikeMotion.class, LikeLikeMotion::new)
                                .privateState1(entity.slot())
                        != 0;
        if (entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING) {
            // The mini-boss option reaches DidKillEnemy's room-event branch,
            // permanently opening the east rescue-room path.
            runtime.roomVariables()
                    .writePendingRoomStatusMask(runtime.roomVariables().readPendingRoomStatusMask() | (0x20));
        }
        if (entity.sourceLoadOrder() < 0 || (owner.readEnemyDropResolver() == null && !swallowedShield)) {
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
                    EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP, owner.readEnemyDropCounters(), 0);
        } else {
            EnemyDropResolver.Context context = new EnemyDropResolver.Context(
                    entity.type(),
                    runtime.roomInspection().enemyDropHealthGroup(entity.slot()),
                    owner.readDroppedItemBySlot(entity.slot()),
                    owner.readEnemyDropMaxHearts(),
                    owner.readEnemyDropHealth(),
                    owner.readInBossBattle(),
                    owner.readEnemyDropActivePowerUp(),
                    runtime.physicsTables().readGroundInteractionSideScrolling(),
                    owner.readEnemyDropCounters(),
                    randomByteSupplier);
            result = owner.resolveDrop(context);
            owner.writeEnemyDropCounters(result.counters());
        }

        int killIndex = owner.readKillCount() & 0xFF;
        owner.writeKillOrder(killIndex, entity.sourceLoadOrder() & 0xFF);
        owner.writeKillCount((owner.readKillCount() + 1) & 0xFF);
        runtime.roomVariables()
                .writePendingClearedEntityMask(
                        runtime.roomVariables().readPendingClearedEntityMask() | (persistentClearMask(entity)));
        if (result.dropped()) {
            runtime.pickups().spawnEnemyDrop(entity, result.itemType());
        }
        runtime.lifecycle().disableEntityWithoutPersistence(entity.slot());
    }

    static void advanceRollingBonesDestruction(
            EntityBossRewards owner, RoomEntity entity, IntSupplier randomByteSupplier) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        if (!runtime.motions()
                .get(RollingBonesMotion.class, RollingBonesMotion::new)
                .deathInitialized(slot)) {
            runtime.motions()
                    .get(RollingBonesMotion.class, RollingBonesMotion::new)
                    .setDeathInitialized(slot, true);
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xFF);
            runtime.combatTables().writeEnemyFlashCountdown(slot, 0xFF);
            return;
        }
        int countdown = runtime.physicsTables().readEnemyTransitionCountdown(slot);
        runtime.combatTables().writeEnemyFlashCountdown(slot, countdown);
        if (countdown == 0) {
            // $06:6CC1 calls DidKillEnemy, including SpawnEnemyDrop. The
            // ROM health-group tables select the miniboss healing fairy.
            if (owner.readEnemyDropResolver() != null) {
                EnemyDropResolver.Result drop = owner.resolveDrop(new EnemyDropResolver.Context(
                        entity.type(),
                        runtime.roomInspection().enemyDropHealthGroup(entity.slot()),
                        owner.readDroppedItemBySlot(slot),
                        owner.readEnemyDropMaxHearts(),
                        owner.readEnemyDropHealth(),
                        owner.readInBossBattle(),
                        owner.readEnemyDropActivePowerUp(),
                        runtime.physicsTables().readGroundInteractionSideScrolling(),
                        owner.readEnemyDropCounters(),
                        randomByteSupplier));
                owner.writeEnemyDropCounters(drop.counters());
                if (drop.dropped()) runtime.pickups().spawnEnemyDrop(entity, drop.itemType());
            }
            int killIndex = owner.readKillCount() & 0xFF;
            owner.writeKillOrder(killIndex, entity.sourceLoadOrder() & 0xFF);
            owner.writeKillCount((owner.readKillCount() + 1) & 0xFF);
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

    static void advanceMoblinKingDestruction(
            EntityBossRewards owner, RoomEntity source, int frame, IntSupplier random) {
        var runtime = owner.runtime;
        int slot = source.slot();
        // Main resolves sword damage before dispatch; the ROM dispatches the
        // dying branch on the following frame, after this hit's active handler.
        boolean hitThisFrame = runtime.motions()
                        .get(MoblinKingMotion.class, MoblinKingMotion::new)
                        .lethalHitFrame(slot)
                == frame + 1;
        runtime.motions().get(MoblinKingMotion.class, MoblinKingMotion::new).setLethalHitFrame(slot, 0);
        if (hitThisFrame) return;
        if (runtime.spriteHandlers() != null)
            source = withDefinition(
                    source, runtime.slotProperties().spriteViews().forMoblinKingState(12, -1, source.z() != 0), 0);
        runtime.slots()[slot] = withFlipAttribute(
                source,
                runtime.physicsTables().readBaseEntityFlipAttribute(slot)
                        ^ ((runtime.combatTables().readEnemyFlashCountdown(slot) << 2) & 0x10));
        if (owner.readBossDeathProducerState(slot) == 0) {
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xA0);
            runtime.combatTables().writeEnemyFlashCountdown(slot, 0xFF);
            owner.writeBossDeathProducerState(slot, 1);
        } else if (owner.readBossDeathProducerState(slot) == 1) {
            if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xC0);
                runtime.combatTables().writeEnemyFlashCountdown(slot, 0xFF);
                owner.writeBossDeathProducerState(slot, 2);
            }
        } else if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot, source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x0C));
            runtime.motions()
                    .get(MoblinSwordMotion.class, MoblinSwordMotion::new)
                    .writeSwordMoblinAlertingSoundCounter(4);
            owner.handleTerminalEnemyDeath(source, random);
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
}
