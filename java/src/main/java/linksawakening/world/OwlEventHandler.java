package linksawakening.world;



/** ROM handler stages for OwlEvent. */
final class OwlEventHandler {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_OWL_EVENT,
        EntityTickStage.PHASE_1_OWL_EVENT,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.runtime.story.advanceOwlEvent(
                state.entity,
                state.frame,
                state.linkEntityX,
                state.linkEntityY,
                state.handlerLinkCollisionEnabled);
            return false;
          }
          return true;
        });
  }
}
