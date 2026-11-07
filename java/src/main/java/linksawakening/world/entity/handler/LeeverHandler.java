package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.entity.EntityHandlerRegistration;







import static linksawakening.world.EntitySnapshots.withVariant;

/** ROM handler stages for Leever. */
final class LeeverHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_LEEVER,
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
