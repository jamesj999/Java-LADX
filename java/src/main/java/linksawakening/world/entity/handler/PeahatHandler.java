package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

/** ROM handler stages for Peahat. */
final class PeahatHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_PEAHAT,
        EntityTickStage.PHASE_3_PEAHAT,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            state.updated =
                state.runtime.peaHatMotion.advance(
                    state.entity, state.frame, state.randomByteSupplier, state.backgroundCollision);
          }
          return true;
        });
  }
}
