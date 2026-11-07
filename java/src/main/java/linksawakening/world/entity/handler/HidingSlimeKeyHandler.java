package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;







import linksawakening.world.RoomEntityRuntime.SlimeKeyTransitionUpdate;

import linksawakening.world.entity.event.DialogRequest;

/** ROM handler stages for HidingSlimeKey. */
final class HidingSlimeKeyHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_HIDING_SLIME_KEY,
        EntityTickStage.PHASE_2_HIDING_SLIME_KEY,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing

              && (state.runtime.entityRoomStatus & 0x10) != 0) {
            // HidingSlimeKeyEntityHandler checks ROOM_STATUS_EVENT_1 before
            // calling the shared reveal helper and unloads without touching
            // the source persistence mask.
            state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
            return false;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_HIDING_SLIME_KEY,
        EntityTickStage.PHASE_2_HIDING_SLIME_KEY_2,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            int slot = state.entity.slot();
            SlimeKeyTransitionUpdate keyUpdate =
                state.runtime.pickups.advanceHidingSlimeKey(
                    state.updated, slot, state.linkEntityX, state.linkEntityY, state.linkZ);
            state.runtime.enemyTransitionCountdown[slot] = keyUpdate.nextTransitionCountdown();
            if (keyUpdate.reward() != null) {
              state.emit(keyUpdate.reward());
              state.emit(
                  new DialogRequest(0, keyUpdate.reward().dialogLowId()));
            }
            if (keyUpdate.unload()) {
              state.lifecycle().disableEntityWithoutPersistence(slot);
              return false;
            }
            state.keyDropTransitionActive = keyUpdate.holdAboveLink();
            state.updated = keyUpdate.entity();
          }
          return true;
        });
    for (int type = 0; type < 256; type++) {
      if ((type == RoomEntityRuntime.ENTITY_HIDING_SLIME_KEY)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_HIDING_SLIME_KEY_3,
            state -> {
              if ((state.runtime.entityRoomStatus & 0x10) != 0) {
                // The handler's first instruction is the completed-room
                // gate, including the INIT dispatch path.
                state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
                return false;
              }
              return true;
            });
      }
    }
  }
}
