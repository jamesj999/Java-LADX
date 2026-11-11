package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MARIN_INDOOR;


import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityHandlerRegistration;

/** ROM handler stages for MarinIndoor. */
final class MarinIndoorHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        ENTITY_MARIN_INDOOR,
        EntityTickStage.PHASE_1_MARIN_INDOOR,
        state -> {
          if (state.roomInspection().indoorRoom()
              && state.roomInspection().indoorMarinAbsent()) {
            state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
            return false;
          }
          return true;
        });
  }
}
