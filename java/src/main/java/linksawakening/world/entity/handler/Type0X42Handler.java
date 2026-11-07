package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

/** ROM handler stages for Type0X42. */
final class Type0X42Handler {
  static void install(EntityHandlerRegistration registry) {
    for (int type = 0; type < 256; type++) {
      if ((type == 0x42)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_TYPE_0X42,
            state -> {
              if (state.status == EntityStatus.ACTIVE

                  && state.runtime.indoorRoom
                  && state.runtime.entityMapId != 0x16) {
                state.story().advanceOwlStatue(
                    state.entity,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.linkZ,
                    state.projectileLinkState.motionState(),
                    state.linkAirborne);
                return false;
              }
              return true;
            });
      }
    }
  }
}
