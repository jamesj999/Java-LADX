package linksawakening.world;



/** ROM handler stages for MarinIndoor. */
final class MarinIndoorHandler {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MARIN_INDOOR,
        EntityTickStage.PHASE_1_MARIN_INDOOR,
        state -> {
          if (state.runtime.indoorRoom

              && state.runtime.chestPlayerLevelsKnown
              && state.runtime.chestSwordLevel != 0) {
            state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
            return false;
          }
          return true;
        });
  }
}
