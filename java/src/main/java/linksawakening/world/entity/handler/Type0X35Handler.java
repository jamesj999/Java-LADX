package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;

/** ROM handler stages for Type0X35. */
final class Type0X35Handler {
  static void install(EntityHandlerRegistration registry) {
    for (int type = 0; type < 256; type++) {
      if ((type == 0x35)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_TYPE_0X35,
            state -> {
              if ((state.roomInspection().entityRoomStatus() & 0x10) != 0) {
                state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
                return false;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((type == 0x35)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_TYPE_0X35_2,
            state -> {
              if (state.status == EntityStatus.ACTIVE

                  && state.pickups().hasHeartPiecePickup(state.entity.slot())) {
                state.pickups().advanceHeartPiecePickup(
                    state.entity, state.linkEntityX, state.linkEntityY, state.linkZ);
                return false;
              }
              return true;
            });
      }
    }
  }
}
