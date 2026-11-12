package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_LIKE_LIKE;
import static linksawakening.world.entity.EntitySnapshots.withVariant;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.motion.LikeLikeMotion;

/** ROM handler stages for LikeLike. */
final class LikeLikeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_LIKE_LIKE, EntityTickStage.PHASE_3_AFTER_LASER, 11, LikeLikeHandler::phase3LikeLike);
    }

    private static boolean phase3LikeLike(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            LikeLikeMotion.Update likeLikeUpdate = state.likeLikeMotion()
                    .advance(
                            state.entity,
                            state.frame,
                            state.linkEntityX,
                            state.linkEntityY,
                            state.linkZ,
                            state.projectileLinkState.motionState(),
                            state.link().actionButtonsHeld(),
                            state.link().linkItemA(),
                            state.link().linkItemB(),
                            state.projectileLinkState.shieldLevel(),
                            state.slot().slowTransitionCountdown(state.entity.slot()),
                            state.randomByteSupplier,
                            state.backgroundCollision);
            state.updated = likeLikeUpdate.entity();
            state.slot().setSlowTransitionCountdown(state.entity.slot(), likeLikeUpdate.slowTransitionCountdown());
            if (likeLikeUpdate.event() != null) {
                state.emit(likeLikeUpdate.event());
                if (likeLikeUpdate.event().kind() == LikeLikeEvent.Kind.RELEASE) {
                    state.slot().setSlowTimerInitialized(state.entity.slot(), true);
                }
            }
            if (likeLikeUpdate.spriteVariant() >= 0) {
                state.updated = withVariant(state.updated, likeLikeUpdate.spriteVariant());
            }
        }
        return true;
    }
}
