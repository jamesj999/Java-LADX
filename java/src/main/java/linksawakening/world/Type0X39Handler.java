package linksawakening.world;



/** ROM handler stages for Type0X39. */
final class Type0X39Handler {
  static void install(EntityHandlerRegistry registry) {
    for (int type = 0; type < 256; type++) {
      if ((type == 0x39)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_TYPE_0X39,
            state -> {
              if (state.status == EntityStatus.ACTIVE

                  && state.runtime.instrumentParticleKind[state.entity.slot()] == 0
                  && state.runtime.instrumentPickupState[state.entity.slot()] < 3
                  && state.runtime.transitionSequenceCounter == 4
                  && state.runtime.spriteSelection != null) {
                int[][] palettes = state.runtime.spriteSelection.objectPalettes();
                int offset = ((state.frame >> 1) & 0x3C) / 2;
                palettes[1][2] = state.runtime.instrumentPaletteCycle[offset];
                palettes[1][3] = state.runtime.instrumentPaletteCycle[offset + 1];
                state.runtime.spriteSelection =
                    state.runtime.spriteSelection.withObjectPalettes(palettes);
              }
              return true;
            });
      }
    }
  }
}
