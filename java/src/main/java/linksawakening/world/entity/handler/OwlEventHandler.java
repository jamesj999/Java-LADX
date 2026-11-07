package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

/** ROM handler stages for OwlEvent. */
final class OwlEventHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_OWL_EVENT,
        EntityTickStage.PHASE_1_OWL_EVENT,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.story().advanceOwlEvent(
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
