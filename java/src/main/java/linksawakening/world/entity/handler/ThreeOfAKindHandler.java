package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_THREE_OF_A_KIND;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.ThreeOfAKindMotion;

/** ROM handler stages for ThreeOfAKind. */
final class ThreeOfAKindHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_THREE_OF_A_KIND,
                EntityTickStage.PHASE_3_AFTER_LASER,
                10,
                ThreeOfAKindHandler::phase3ThreeOfAKind);
    }

    private static boolean phase3ThreeOfAKind(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && timerGates.threeOfAKindInteractive()) {

            state.slot().setHealth(slotState.entity().slot(), 0x20);
            ThreeOfAKindMotion.Update cardUpdate = state.motion(ThreeOfAKindMotion.class, ThreeOfAKindMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.frame(),
                            state.slot().transitionCountdown(slotState.entity().slot()),
                            state.slot().ignoreHits(slotState.entity().slot()),
                            frameInput.randomByteSupplier(),
                            frameInput.backgroundCollision());
            slotState.updated(cardUpdate.entity());
            state.slot().setTransitionCountdown(slotState.entity().slot(), cardUpdate.transitionCountdown());
            presentationFlags.preserveThreeOfAKindPresentation(true);
        }
        return true;
    }
}
