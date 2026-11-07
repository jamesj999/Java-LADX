package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_IRON_MASK;


import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

import static linksawakening.world.entity.EntitySnapshots.withDefinition;

/** ROM handler stages for IronMask. */
final class IronMaskHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        ENTITY_IRON_MASK,
        EntityTickStage.PHASE_3_IRON_MASK,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            int privateState2 = state.slot().ironMaskPrivateState2(state.entity.slot());
            int variant =
                privateState2 == 0 ? state.updated.spriteVariant() : ((state.frame >>> 4) & 0x01);
            if (state.sprites() != null) {
              state.updated =
                  withDefinition(
                      state.updated,
                      state.sprites().forIronMaskState(privateState2),
                      variant);
            }
            state.preserveIronMaskPresentation = true;
          }
          return true;
        });
  }
}
