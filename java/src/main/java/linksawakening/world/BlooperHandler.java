package linksawakening.world;

import java.util.Objects;

/** ROM handler stages for Blooper. */
final class BlooperHandler {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_BLOOPER,
        EntityTickStage.PHASE_3_BLOOPER,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing

              && state.handlerLinkCollisionEnabled) {
            RoomEntityBackgroundInteraction blooperBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (blooperBackgroundInteraction == null && state.backgroundCollision != null) {
              blooperBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            BlooperMotion.Movement movement =
                state.runtime.blooperMotion.move(
                    state.entity,
                    state.frame,
                    blooperBackgroundInteraction,
                    state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()]);
            RoomEntityGroundInteraction.Result groundResult =
                Objects.requireNonNull(
                    state.runtime.groundInteraction.apply(
                        movement.entity(),
                        state.frame,
                        state.runtime.entityGroundStatus[state.entity.slot()],
                        state.runtime.backgroundMechanics.verticalSpeedZ(movement.entity()),
                        state.runtime.groundInteractionSideScrolling),
                    "Room entity ground interaction returned null");
            BlooperMotion.Update blooperUpdate =
                state.runtime.blooperMotion.finish(
                    movement, groundResult, state.frame, state.linkEntityX, state.linkEntityY);
            state.updated = blooperUpdate.entity();
            state.preAppliedGroundResult = blooperUpdate.groundResult();
            state.preserveBlooperPresentation = true;
          }
          return true;
        });
  }
}
