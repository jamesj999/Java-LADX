package linksawakening.world;



/** ROM handler stages for HardhatBeetle. */
final class HardhatBeetleHandler {
  static void installPhase4(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_HARDHAT_BEETLE,
        EntityTickStage.PHASE_4_HARDHAT_BEETLE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            state.updated =
                state.runtime.hardHatMotion.advance(
                    state.entity,
                    state.frame,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.randomByteSupplier,
                    state.backgroundCollision);
          }
          return true;
        });
  }
}
