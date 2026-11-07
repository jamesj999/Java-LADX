package linksawakening.world;



/** ROM handler stages for WaterTektite. */
final class WaterTektiteHandler {
  static void installPhase3(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_WATER_TEKTITE,
        EntityTickStage.PHASE_3_WATER_TEKTITE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            state.updated =
                state.runtime.waterTektiteMotion.advance(
                    state.entity, state.frame, state.randomByteSupplier, state.backgroundCollision);
          }
          return true;
        });
  }
}
