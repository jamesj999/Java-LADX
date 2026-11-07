package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

/** ROM handler stages for MadamMeowmeow. */
final class MadamMeowmeowHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MADAM_MEOWMEOW,
        EntityTickStage.PHASE_1_MADAM_MEOWMEOW,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.story().advanceMadamMeowMeow(
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
