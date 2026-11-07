package linksawakening.world;

import static linksawakening.world.EntitySnapshots.withDefinition;

/** ROM handler stages for SwordShieldPickup. */
final class SwordShieldPickupHandler {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_SWORD_SHIELD_PICKUP,
        EntityTickStage.PHASE_1_SWORD_SHIELD_PICKUP,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            if (state.runtime.chestSwordLevel == 0 && state.runtime.spriteHandlers != null) {
              state.entity =
                  withDefinition(
                      state.entity,
                      state.runtime.spriteHandlers.forSwordShieldPickup(true),
                      state.entity.spriteVariant());
              state.replaceEntity(state.entity);
            }
            if (state.runtime.swordPickupSequenceActive[state.entity.slot()]) {
              state.runtime.rewards.advanceSwordPickup(
                  state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.linkZ);
              return false;
            }
          }
          return true;
        });
  }
}
