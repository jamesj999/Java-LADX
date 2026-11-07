package linksawakening.world;



/** ROM handler stages for HeartContainer. */
final class HeartContainerHandler {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_HEART_CONTAINER,
        EntityTickStage.PHASE_1_HEART_CONTAINER,
        state -> {
          if (state.status == EntityStatus.ACTIVE

              && state.runtime.enemyTransitionCountdown[state.entity.slot()] != 0) {
            state.runtime.bossRewards.advanceCollectedHeartContainer(
                state.entity, state.linkEntityX, state.linkEntityY, state.linkZ);
            return false;
          }
          return true;
        });
  }
}
