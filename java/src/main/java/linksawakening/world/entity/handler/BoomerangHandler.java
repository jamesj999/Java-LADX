package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOOMERANG;

import linksawakening.world.entity.EntityFrameInputLatch;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityProjectileController;
import linksawakening.world.entity.EntityProjectileController.BoomerangFrame;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityTimerGates;

/** ROM handler stages for Boomerang. */
final class BoomerangHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                ENTITY_BOOMERANG,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                6,
                frame -> phase1Boomerang(frame.slotFrame(), frame.inputs(), frame.gates(), frame.projectiles()));
    }

    private static boolean phase1Boomerang(
            EntitySlotExecution slotState,
            EntityFrameInputLatch frameInput,
            EntityTimerGates timerGates,
            EntityProjectileController projectiles) {
        if (slotState.status() == EntityStatus.ACTIVE) {
            projectiles.advanceBoomerangEntity(new BoomerangFrame(
                    slotState.index(),
                    slotState.entity(),
                    frameInput.frame(),
                    frameInput.linkEntityX(),
                    frameInput.linkEntityY(),
                    frameInput.linkZ(),
                    frameInput.projectileLinkState().motionState(),
                    timerGates.handlerInteractive(),
                    timerGates.projectileTimersInteractive()));
            return false;
        }
        return true;
    }
}
