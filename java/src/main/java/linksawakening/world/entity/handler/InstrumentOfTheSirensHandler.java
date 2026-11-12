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
        if (state.status == EntityStatus.ACTIVE && state.rewards().instrumentParticleKind(state.entity.slot()) == 1) {
            state.rewards().advanceInstrumentParticle(state.entity);
            return false;
        }
        return true;
    }

    private static boolean phase1InstrumentOfTheSirens2(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE
                && state.rewards().instrumentPickupSequenceActive(state.entity.slot())) {
            state.rewards()
                    .advanceInstrumentPickup(
                            state.entity,
                            state.frame,
                            state.linkEntityX,
                            state.linkEntityY,
                            state.linkZ,
                            state.randomByteSupplier);
            return false;
        }
        return true;
    }
}
