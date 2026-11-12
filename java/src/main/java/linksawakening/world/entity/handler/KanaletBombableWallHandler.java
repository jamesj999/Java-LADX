package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_HIDING_SLIME_KEY;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_KANALET_BOMBABLE_WALL;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for KanaletBombableWall. */
final class KanaletBombableWallHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_KANALET_BOMBABLE_WALL,
                EntityTickStage.PHASE_1_AFTER_SPAWN_GUARD,
                0,
                KanaletBombableWallHandler::phase1KanaletBombableWall);
    }

    private static boolean phase1KanaletBombableWall(EntityFrameExecution state) {
        if ((state.roomInspection().entityRoomStatus() & 0x10) != 0) {
            state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
            return false;
        }
        state.lifecycle().setDroppedItemBySlot(state.entity.slot(), ENTITY_HIDING_SLIME_KEY);
        if (state.slot().ignoreHits(state.entity.slot()) != 0) {
            if (state.enemySpawns().spawnKanaletWallMoblin(state.entity)) {
                state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
                return false;
            }
        }

        return true;
    }
}
