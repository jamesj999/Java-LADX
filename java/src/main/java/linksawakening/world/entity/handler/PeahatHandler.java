package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_PEAHAT;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for Peahat. */
final class PeahatHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_PEAHAT, EntityTickStage.PHASE_3_AFTER_LASER, 15, PeahatHandler::phase3Peahat);
    }

    private static boolean phase3Peahat(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            state.updated = state.peaHatMotion()
                    .advance(state.entity, state.frame, state.randomByteSupplier, state.backgroundCollision);
        }
        return true;
    }
}
