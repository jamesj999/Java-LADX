package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

import linksawakening.world.entity.motion.FairyMotion;

/** ROM handler stages for DroppableFairy. */
final class DroppableFairyHandler {
  static void install(EntityHandlerRegistration registry) {
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
                    state.slot().transitionCountdown(state.entity.slot()));
            state.updated = fairyUpdate.entity();
            state.slot().setTransitionCountdown(state.entity.slot(), fairyUpdate.transitionCountdown());
          }
          return true;
        });
  }
}
