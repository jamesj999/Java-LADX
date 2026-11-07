package linksawakening.world;



/** ROM handler stages for Type0X35. */
final class Type0X35Handler {
  static void installPhase1(EntityHandlerRegistry registry) {
    for (int type = 0; type < 256; type++) {
      if ((type == 0x35)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_TYPE_0X35,
            state -> {
              if ((state.runtime.entityRoomStatus & 0x10) != 0) {
                state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                return false;
              }
              return true;
            });
      }
    }
  }

  static void installPhase1Part2(EntityHandlerRegistry registry) {
    for (int type = 0; type < 256; type++) {
      if ((type == 0x35)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_TYPE_0X35_2,
            state -> {
              if (state.status == EntityStatus.ACTIVE

                  && state.runtime.heartPiecePickup[state.entity.slot()] != null) {
                state.runtime.pickups.advanceHeartPiecePickup(
                    state.entity, state.linkEntityX, state.linkEntityY, state.linkZ);
                return false;
              }
              return true;
            });
      }
    }
  }
}
