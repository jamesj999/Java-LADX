package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

import static linksawakening.world.entity.EntitySnapshots.withDefinition;

/** ROM handler stages for SwordShieldPickup. */
final class SwordShieldPickupHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_SWORD_SHIELD_PICKUP,
        EntityTickStage.PHASE_1_SWORD_SHIELD_PICKUP,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            if (state.runtime.chestSwordLevel == 0 && state.sprites() != null) {
              state.entity =
                  withDefinition(
                      state.entity,
                      state.sprites().forSwordShieldPickup(true),
                      state.entity.spriteVariant());
              state.replaceEntity(state.entity);
            }
            if (state.runtime.swordPickupSequenceActive[state.entity.slot()]) {
              state.rewards().advanceSwordPickup(
                  state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.linkZ);
              return false;
            }
          }
          return true;
        });
  }
}
