package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_HEART_CONTAINER;

import linksawakening.world.entity.EntityBossRewards;
import linksawakening.world.entity.EntityFrameInputLatch;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntitySlotView;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for HeartContainer. */
final class HeartContainerHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                ENTITY_HEART_CONTAINER,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                14,
                frame -> phase1HeartContainer(frame.slotFrame(), frame.inputs(), frame.slot(), frame.bossRewards()));
    }

    private static boolean phase1HeartContainer(
            EntitySlotExecution slotState,
            EntityFrameInputLatch frameInput,
            EntitySlotView slot,
            EntityBossRewards bossRewards) {
        if (slotState.status() == EntityStatus.ACTIVE
                && slot.transitionCountdown(slotState.entity().slot()) != 0) {
            bossRewards.advanceCollectedHeartContainer(
                    slotState.entity(), frameInput.linkEntityX(), frameInput.linkEntityY(), frameInput.linkZ());
            return false;
        }
        return true;
    }
}
