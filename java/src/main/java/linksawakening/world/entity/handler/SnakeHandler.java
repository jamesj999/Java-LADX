package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_SNAKE;


import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

import linksawakening.world.entity.motion.SnakeMotion;

/** ROM handler stages for Snake. */
final class SnakeHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        ENTITY_SNAKE,
        EntityTickStage.PHASE_3_SNAKE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            SnakeMotion.Update snakeUpdate =
                state.snakeMotion().advance(
                    state.entity,
                    state.frame,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.randomByteSupplier,
                    state.backgroundCollision,
                    state.slot().transitionCountdown(state.entity.slot()));
            state.updated = snakeUpdate.entity();
            state.slot().setTransitionCountdown(state.entity.slot(), snakeUpdate.transitionCountdown());
            state.preserveSnakePresentation = true;
          }
          return true;
        });
  }
}
