package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;









/** ROM handler stages for HardhatBeetle. */
final class HardhatBeetleHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_HARDHAT_BEETLE,
        EntityTickStage.PHASE_4_HARDHAT_BEETLE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            state.updated =
                state.runtime.hardHatMotion.advance(
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
