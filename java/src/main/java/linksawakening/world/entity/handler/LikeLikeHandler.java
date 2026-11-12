package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_LIKE_LIKE;
import static linksawakening.world.entity.EntitySnapshots.withVariant;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.LikeLikeFrame;
import linksawakening.world.entity.motion.LikeLikeMotion;

/** ROM handler stages for LikeLike. */
final class LikeLikeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_LIKE_LIKE, EntityTickStage.PHASE_3_AFTER_LASER, 11, LikeLikeHandler::phase3LikeLike);
    }

    private static boolean phase3LikeLike(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            LikeLikeMotion.Update likeLikeUpdate = state.motion(LikeLikeMotion.class, LikeLikeMotion::new)
                    .advance(new LikeLikeFrame(
                            slotState.entity(),
                            frameInput.frame(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.linkZ(),
                            frameInput.projectileLinkState().motionState(),
                            state.link().actionButtonsHeld(),
                            state.link().linkItemA(),
                            state.link().linkItemB(),
                            frameInput.projectileLinkState().shieldLevel(),
                            state.slot()
                                    .slowTransitionCountdown(slotState.entity().slot()),
                            frameInput.randomByteSupplier(),
                            frameInput.backgroundCollision()));
            slotState.updated(likeLikeUpdate.entity());
            state.slot()
                    .setSlowTransitionCountdown(slotState.entity().slot(), likeLikeUpdate.slowTransitionCountdown());
            if (likeLikeUpdate.event() != null) {
                state.emit(likeLikeUpdate.event());
                if (likeLikeUpdate.event().kind() == LikeLikeEvent.Kind.RELEASE) {
                    state.slot().setSlowTimerInitialized(slotState.entity().slot(), true);
                }
            }
            if (likeLikeUpdate.spriteVariant() >= 0) {
                slotState.updated(withVariant(slotState.updated(), likeLikeUpdate.spriteVariant()));
            }
        }
        return true;
    }
}
