package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_HIDING_SLIME_KEY;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_KANALET_BOMBABLE_WALL;

import linksawakening.world.entity.EntityEnemySpawns;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityRoomInspection;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntitySlotLifecycle;
import linksawakening.world.entity.EntitySlotView;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for KanaletBombableWall. */
final class KanaletBombableWallHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                ENTITY_KANALET_BOMBABLE_WALL,
                EntityTickStage.PHASE_1_AFTER_SPAWN_GUARD,
                0,
                frame -> phase1KanaletBombableWall(
                        frame.slotFrame(),
                        frame.roomInspection(),
                        frame.lifecycle(),
                        frame.slot(),
                        frame.enemySpawns()));
    }

    private static boolean phase1KanaletBombableWall(
            EntitySlotExecution slotState,
            EntityRoomInspection roomInspection,
            EntitySlotLifecycle lifecycle,
            EntitySlotView slot,
            EntityEnemySpawns enemySpawns) {
        if ((roomInspection.entityRoomStatus() & 0x10) != 0) {
            lifecycle.disableEntityWithoutPersistence(slotState.entity().slot());
            return false;
        }
        lifecycle.setDroppedItemBySlot(slotState.entity().slot(), ENTITY_HIDING_SLIME_KEY);
        if (slot.ignoreHits(slotState.entity().slot()) != 0) {
            if (enemySpawns.spawnKanaletWallMoblin(slotState.entity())) {
                lifecycle.disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
        }

        return true;
    }
}
