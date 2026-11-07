package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.entity.EntityHandlerRegistration;









/** ROM handler stages for Moblin. */
final class MoblinHandler {
  static void install(EntityHandlerRegistration registry) {
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_OCTOROK || type == RoomEntityRuntime.ENTITY_MOBLIN))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_MOBLIN,
            state -> {
              {
                state.runtime.roamingEnemyMotion.initialize(state.entity.slot());
              }
              return true;
            });
      }
    }
  }
}
