package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

/** ROM handler stages for SleepyToadstool. */
final class SleepyToadstoolHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_SLEEPY_TOADSTOOL,
        EntityTickStage.PHASE_1_SLEEPY_TOADSTOOL,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            if (!state.runtime.toadstoolPickupSequenceActive[state.entity.slot()]
                && (state.runtime.playerHasToadstool
                    || state.runtime.playerMagicPowderCount != 0)) {
              state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
              return false;
            }
            if (state.runtime.toadstoolPickupSequenceActive[state.entity.slot()]) {
              state.runtime.rewards.advanceToadstoolPickup(
                  state.entity, state.linkEntityX, state.linkEntityY, state.linkZ);
              return false;
            }
          }
          return true;
        });
  }
}
