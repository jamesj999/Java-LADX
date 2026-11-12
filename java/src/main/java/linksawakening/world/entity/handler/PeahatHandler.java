package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_PEAHAT;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;

/** ROM handler stages for Peahat. */
final class PeahatHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_PEAHAT, EntityTickStage.PHASE_3_AFTER_LASER, 15, PeahatHandler::phase3Peahat);
    }

    private static boolean phase3Peahat(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            slotState.updated(state.motion(PeaHatMotion.class, PeaHatMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.frame(),
                            frameInput.randomByteSupplier(),
                            frameInput.backgroundCollision()));
        }
        return true;
    }
}
