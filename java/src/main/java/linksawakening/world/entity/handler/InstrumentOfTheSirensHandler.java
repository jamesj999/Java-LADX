package linksawakening.world.entity.handler;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.EntityFrameInputLatch;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityRewardController;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for InstrumentOfTheSirens. */
final class InstrumentOfTheSirensHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                22,
                frame -> phase1InstrumentOfTheSirens(frame.slotFrame(), frame.rewards()));
        registry.registerTick(
                EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                23,
                frame -> phase1InstrumentOfTheSirens2(frame.slotFrame(), frame.inputs(), frame.rewards()));
    }

    private static boolean phase1InstrumentOfTheSirens(EntitySlotExecution slotState, EntityRewardController rewards) {
        if (slotState.status() == EntityStatus.ACTIVE
                && rewards.instrumentParticleKind(slotState.entity().slot()) == 1) {
            rewards.advanceInstrumentParticle(slotState.entity());
            return false;
        }
        return true;
    }

    private static boolean phase1InstrumentOfTheSirens2(
            EntitySlotExecution slotState, EntityFrameInputLatch frameInput, EntityRewardController rewards) {
        if (slotState.status() == EntityStatus.ACTIVE
                && rewards.instrumentPickupSequenceActive(slotState.entity().slot())) {
            rewards.advanceInstrumentPickup(
                    slotState.entity(),
                    frameInput.frame(),
                    frameInput.linkEntityX(),
                    frameInput.linkEntityY(),
                    frameInput.linkZ(),
                    frameInput.randomByteSupplier());
            return false;
        }
        return true;
    }
}
