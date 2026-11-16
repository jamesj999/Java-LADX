package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_RICHARD;

import linksawakening.world.entity.EntityFrameInputLatch;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityStoryController;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for Richard. */
final class RichardHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                ENTITY_RICHARD,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                16,
                frame -> phase1Richard(frame.slotFrame(), frame.inputs(), frame.story()));
    }

    private static boolean phase1Richard(
            EntitySlotExecution slotState, EntityFrameInputLatch frameInput, EntityStoryController story) {
        if (slotState.status() == EntityStatus.ACTIVE) {
            story.advanceRichard(
                    slotState.entity(),
                    frameInput.frame(),
                    frameInput.linkEntityX(),
                    frameInput.linkEntityY(),
                    frameInput.linkAirborne());
            return false;
        }
        return true;
    }
}
