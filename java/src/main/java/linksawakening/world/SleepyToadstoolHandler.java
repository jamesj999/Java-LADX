package linksawakening.world;



/** ROM handler stages for SleepyToadstool. */
final class SleepyToadstoolHandler {
  static void install(EntityHandlerRegistry registry) {
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
