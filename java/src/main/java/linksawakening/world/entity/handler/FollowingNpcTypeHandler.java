package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOW_WOW;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;

/** ROM handler stages for FollowingNpcType. */
final class FollowingNpcTypeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_BOW_WOW,
                EntityTickStage.PHASE_2_AFTER_INITIALIZE,
                10,
                FollowingNpcTypeHandler::phase2FollowingNpcType);
        registry.registerTick(
                ENTITY_MARIN_AT_THE_SHORE,
                EntityTickStage.PHASE_2_AFTER_INITIALIZE,
                10,
                FollowingNpcTypeHandler::phase2FollowingNpcType);
        registry.registerTick(
                ENTITY_GHOST,
                EntityTickStage.PHASE_2_AFTER_INITIALIZE,
                10,
                FollowingNpcTypeHandler::phase2FollowingNpcType);
        registry.registerTick(
                ENTITY_ROOSTER,
                EntityTickStage.PHASE_2_AFTER_INITIALIZE,
                10,
                FollowingNpcTypeHandler::phase2FollowingNpcType);
    }

    private static boolean phase2FollowingNpcType(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (slotState.entity().type() == ENTITY_BOW_WOW) {
            state.motion(BowWowMotion.class, BowWowMotion::new)
                    .initialize(slotState.entity().slot());
        } else {
            state.motion(FollowingNpcMotion.class, FollowingNpcMotion::new)
                    .initialize(slotState.entity().slot(), slotState.entity().type());
        }

        return true;
    }
}
