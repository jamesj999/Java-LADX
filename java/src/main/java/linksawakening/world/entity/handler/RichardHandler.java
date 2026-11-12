package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_RICHARD;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for Richard. */
final class RichardHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_RICHARD, EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE, 16, RichardHandler::phase1Richard);
    }

    private static boolean phase1Richard(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE) {
            state.story()
                    .advanceRichard(
                            state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.linkAirborne);
            return false;
        }
        return true;
    }
}
