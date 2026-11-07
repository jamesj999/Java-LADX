package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;







import static linksawakening.world.EntityTypeRules.isGhiniType;
import static linksawakening.world.EntityTypeRules.isHidingGhiniType;

/** ROM handler stages for GhiniType. */
final class GhiniTypeHandler {
  static void install(EntityHandlerRegistration registry) {
    for (int type = 0; type < 256; type++) {
      if (((isGhiniType(type)))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_GHINI_TYPE,
            state -> {
              {
                state.runtime.ghiniMotion.initialize(state.entity.slot(), state.entity.type());
                if (isHidingGhiniType(state.entity.type())) {
                  state.preserveGhiniPresentation = true;
                }
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((isGhiniType(type))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_4_GHINI_TYPE,
            state -> {
              if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
                state.updated =
                    state.runtime.ghiniMotion.advance(
                        state.entity,
                        state.frame,
                        state.entity.type(),
                        state.linkEntityX,
                        state.linkEntityY,
                        state.romCollisionType,
                        state.randomByteSupplier);
                state.preserveGhiniPresentation = true;
              }
              return true;
            });
      }
    }
  }
}
