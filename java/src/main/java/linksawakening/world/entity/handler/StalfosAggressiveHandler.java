package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;









/** ROM handler stages for StalfosAggressive. */
final class StalfosAggressiveHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_STALFOS_AGGRESSIVE,
        EntityTickStage.PHASE_3_STALFOS_AGGRESSIVE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            state.updated =
                state.runtime.stalfosAggressiveMotion.advance(
                    state.entity,
                    state.frame,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.randomByteSupplier,
                    state.backgroundCollision);
          }
          return true;
        });
  }
}
