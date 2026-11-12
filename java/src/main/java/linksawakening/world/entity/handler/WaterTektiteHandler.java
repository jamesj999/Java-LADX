package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_WATER_TEKTITE;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for WaterTektite. */
final class WaterTektiteHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_WATER_TEKTITE, EntityTickStage.PHASE_3_WATER_TEKTITE, WaterTektiteHandler::phase3WaterTektite);
    }

    private static boolean phase3WaterTektite(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            state.updated = state.waterTektiteMotion()
                    .advance(state.entity, state.frame, state.randomByteSupplier, state.backgroundCollision);
        }
        return true;
    }
}
