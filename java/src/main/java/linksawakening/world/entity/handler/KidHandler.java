package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;









/** ROM handler stages for Kid. */
final class KidHandler {
  static void install(EntityHandlerRegistration registry) {
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_KID_71 || type == RoomEntityRuntime.ENTITY_KID_72))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_KID_72,
            state -> {
              if (state.status == EntityStatus.ACTIVE

                  && state.runtime.bowWowState == 0x80) {
                state.runtime.story.advanceKidnapWarningKid(
                    state.entity, state.frame, state.linkEntityX, state.linkEntityY);
                return false;
              }
              return true;
            });
      }
    }
  }
}
