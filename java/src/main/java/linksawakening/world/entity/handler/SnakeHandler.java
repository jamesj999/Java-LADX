package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;







import linksawakening.world.entity.motion.SnakeMotion;



/** ROM handler stages for Snake. */
final class SnakeHandler {
  static void install(EntityHandlerRegistration registry) {
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
