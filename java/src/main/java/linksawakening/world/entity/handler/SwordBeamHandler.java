package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_SWORD_BEAM;

import linksawakening.world.entity.EntityFrameInputLatch;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityProjectileController;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityTimerGates;

/** ROM handler stages for SwordBeam. */
final class SwordBeamHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                ENTITY_SWORD_BEAM,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                7,
                frame -> phase1SwordBeam(frame.slotFrame(), frame.inputs(), frame.gates(), frame.projectiles()));
    }

    private static boolean phase1SwordBeam(
            EntitySlotExecution slotState,
            EntityFrameInputLatch frameInput,
            EntityTimerGates timerGates,
            EntityProjectileController projectiles) {
        if (slotState.status() == EntityStatus.ACTIVE) {
            projectiles.advanceSwordBeamEntity(
                    slotState.index(),
                    slotState.entity(),
                    frameInput.frame(),
                    frameInput.linkEntityX(),
                    frameInput.linkEntityY(),
                    frameInput.romLinkDirection(),
                    timerGates.handlerInteractive());
            return false;
        }
        return true;
    }
}
