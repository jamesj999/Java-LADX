package linksawakening.world;



/** ROM handler stages for ThreeOfAKind. */
final class ThreeOfAKindHandler {
  static void installPhase3(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_THREE_OF_A_KIND,
        EntityTickStage.PHASE_3_THREE_OF_A_KIND,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing

              && state.threeOfAKindInteractive) {
            state.runtime.enemyHealth[state.entity.slot()] = 0x20;
            ThreeOfAKindMotion.Update cardUpdate =
                state.runtime.threeOfAKindMotion.advance(
                    state.entity,
                    state.frame,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()],
                    state.randomByteSupplier,
                    state.backgroundCollision);
            state.updated = cardUpdate.entity();
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                cardUpdate.transitionCountdown();
            state.preserveThreeOfAKindPresentation = true;
          }
          return true;
        });
  }
}
