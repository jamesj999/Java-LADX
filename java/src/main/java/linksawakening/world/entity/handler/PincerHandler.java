package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_PINCER;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.PincerMotion;

/** ROM handler stages for Pincer. */
final class PincerHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_PINCER, EntityTickStage.PHASE_3_BEFORE_LASER, 11, PincerHandler::phase3Pincer);
    }

    private static boolean phase3Pincer(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && timerGates.handlerLinkCollisionEnabled()) {

            // PincerEntityHandler clears its stale ground status before
            // rendering and does not call the generic background helper.
            state.background().setEntityGroundStatus(slotState.entity().slot(), 0);
            PincerMotion.Update pincerUpdate = state.motion(PincerMotion.class, PincerMotion::new)
                    .advance(
                            slotState.entity(),
                            state.slot().transitionCountdown(slotState.entity().slot()),
                            state.slot().physicsFlags(slotState.entity().slot()),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY());
            slotState.updated(pincerUpdate.entity());
            state.slot().setTransitionCountdown(slotState.entity().slot(), pincerUpdate.transitionCountdown());
            state.slot().setPhysicsFlags(slotState.entity().slot(), pincerUpdate.physicsFlags());
            presentationFlags.preservePincerPresentation(true);
        }
        return true;
    }
}
