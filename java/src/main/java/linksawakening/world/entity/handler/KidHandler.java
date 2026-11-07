package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_KID_71;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_KID_72;


import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

/** ROM handler stages for Kid. */
final class KidHandler {
  static void install(EntityHandlerRegistration registry) {
    for (int type = 0; type < 256; type++) {
      if (((type == ENTITY_KID_71 || type == ENTITY_KID_72))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_KID_72,
            state -> {
              if (state.status == EntityStatus.ACTIVE

                  && state.story().bowWowState() == 0x80) {
                state.story().advanceKidnapWarningKid(
                    state.entity, state.frame, state.linkEntityX, state.linkEntityY);
                return false;
              }
              return true;
            });
      }
    }
  }
}
