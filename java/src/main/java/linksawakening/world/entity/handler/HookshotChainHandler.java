package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_HOOKSHOT_CHAIN;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;

/** ROM handler stages for HookshotChain. */
final class HookshotChainHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_HOOKSHOT_CHAIN,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                4,
                HookshotChainHandler::phase1HookshotChain);
    }

    private static boolean phase1HookshotChain(EntityFrameExecution state) {
        return state.scratch(HookshotChainHandlerFrameStep.class, HookshotChainHandlerFrameStep::new)
                .run(state);
    }
}
