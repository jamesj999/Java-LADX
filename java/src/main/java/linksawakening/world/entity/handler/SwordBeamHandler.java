package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_SWORD_BEAM;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for SwordBeam. */
final class SwordBeamHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_SWORD_BEAM,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                7,
                SwordBeamHandler::phase1SwordBeam);
    }

    private static boolean phase1SwordBeam(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE) {
            state.projectiles()
                    .advanceSwordBeamEntity(
                            state.index,
                            state.entity,
                            state.frame,
                            state.linkEntityX,
                            state.linkEntityY,
                            state.romLinkDirection,
                            state.handlerInteractive);
            return false;
        }
        return true;
    }
}
