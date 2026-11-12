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
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                9,
                MagicPowderSprinkleHandler::phase1MagicPowderSprinkle);
    }

    private static boolean phase1MagicPowderSprinkle(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();

        if (slotState.status() == EntityStatus.ACTIVE) {
            state.projectiles()
                    .advanceMagicPowderSprinkleEntity(
                            slotState.index(),
                            slotState.entity(),
                            frameInput.frame(),
                            frameInput.projectileLinkState().motionState(),
                            timerGates.handlerInteractive() && !frameInput.creditsGameplay(),
                            timerGates.projectileTimersInteractive());
            return false;
        }
        return true;
    }
}
