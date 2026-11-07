package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

import linksawakening.world.entity.motion.ThreeOfAKindMotion;

/** ROM handler stages for ThreeOfAKind. */
final class ThreeOfAKindHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_THREE_OF_A_KIND,
        EntityTickStage.PHASE_3_THREE_OF_A_KIND,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing

              && state.threeOfAKindInteractive) {
            state.slot().setHealth(state.entity.slot(), 0x20);
            ThreeOfAKindMotion.Update cardUpdate =
                state.threeOfAKindMotion().advance(
                    state.entity,
                    state.frame,
                    state.slot().transitionCountdown(state.entity.slot()),
                    state.slot().ignoreHits(state.entity.slot()),
                    state.randomByteSupplier,
                    state.backgroundCollision);
            state.updated = cardUpdate.entity();
            state.slot().setTransitionCountdown(state.entity.slot(), cardUpdate.transitionCountdown());
            state.preserveThreeOfAKindPresentation = true;
          }
          return true;
        });
  }
}
