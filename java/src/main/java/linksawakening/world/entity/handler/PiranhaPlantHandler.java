package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_PIRANHA_PLANT;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.PiranhaMotion;

/** ROM handler stages for PiranhaPlant. */
final class PiranhaPlantHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_PIRANHA_PLANT,
                EntityTickStage.PHASE_4_BEFORE_FOLLOWER,
                9,
                PiranhaPlantHandler::phase4PiranhaPlant);
    }

    private static boolean phase4PiranhaPlant(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && !frameInput.creditsGameplay()
                && state.lifecycle().transitionSequenceCounter() == 0x04
                && timerGates.handlerLinkCollisionEnabled()) {
            int slot = slotState.entity().slot();
            PiranhaMotion.Update piranhaUpdate = state.motion(PiranhaMotion.class, PiranhaMotion::new)
                    .advance(
                            slotState.updated(),
                            state.slot().transitionCountdown(slot),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY());
            slotState.updated(piranhaUpdate.entity());
            state.slot().setTransitionCountdown(slot, piranhaUpdate.transitionCountdown());
            presentationFlags.preservePiranhaPresentation(true);
        }
        return true;
    }
}
