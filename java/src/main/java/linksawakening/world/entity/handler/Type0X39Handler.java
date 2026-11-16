package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityFrameInputLatch;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityRewardController;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for Type0X39. */
final class Type0X39Handler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                0x39,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                21,
                frame -> phase1Type0X39(frame.slotFrame(), frame.inputs(), frame.rewards()));
    }

    private static boolean phase1Type0X39(
            EntitySlotExecution slotState, EntityFrameInputLatch frameInput, EntityRewardController rewards) {
        rewards.refreshInstrumentPalette(slotState.status(), slotState.entity().slot(), frameInput.frame());
        return true;
    }
}
