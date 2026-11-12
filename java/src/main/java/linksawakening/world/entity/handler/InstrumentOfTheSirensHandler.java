package linksawakening.world.entity.handler;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for InstrumentOfTheSirens. */
final class InstrumentOfTheSirensHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                22,
                InstrumentOfTheSirensHandler::phase1InstrumentOfTheSirens);
        registry.registerTick(
                EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                23,
                InstrumentOfTheSirensHandler::phase1InstrumentOfTheSirens2);
    }

    private static boolean phase1InstrumentOfTheSirens(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (slotState.status() == EntityStatus.ACTIVE
                && state.rewards().instrumentParticleKind(slotState.entity().slot()) == 1) {
            state.rewards().advanceInstrumentParticle(slotState.entity());
            return false;
        }
        return true;
    }

    private static boolean phase1InstrumentOfTheSirens2(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE
                && state.rewards()
                        .instrumentPickupSequenceActive(slotState.entity().slot())) {
            state.rewards()
                    .advanceInstrumentPickup(
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
