package linksawakening.world;



/** ROM handler stages for Moblin. */
final class MoblinHandler {
  static void install(EntityHandlerRegistry registry) {
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
