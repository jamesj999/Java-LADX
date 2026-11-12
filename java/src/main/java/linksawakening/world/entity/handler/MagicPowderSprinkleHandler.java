package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MAGIC_POWDER_SPRINKLE;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for MagicPowderSprinkle. */
final class MagicPowderSprinkleHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_MAGIC_POWDER_SPRINKLE,
                EntityTickStage.PHASE_1_MAGIC_POWDER_SPRINKLE,
                MagicPowderSprinkleHandler::phase1MagicPowderSprinkle);
    }

    private static boolean phase1MagicPowderSprinkle(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE) {
            state.projectiles()
                    .advanceMagicPowderSprinkleEntity(
                            state.index,
                            state.entity,
                            state.frame,
                            state.projectileLinkState.motionState(),
                            state.handlerInteractive && !state.creditsGameplay,
                            state.projectileTimersInteractive);
            return false;
        }
        return true;
    }
}
