package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;









/** ROM handler stages for Richard. */
final class RichardHandler {
  static void install(EntityHandlerRegistration registry) {
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
