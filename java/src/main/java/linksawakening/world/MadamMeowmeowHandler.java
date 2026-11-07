package linksawakening.world;



/** ROM handler stages for MadamMeowmeow. */
final class MadamMeowmeowHandler {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MADAM_MEOWMEOW,
        EntityTickStage.PHASE_1_MADAM_MEOWMEOW,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.runtime.story.advanceMadamMeowMeow(
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
