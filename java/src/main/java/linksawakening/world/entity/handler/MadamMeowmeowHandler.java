package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MADAM_MEOWMEOW;

import linksawakening.world.entity.EntityFrameInputLatch;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityStoryController;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for MadamMeowmeow. */
final class MadamMeowmeowHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                ENTITY_MADAM_MEOWMEOW,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                15,
                frame -> phase1MadamMeowmeow(frame.slotFrame(), frame.inputs(), frame.story()));
    }

    private static boolean phase1MadamMeowmeow(
            EntitySlotExecution slotState, EntityFrameInputLatch frameInput, EntityStoryController story) {
        if (slotState.status() == EntityStatus.ACTIVE) {
            story.advanceMadamMeowMeow(
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
