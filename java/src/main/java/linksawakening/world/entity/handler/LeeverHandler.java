package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_LEEVER;


import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityHandlerRegistration;

import static linksawakening.world.entity.EntitySnapshots.withVariant;

/** ROM handler stages for Leever. */
final class LeeverHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        ENTITY_LEEVER,
        EntityTickStage.PHASE_2_LEEVER,
        state -> {
          if (state.wasInitializing) {
            // EntityInitLeever calls SetEntitySpriteVariant($FF) before
            // the first active handler dispatch.
            state.updated = withVariant(state.entity, -1);
          }
          return true;
        });
  }
}
