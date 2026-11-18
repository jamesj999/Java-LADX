package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_OWL_EVENT;

import linksawakening.world.entity.EntityFrameInputLatch;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityStoryController;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityTimerGates;

/** ROM handler stages for OwlEvent. */
final class OwlEventHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                ENTITY_OWL_EVENT,
                EntityTickStage.PHASE_1_AFTER_INSTRUMENT_PICKUP,
                1,
                frame -> phase1OwlEvent(frame.slotFrame(), frame.inputs(), frame.gates(), frame.story()));
    }

    private static boolean phase1OwlEvent(
            EntitySlotExecution slotState,
            EntityFrameInputLatch frameInput,
            EntityTimerGates timerGates,
            EntityStoryController story) {
        if (slotState.status() == EntityStatus.ACTIVE) {
            story.advanceOwlEvent(
                    slotState.entity(),
                    frameInput.frame(),
                    frameInput.linkEntityX(),
                    frameInput.linkEntityY(),
                    timerGates.handlerLinkCollisionEnabled());
            return false;
        }
        return true;
    }
}
