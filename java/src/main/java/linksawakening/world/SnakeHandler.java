package linksawakening.world;



/** ROM handler stages for Snake. */
final class SnakeHandler {
  static void installPhase3(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_SNAKE,
        EntityTickStage.PHASE_3_SNAKE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            SnakeMotion.Update snakeUpdate =
                state.runtime.snakeMotion.advance(
                    state.entity,
                    state.frame,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.randomByteSupplier,
                    state.backgroundCollision,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()]);
            state.updated = snakeUpdate.entity();
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                snakeUpdate.transitionCountdown();
            state.preserveSnakePresentation = true;
          }
          return true;
        });
  }
}
