package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityHandlerRegistration;

import static linksawakening.world.entity.EntityTypeRules.isFollowingNpcType;

/** ROM handler stages for FollowingNpcType. */
final class FollowingNpcTypeHandler {
  static void install(EntityHandlerRegistration registry) {
    for (int type = 0; type < 256; type++) {
      if (((isFollowingNpcType(type)))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_FOLLOWING_NPC_TYPE,
            state -> {
              {
                if (state.entity.type() == state.runtime.ENTITY_BOW_WOW) {
                  state.runtime.bowWowMotion.initialize(state.entity.slot());
                } else {
                  state.runtime.followingNpcMotion.initialize(
                      state.entity.slot(), state.entity.type());
                }
              }
              return true;
            });
      }
    }
  }
}
