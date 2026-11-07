package linksawakening.world;

import linksawakening.world.RoomEntityRuntime.SlimeKeyTransitionUpdate;

import linksawakening.world.entity.event.DialogRequest;

/** ROM handler stages for HidingSlimeKey. */
final class HidingSlimeKeyHandler {
  static void installPhase2(EntityHandlerRegistry registry) {
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
            state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
            return false;
          }
          return true;
        });
  }

  static void installPhase2Part2(EntityHandlerRegistry registry) {
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
              state.runtime.pendingSlimeKeyRewardEvents.add(keyUpdate.reward());
              state.runtime.pendingDialogRequests.add(
                  new DialogRequest(0, keyUpdate.reward().dialogLowId()));
            }
            if (keyUpdate.unload()) {
              state.runtime.lifecycle.disableEntityWithoutPersistence(slot);
              return false;
            }
            state.keyDropTransitionActive = keyUpdate.holdAboveLink();
            state.updated = keyUpdate.entity();
          }
          return true;
        });
  }

  static void installPhase2Part3(EntityHandlerRegistry registry) {
    for (int type = 0; type < 256; type++) {
      if ((type == RoomEntityRuntime.ENTITY_HIDING_SLIME_KEY)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_HIDING_SLIME_KEY_3,
            state -> {
              if ((state.runtime.entityRoomStatus & 0x10) != 0) {
                // The handler's first instruction is the completed-room
                // gate, including the INIT dispatch path.
                state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                return false;
              }
              return true;
            });
      }
    }
  }
}
