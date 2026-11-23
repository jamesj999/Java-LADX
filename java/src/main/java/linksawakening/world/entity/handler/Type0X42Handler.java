package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityFrameInputLatch;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityRoomInspection;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityStoryController;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for Type0X42. */
final class Type0X42Handler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                0x42,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                17,
                frame -> phase1Type0X42(frame.slotFrame(), frame.inputs(), frame.roomInspection(), frame.story()));
    }

    private static boolean phase1Type0X42(
            EntitySlotExecution slotState,
            EntityFrameInputLatch frameInput,
            EntityRoomInspection roomInspection,
            EntityStoryController story) {
        if (slotState.status() == EntityStatus.ACTIVE
                && roomInspection.indoorRoom()
                && roomInspection.entityMapId() != 0x16) {
            story.advanceOwlStatue(
                    slotState.entity(),
                    frameInput.linkEntityX(),
                    frameInput.linkEntityY(),
                    frameInput.linkZ(),
                    frameInput.projectileLinkState().motionState(),
                    frameInput.linkAirborne());
            return false;
        }
        return true;
    }
}
