package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MAGIC_POWDER_SPRINKLE;

import linksawakening.world.entity.EntityFrameInputLatch;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityProjectileController;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityTimerGates;

/** ROM handler stages for MagicPowderSprinkle. */
final class MagicPowderSprinkleHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                ENTITY_MAGIC_POWDER_SPRINKLE,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                9,
                frame -> phase1MagicPowderSprinkle(
                        frame.slotFrame(), frame.inputs(), frame.gates(), frame.projectiles()));
    }

    private static boolean phase1MagicPowderSprinkle(
            EntitySlotExecution slotState,
            EntityFrameInputLatch frameInput,
            EntityTimerGates timerGates,
            EntityProjectileController projectiles) {
        if (slotState.status() == EntityStatus.ACTIVE) {
            projectiles.advanceMagicPowderSprinkleEntity(
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
