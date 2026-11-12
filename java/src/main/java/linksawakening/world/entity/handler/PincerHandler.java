package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_PINCER;

import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.PincerMotion;

/** ROM handler stages for Pincer. */
final class PincerHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_PINCER, EntityTickStage.PHASE_3_PINCER, state -> {
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && state.handlerLinkCollisionEnabled) {

                // PincerEntityHandler clears its stale ground status before
                // rendering and does not call the generic background helper.
                state.background().setEntityGroundStatus(state.entity.slot(), 0);
                PincerMotion.Update pincerUpdate = state.pincerMotion()
                        .advance(
                                state.entity,
                                state.slot().transitionCountdown(state.entity.slot()),
                                state.slot().physicsFlags(state.entity.slot()),
                                state.linkEntityX,
                                state.linkEntityY);
                state.updated = pincerUpdate.entity();
                state.slot().setTransitionCountdown(state.entity.slot(), pincerUpdate.transitionCountdown());
                state.slot().setPhysicsFlags(state.entity.slot(), pincerUpdate.physicsFlags());
                state.preservePincerPresentation = true;
            }
            return true;
        });
    }
}
