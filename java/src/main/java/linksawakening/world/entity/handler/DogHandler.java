package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_DOG;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.DogFrame;
import linksawakening.world.entity.motion.DogMotion;

/** ROM handler stages for Dog. */
final class DogHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_DOG, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(ENTITY_DOG, EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE, 2, DogHandler::phase1Dog);
        registry.registerTick(ENTITY_DOG, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 14, DogHandler::phase4Dog);
    }

    private static boolean phase1Dog(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (slotState.status() == EntityStatus.ACTIVE) {
            // DogEntityHandler writes this before ReturnIfNonInteractive_19.
            state.slot().setHealth(slotState.entity().slot(), DogMotion.HANDLER_HEALTH);
        }
        return true;
    }

    private static boolean phase4Dog(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && !frameInput.creditsGameplay()
                && timerGates.handlerLinkCollisionEnabled()) {
            int slot = slotState.entity().slot();
            RoomEntityBackgroundInteraction dogBackgroundInteraction =
                    state.background().interaction();
            if (dogBackgroundInteraction == null && frameInput.backgroundCollision() != null) {
                dogBackgroundInteraction = frameInput.hinoxBackgroundInteraction();
            }
            DogMotion.Update dogUpdate = state.motion(DogMotion.class, DogMotion::new)
                    .advance(new DogFrame(
                            slotState.updated(),
                            frameInput.frame(),
                            state.slot().transitionCountdown(slot),
                            state.slot().physicsFlags(slot),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.romLinkDirection(),
                            state.link().actionButtonAHeld(),
                            state.link().dialogActive(),
                            frameInput.randomByteSupplier(),
                            dogBackgroundInteraction));
            slotState.updated(dogUpdate.entity());
            state.slot().setTransitionCountdown(slot, dogUpdate.transitionCountdown());
            state.slot().setPhysicsFlags(slot, dogUpdate.physicsFlags());
            slotState.applyGenericGroundInteraction(dogUpdate.appliesBackgroundInteraction());
            if (dogUpdate.dialogRequested()) {
                state.emit(new DialogRequest(0, dogUpdate.dialogLowId()));
            }
            presentationFlags.preserveDogPresentation(true);
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(DogMotion.class, DogMotion::new).initialize(entity.slot());
    }
}
