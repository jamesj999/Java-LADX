package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_SLEEPY_TOADSTOOL;

import linksawakening.world.entity.EntityFrameInputLatch;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityRewardController;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntitySlotLifecycle;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityStoryController;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for SleepyToadstool. */
final class SleepyToadstoolHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                ENTITY_SLEEPY_TOADSTOOL,
                EntityTickStage.PHASE_1_AFTER_INSTRUMENT_PICKUP,
                0,
                frame -> phase1SleepyToadstool(
                        frame.slotFrame(), frame.inputs(), frame.rewards(), frame.story(), frame.lifecycle()));
    }

    private static boolean phase1SleepyToadstool(
            EntitySlotExecution slotState,
            EntityFrameInputLatch frameInput,
            EntityRewardController rewards,
            EntityStoryController story,
            EntitySlotLifecycle lifecycle) {
        if (slotState.status() == EntityStatus.ACTIVE) {
            if (!rewards.toadstoolPickupSequenceActive(slotState.entity().slot())
                    && (story.playerHasToadstool() || story.playerMagicPowderCount() != 0)) {
                lifecycle.disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
            if (rewards.toadstoolPickupSequenceActive(slotState.entity().slot())) {
                rewards.advanceToadstoolPickup(
                        slotState.entity(), frameInput.linkEntityX(), frameInput.linkEntityY(), frameInput.linkZ());
                return false;
            }
        }
        return true;
    }
}
