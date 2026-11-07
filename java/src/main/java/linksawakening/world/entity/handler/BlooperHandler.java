package linksawakening.world.entity.handler;

import linksawakening.world.entity.RoomEntityGroundInteraction;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

import linksawakening.world.entity.motion.BlooperMotion;

import java.util.Objects;

/** ROM handler stages for Blooper. */
final class BlooperHandler {
  static void install(EntityHandlerRegistration registry) {
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
