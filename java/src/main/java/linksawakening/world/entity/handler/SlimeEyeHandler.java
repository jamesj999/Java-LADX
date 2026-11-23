package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_SLIME_EYE;

import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.SlimeEyeController;

/** The complete bank-$04 handler owns its slot through its early return. */
final class SlimeEyeHandler {
    static void install(EntityHandlerRegistry registry, SlimeEyeController slimeEye) {
        registry.registerTick(ENTITY_SLIME_EYE, EntityTickStage.PHASE_2_BEFORE_STATUS_BRANCHES, 0, frame -> {
            slimeEye.advance(frame.slotFrame().entity(), frame.inputs(), frame.gates());
            return false;
        });
    }
}
