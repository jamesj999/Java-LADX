package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_SLEEPY_TOADSTOOL;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for SleepyToadstool. */
final class SleepyToadstoolHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_SLEEPY_TOADSTOOL,
                EntityTickStage.PHASE_1_AFTER_INSTRUMENT_PICKUP,
                0,
                SleepyToadstoolHandler::phase1SleepyToadstool);
    }

    private static boolean phase1SleepyToadstool(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE) {
            if (!state.rewards().toadstoolPickupSequenceActive(state.entity.slot())
                    && (state.story().playerHasToadstool() || state.story().playerMagicPowderCount() != 0)) {
                state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
                return false;
            }
            if (state.rewards().toadstoolPickupSequenceActive(state.entity.slot())) {
                state.rewards().advanceToadstoolPickup(state.entity, state.linkEntityX, state.linkEntityY, state.linkZ);
                return false;
            }
        }
        return true;
    }
}
