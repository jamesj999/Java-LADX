package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MAGIC_ROD_FIREBALL;

import linksawakening.world.entity.EntityFrameInputLatch;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityProjectileController;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityTimerGates;

/** ROM handler stages for MagicRodFireball. */
final class MagicRodFireballHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                ENTITY_MAGIC_ROD_FIREBALL,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                8,
                frame -> phase1MagicRodFireball(frame.slotFrame(), frame.inputs(), frame.gates(), frame.projectiles()));
    }

    private static boolean phase1MagicRodFireball(
            EntitySlotExecution slotState,
            EntityFrameInputLatch frameInput,
            EntityTimerGates timerGates,
            EntityProjectileController projectiles) {
        if (slotState.status() == EntityStatus.ACTIVE) {
            projectiles.advanceMagicRodFireballEntity(
                    slotState.index(),
                    slotState.entity(),
                    frameInput.frame(),
                    timerGates.handlerInteractive(),
                    timerGates.projectileTimersInteractive());
            return false;
        }
        return true;
    }
}
