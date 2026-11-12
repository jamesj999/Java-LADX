package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for Type0X42. */
final class Type0X42Handler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                0x42, EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE, 17, Type0X42Handler::phase1Type0X42);
    }

    private static boolean phase1Type0X42(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE
                && state.roomInspection().indoorRoom()
                && state.roomInspection().entityMapId() != 0x16) {
            state.story()
                    .advanceOwlStatue(
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
