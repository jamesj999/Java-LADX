package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MADAM_MEOWMEOW;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for MadamMeowmeow. */
final class MadamMeowmeowHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_MADAM_MEOWMEOW,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                15,
                MadamMeowmeowHandler::phase1MadamMeowmeow);
    }

    private static boolean phase1MadamMeowmeow(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE) {
            state.story()
                    .advanceMadamMeowMeow(
                            state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.linkAirborne);
            return false;
        }
        return true;
    }
}
