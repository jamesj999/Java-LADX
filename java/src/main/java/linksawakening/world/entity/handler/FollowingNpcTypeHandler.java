package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOW_WOW;
import static linksawakening.world.entity.EntityTypeRules.isFollowingNpcType;

import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for FollowingNpcType. */
final class FollowingNpcTypeHandler {
    static void install(EntityHandlerRegistration registry) {
        for (int type = 0; type < 256; type++) {
            if (((isFollowingNpcType(type)))) {
                registry.registerTick(type, EntityTickStage.PHASE_2_FOLLOWING_NPC_TYPE, state -> {
                    {
                        if (state.entity.type() == ENTITY_BOW_WOW) {
                            state.bowWowMotion().initialize(state.entity.slot());
                        } else {
                            state.followingNpcMotion().initialize(state.entity.slot(), state.entity.type());
                        }
                    }
                    return true;
                });
            }
        }
    }
}
