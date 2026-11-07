package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityHandlerRegistration;









/** ROM handler stages for MarinIndoor. */
final class MarinIndoorHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MARIN_INDOOR,
        EntityTickStage.PHASE_1_MARIN_INDOOR,
        state -> {
          if (state.runtime.indoorRoom

              && state.runtime.chestPlayerLevelsKnown
              && state.runtime.chestSwordLevel != 0) {
            state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
            return false;
          }
          return true;
        });
  }
}
