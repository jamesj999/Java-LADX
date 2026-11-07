package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

/** ROM handler stages for HeartContainer. */
final class HeartContainerHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_HEART_CONTAINER,
        EntityTickStage.PHASE_1_HEART_CONTAINER,
        state -> {
          if (state.status == EntityStatus.ACTIVE

              && state.slot().transitionCountdown(state.entity.slot()) != 0) {
            state.runtime.bossRewards.advanceCollectedHeartContainer(
                state.entity, state.linkEntityX, state.linkEntityY, state.linkZ);
            return false;
          }
          return true;
        });
  }
}
