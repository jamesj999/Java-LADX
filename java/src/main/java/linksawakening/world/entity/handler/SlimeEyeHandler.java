package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityTickStage;

/** The complete bank-$04 handler owns its slot through its early return. */
final class SlimeEyeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(0x5B, EntityTickStage.PHASE_2_SLIME_EYE, frame -> {
            frame.slimeEye().advance(frame);
            return false;
        });
    }
}
