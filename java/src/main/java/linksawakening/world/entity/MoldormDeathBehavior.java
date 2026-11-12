package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.MoldormSegmentPosition;
import linksawakening.world.entity.motion.*;

/** Focused behavior using the state owned by EntityBossRewards. */
final class MoldormDeathBehavior {
    static void advanceMoldormDestruction(EntityBossRewards owner, RoomEntity source, int frameCounter) {
        var runtime = owner.runtime;
        int slot = source.slot();
        int tailState =
                runtime.motions().get(MoldormMotion.class, MoldormMotion::new).destructionTailState(slot);
        if (runtime.spriteHandlers() != null) {
            RoomEntity rendered = withDefinition(
                    source,
                    runtime.spriteHandlers()
                            .forMoldormState(
                                    runtime.motions()
                                            .get(MoldormMotion.class, MoldormMotion::new)
                                            .headSpriteVariant(slot),
                                    source.x(),
                                    (source.y() - source.z()) & 0xFF,
                                    runtime.motions()
                                            .get(MoldormMotion.class, MoldormMotion::new)
                                            .currentTailSegments(slot),
                                    tailState,
                                    frameCounter),
                    0);
            runtime.slots()[slot] = withFlipAttribute(
                    rendered,
                    runtime.physicsTables().readBaseEntityFlipAttribute(slot)
                            ^ ((runtime.combatTables().readEnemyFlashCountdown(slot) << 2) & 0x10));
        }

        switch (owner.readBossDeathProducerState(slot)) {
            case 0 -> {
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x60);
                runtime.combatTables().writeEnemyFlashCountdown(slot, 0xFF);
                owner.writeBossDeathProducerState(slot, 1);
            }
            case 1 -> {
                if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
                    runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xFF);
                    runtime.combatTables().writeEnemyFlashCountdown(slot, 0xFF);
                    owner.writeBossDeathProducerState(slot, 2);
                }
            }
            case 2 -> {
                if ((runtime.physicsTables().readEnemyTransitionCountdown(slot) & 0x1F) != 0) {
                    return;
                }
                if (tailState == 4) {
                    runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x30);
                    owner.writeBossDeathProducerState(slot, 3);
                    return;
                }
                MoldormSegmentPosition poofPosition = runtime.motions()
                        .get(MoldormMotion.class, MoldormMotion::new)
                        .currentTailSegments(slot)
                        .get(3 - tailState);
                owner.createMoldormPoof(source, poofPosition.x(), poofPosition.y());
                runtime.motions()
                        .get(MoldormMotion.class, MoldormMotion::new)
                        .setDestructionTailState(slot, tailState + 1);
            }
            default -> owner.advanceMoldormBossExplosion(source);
        }
    }

    static void advanceMoldormBossExplosion(EntityBossRewards owner, RoomEntity source) {
        var runtime = owner.runtime;
        int slot = source.slot();
        int countdown = runtime.physicsTables().readEnemyTransitionCountdown(slot);
        if (countdown == 0) {
            owner.spawnMoldormHeartContainer(source);
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
        owner.createMoldormPoof(
                source, (source.x() + xOffsets[index]) & 0xFF, ((source.y() - source.z()) + yOffsets[index]) & 0xFF);
        if (countdown == 0x10) {
            runtime.motions().get(MoldormMotion.class, MoldormMotion::new).setDestructionTailState(slot, 5);
        }
    }

    static void spawnMoldormHeartContainer(EntityBossRewards owner, RoomEntity source) {
        var runtime = owner.runtime;
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
        runtime.motions()
                .get(EnemyDropMotion.class, EnemyDropMotion::new)
                .initialize(freeSlot, runtime.physicsTables().readGroundInteractionSideScrolling(), 0x10);
        owner.writeEnemyDropActive(freeSlot, true);
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
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }
}
