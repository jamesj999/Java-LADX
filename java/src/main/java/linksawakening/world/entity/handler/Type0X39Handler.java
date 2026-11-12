package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for Type0X39. */
final class Type0X39Handler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                0x39, EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE, 21, Type0X39Handler::phase1Type0X39);
    }

    private static boolean phase1Type0X39(EntityFrameExecution state) {
        state.rewards().refreshInstrumentPalette(state.status, state.entity.slot(), state.frame);
        return true;
    }
}
