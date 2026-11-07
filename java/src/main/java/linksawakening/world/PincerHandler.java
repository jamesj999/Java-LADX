package linksawakening.world;

import linksawakening.world.entity.motion.PincerMotion;



/** ROM handler stages for Pincer. */
final class PincerHandler {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_PINCER,
        EntityTickStage.PHASE_3_PINCER,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing

              && state.handlerLinkCollisionEnabled) {
            // PincerEntityHandler clears its stale ground status before
            // rendering and does not call the generic background helper.
            state.runtime.entityGroundStatus[state.entity.slot()] = 0;
            PincerMotion.Update pincerUpdate =
                state.runtime.pincerMotion.advance(
                    state.entity,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.runtime.enemyPhysicsFlags[state.entity.slot()],
                    state.linkEntityX,
                    state.linkEntityY);
            state.updated = pincerUpdate.entity();
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                pincerUpdate.transitionCountdown();
            state.runtime.enemyPhysicsFlags[state.entity.slot()] = pincerUpdate.physicsFlags();
            state.preservePincerPresentation = true;
          }
          return true;
        });
  }
}
