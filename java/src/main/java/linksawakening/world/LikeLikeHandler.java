package linksawakening.world;

import linksawakening.world.entity.event.LikeLikeEvent;
import static linksawakening.world.EntitySnapshots.withVariant;

/** ROM handler stages for LikeLike. */
final class LikeLikeHandler {
  static void installPhase3(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_LIKE_LIKE,
        EntityTickStage.PHASE_3_LIKE_LIKE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            LikeLikeMotion.Update likeLikeUpdate =
                state.runtime.likeLikeMotion.advance(
                    state.entity,
                    state.frame,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.linkZ,
                    state.projectileLinkState.motionState(),
                    state.runtime.actionButtonsHeld,
                    state.runtime.linkItemA,
                    state.runtime.linkItemB,
                    state.projectileLinkState.shieldLevel(),
                    state.runtime.slowTransitionCountdown[state.entity.slot()],
                    state.randomByteSupplier,
                    state.backgroundCollision);
            state.updated = likeLikeUpdate.entity();
            state.runtime.slowTransitionCountdown[state.entity.slot()] =
                likeLikeUpdate.slowTransitionCountdown();
            if (likeLikeUpdate.event() != null) {
              state.runtime.pendingLikeLikeEvents.add(likeLikeUpdate.event());
              if (likeLikeUpdate.event().kind() == LikeLikeEvent.Kind.RELEASE) {
                state.runtime.slowTimerInitialized[state.entity.slot()] = true;
              }
            }
            if (likeLikeUpdate.spriteVariant() >= 0) {
              state.updated = withVariant(state.updated, likeLikeUpdate.spriteVariant());
            }
          }
          return true;
        });
  }
}
