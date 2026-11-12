package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_BUZZ_BLOB;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BuzzBlobMotion;

/** ROM handler stages for BuzzBlob. */
final class BuzzBlobHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_BUZZ_BLOB, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(
                ENTITY_BUZZ_BLOB, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 12, BuzzBlobHandler::phase4BuzzBlob);
    }

    private static boolean phase4BuzzBlob(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && !frameInput.creditsGameplay()
                && timerGates.handlerLinkCollisionEnabled()) {
            int slot = slotState.entity().slot();
            RoomEntityBackgroundInteraction buzzBlobBackgroundInteraction =
                    state.background().interaction();
            if (buzzBlobBackgroundInteraction == null && frameInput.backgroundCollision() != null) {
                buzzBlobBackgroundInteraction = frameInput.hinoxBackgroundInteraction();
            }
            BuzzBlobMotion.Update buzzBlobUpdate = state.motion(BuzzBlobMotion.class, BuzzBlobMotion::new)
                    .advance(
                            slotState.updated(),
                            frameInput.frame(),
                            state.slot().transitionCountdown(slot),
                            frameInput.randomByteSupplier(),
                            buzzBlobBackgroundInteraction);
            slotState.updated(buzzBlobUpdate.entity());
            state.slot().setTransitionCountdown(slot, buzzBlobUpdate.transitionCountdown());
            slotState.applyGenericGroundInteraction(buzzBlobUpdate.appliesBackgroundInteraction());
            presentationFlags.preserveBuzzBlobPresentation(true);
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(BuzzBlobMotion.class, BuzzBlobMotion::new).initialize(entity.slot());
    }
}
