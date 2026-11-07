package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

/** ROM handler stages for Type0X39. */
final class Type0X39Handler {
  static void install(EntityHandlerRegistration registry) {
    for (int type = 0; type < 256; type++) {
      if ((type == 0x39)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_TYPE_0X39,
            state -> {
              state.rewards().refreshInstrumentPalette(state.status, state.entity.slot(), state.frame);
              return true;
            });
      }
    }
  }
}
