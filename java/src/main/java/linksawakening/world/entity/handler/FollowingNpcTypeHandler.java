package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOW_WOW;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for FollowingNpcType. */
final class FollowingNpcTypeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_BOW_WOW,
                EntityTickStage.PHASE_2_FOLLOWING_NPC_TYPE,
                FollowingNpcTypeHandler::phase2FollowingNpcType);
        registry.registerTick(
                ENTITY_MARIN_AT_THE_SHORE,
                EntityTickStage.PHASE_2_FOLLOWING_NPC_TYPE,
                FollowingNpcTypeHandler::phase2FollowingNpcType);
        registry.registerTick(
                ENTITY_GHOST,
                EntityTickStage.PHASE_2_FOLLOWING_NPC_TYPE,
                FollowingNpcTypeHandler::phase2FollowingNpcType);
        registry.registerTick(
                ENTITY_ROOSTER,
                EntityTickStage.PHASE_2_FOLLOWING_NPC_TYPE,
                FollowingNpcTypeHandler::phase2FollowingNpcType);
    }

    private static boolean phase2FollowingNpcType(EntityFrameExecution state) {
        if (state.entity.type() == ENTITY_BOW_WOW) {
            state.bowWowMotion().initialize(state.entity.slot());
        } else {
            state.followingNpcMotion().initialize(state.entity.slot(), state.entity.type());
        }

        return true;
    }
}
