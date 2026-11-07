package linksawakening.world;



/** ROM handler stages for Star. */
final class StarHandler {
  static void installPhase3(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_STAR,
        EntityTickStage.PHASE_3_STAR,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing

              && state.handlerLinkCollisionEnabled) {
            RoomEntityBackgroundInteraction starBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (starBackgroundInteraction == null && state.backgroundCollision != null) {
              starBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            state.updated =
                state.runtime.starMotion.advance(
                    state.entity,
                    state.frame,
                    state.randomByteSupplier,
                    starBackgroundInteraction,
                    state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()]);
            state.preserveStarPresentation = true;
          }
          return true;
        });
  }
}
