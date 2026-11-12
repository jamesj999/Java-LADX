package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_BUZZ_BLOB;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.motion.BuzzBlobMotion;

/** ROM handler stages for BuzzBlob. */
final class BuzzBlobHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_BUZZ_BLOB, EntityTickStage.PHASE_4_BUZZ_BLOB, BuzzBlobHandler::phase4BuzzBlob);
    }

    private static boolean phase4BuzzBlob(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE
                && !state.wasInitializing
                && !state.creditsGameplay
                && state.handlerLinkCollisionEnabled) {
            int slot = state.entity.slot();
            RoomEntityBackgroundInteraction buzzBlobBackgroundInteraction =
                    state.background().interaction();
            if (buzzBlobBackgroundInteraction == null && state.backgroundCollision != null) {
                buzzBlobBackgroundInteraction = RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            BuzzBlobMotion.Update buzzBlobUpdate = state.buzzBlobMotion()
                    .advance(
                            state.updated,
                            state.frame,
                            state.slot().transitionCountdown(slot),
                            state.randomByteSupplier,
                            buzzBlobBackgroundInteraction);
            state.updated = buzzBlobUpdate.entity();
            state.slot().setTransitionCountdown(slot, buzzBlobUpdate.transitionCountdown());
            state.applyGenericGroundInteraction = buzzBlobUpdate.appliesBackgroundInteraction();
            state.preserveBuzzBlobPresentation = true;
        }
        return true;
    }
}
