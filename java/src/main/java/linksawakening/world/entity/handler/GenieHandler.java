package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_GENIE;

import linksawakening.world.entity.EntityFrameInputLatch;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.GenieEntityController;

/** ROM handler stages for Genie. */
final class GenieHandler {
    static void install(EntityHandlerRegistry registry, GenieEntityController genie) {
        registry.registerTick(
                ENTITY_GENIE,
                EntityTickStage.PHASE_1_AFTER_STATUS_TIMERS,
                0,
                frame -> phase1Genie(frame.slotFrame(), genie));
        registry.registerTick(
                ENTITY_GENIE,
                EntityTickStage.PHASE_1_AFTER_STATUS_TIMERS,
                1,
                frame -> phase1Genie2(frame.slotFrame(), genie));
        registry.registerTick(
                ENTITY_GENIE,
                EntityTickStage.PHASE_2_DYING,
                1,
                frame -> phase2Genie(frame.slotFrame(), frame.inputs(), genie));
        registry.registerTick(
                ENTITY_GENIE,
                EntityTickStage.PHASE_4_BEFORE_FOLLOWER,
                0,
                frame -> genie.advanceActiveSlot(frame.slotFrame(), frame.inputs(), frame.gates()));
    }

    private static boolean phase1Genie(EntitySlotExecution slotState, GenieEntityController genie) {
        if (genie.phaseCountdown(slotState.entity().slot()) > 0) {
            genie.decrementPhaseCountdown(slotState.entity().slot());
        }
        return true;
    }

    private static boolean phase1Genie2(EntitySlotExecution slotState, GenieEntityController genie) {
        if (genie.attackFlashCountdown(slotState.entity().slot()) > 0) {
            genie.decrementAttackFlashCountdown(slotState.entity().slot());
        }
        return true;
    }

    private static boolean phase2Genie(
            EntitySlotExecution slotState, EntityFrameInputLatch frameInput, GenieEntityController genie) {
        if (genie.phase(slotState.entity().slot()) == 1) {
            genie.advanceGenieDestruction(slotState.entity(), frameInput.frame(), frameInput.randomByteSupplier());
            return false;
        }
        return true;
    }
}
