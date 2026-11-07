package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;







import linksawakening.entity.EntitySpriteHandlerCatalog;

/** ROM handler stages for InstrumentOfTheSirens. */
final class InstrumentOfTheSirensHandler {
  static void install(EntityHandlerRegistration registry) {
    for (int type = 0; type < 256; type++) {
      if ((type == EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_INSTRUMENT_OF_THE_SIRENS,
            state -> {
              if (state.status == EntityStatus.ACTIVE

                  && state.runtime.instrumentParticleKind[state.entity.slot()] == 1) {
                state.runtime.rewards.advanceInstrumentParticle(state.entity);
                return false;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((type == EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_INSTRUMENT_OF_THE_SIRENS_2,
            state -> {
              if (state.status == EntityStatus.ACTIVE

                  && state.runtime.instrumentPickupSequenceActive[state.entity.slot()]) {
                state.runtime.rewards.advanceInstrumentPickup(
                    state.entity,
                    state.frame,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.linkZ,
                    state.randomByteSupplier);
                return false;
              }
              return true;
            });
      }
    }
  }
}
