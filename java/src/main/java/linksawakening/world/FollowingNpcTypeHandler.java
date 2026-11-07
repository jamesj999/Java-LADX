package linksawakening.world;

import static linksawakening.world.EntityTypeRules.isFollowingNpcType;

/** ROM handler stages for FollowingNpcType. */
final class FollowingNpcTypeHandler {
  static void installPhase2(EntityHandlerRegistry registry) {
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
