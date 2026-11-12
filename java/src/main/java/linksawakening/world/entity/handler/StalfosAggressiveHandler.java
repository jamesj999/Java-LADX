package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_STALFOS_AGGRESSIVE;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;

/** ROM handler stages for StalfosAggressive. */
final class StalfosAggressiveHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_STALFOS_AGGRESSIVE,
                EntityTickStage.PHASE_3_AFTER_LASER,
                8,
                StalfosAggressiveHandler::phase3StalfosAggressive);
    }

    private static boolean phase3StalfosAggressive(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            slotState.updated(state.motion(StalfosAggressiveMotion.class, StalfosAggressiveMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.frame(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.randomByteSupplier(),
                            frameInput.backgroundCollision()));
        }
        return true;
    }
}
