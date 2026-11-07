package linksawakening.world;

import linksawakening.world.entity.motion.FairyMotion;



/** ROM handler stages for DroppableFairy. */
final class DroppableFairyHandler {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_DROPPABLE_FAIRY,
        EntityTickStage.PHASE_3_DROPPABLE_FAIRY,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            FairyMotion.Update fairyUpdate =
                state.runtime.fairyMotion.advance(
                    state.entity,
                    state.frame,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.randomByteSupplier,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()]);
            state.updated = fairyUpdate.entity();
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                fairyUpdate.transitionCountdown();
          }
          return true;
        });
  }
}
