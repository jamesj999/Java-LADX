package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_GENIE;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;

/** ROM handler stages for Genie. */
final class GenieHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_GENIE, EntityTickStage.PHASE_1_AFTER_STATUS_TIMERS, 0, GenieHandler::phase1Genie);
        registry.registerTick(ENTITY_GENIE, EntityTickStage.PHASE_1_AFTER_STATUS_TIMERS, 1, GenieHandler::phase1Genie2);
        registry.registerTick(ENTITY_GENIE, EntityTickStage.PHASE_2_DYING, 1, GenieHandler::phase2Genie);
        registry.registerTick(ENTITY_GENIE, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 0, GenieHandler::phase4Genie);
    }

    private static boolean phase1Genie(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (state.genie().geniePrivateCountdown1(slotState.entity().slot()) > 0) {
            state.genie().decrementGeniePrivateCountdown1(slotState.entity().slot());
        }
        return true;
    }

    private static boolean phase1Genie2(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (state.genie().geniePrivateCountdown2(slotState.entity().slot()) > 0) {
            state.genie().decrementGeniePrivateCountdown2(slotState.entity().slot());
        }
        return true;
    }

    private static boolean phase2Genie(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (state.genie().geniePrivateState1(slotState.entity().slot()) == 1) {
            state.genie()
                    .advanceGenieDestruction(slotState.entity(), frameInput.frame(), frameInput.randomByteSupplier());
            return false;
        }
        return true;
    }

    private static boolean phase4Genie(EntityFrameExecution state) {
        return state.scratch(GenieHandlerFrameStep.class, GenieHandlerFrameStep::new)
                .run(state);
    }
}
