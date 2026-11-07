package linksawakening.world;

import static linksawakening.world.EntitySnapshots.withDefinition;

/** ROM handler stages for IronMask. */
final class IronMaskHandler {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_IRON_MASK,
        EntityTickStage.PHASE_3_IRON_MASK,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            int privateState2 = state.runtime.ironMaskPrivateState2[state.entity.slot()];
            int variant =
                privateState2 == 0 ? state.updated.spriteVariant() : ((state.frame >>> 4) & 0x01);
            if (state.runtime.spriteHandlers != null) {
              state.updated =
                  withDefinition(
                      state.updated,
                      state.runtime.spriteHandlers.forIronMaskState(privateState2),
                      variant);
            }
            state.preserveIronMaskPresentation = true;
          }
          return true;
        });
  }
}
