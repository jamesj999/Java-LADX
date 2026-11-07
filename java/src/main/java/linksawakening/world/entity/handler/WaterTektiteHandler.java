package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;









/** ROM handler stages for WaterTektite. */
final class WaterTektiteHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_WATER_TEKTITE,
        EntityTickStage.PHASE_3_WATER_TEKTITE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            state.updated =
                state.runtime.waterTektiteMotion.advance(
                    state.entity, state.frame, state.randomByteSupplier, state.backgroundCollision);
          }
          return true;
        });
  }
}
