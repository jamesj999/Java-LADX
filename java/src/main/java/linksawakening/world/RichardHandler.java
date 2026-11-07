package linksawakening.world;



/** ROM handler stages for Richard. */
final class RichardHandler {
  static void installPhase1(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_RICHARD,
        EntityTickStage.PHASE_1_RICHARD,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.runtime.story.advanceRichard(
                state.entity,
                state.frame,
                state.linkEntityX,
                state.linkEntityY,
                state.linkAirborne);
            return false;
          }
          return true;
        });
  }
}
