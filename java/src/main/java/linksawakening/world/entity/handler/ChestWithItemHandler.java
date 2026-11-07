package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;

/** ROM handler stages for ChestWithItem. */
final class ChestWithItemHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_CHEST_WITH_ITEM,
        EntityTickStage.PHASE_2_CHEST_WITH_ITEM,
        state -> {
          if (state.wasInitializing) {
            state.pickups().initializeChestEntity(state.entity);
            state.updated =
                withPositionAndVariant(
                    state.entity,
                    state.entity.x(),
                    (state.entity.y() - 0x08) & 0xFF,
                    state.entity.spriteVariant());
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_CHEST_WITH_ITEM,
        EntityTickStage.PHASE_2_CHEST_WITH_ITEM_2,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            if (state.pickups().advanceChestEntity(state.index, state.entity)) {
              return false;
            }
            state.updated = state.entityAt(state.index);
          }
          return true;
        });
  }
}
