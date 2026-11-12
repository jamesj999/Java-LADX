package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_HIDING_SLIME_KEY;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime.SlimeKeyTransitionUpdate;
import linksawakening.world.entity.event.DialogRequest;

/** ROM handler stages for HidingSlimeKey. */
final class HidingSlimeKeyHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_HIDING_SLIME_KEY,
                EntityTickStage.PHASE_2_HIDING_SLIME_KEY,
                HidingSlimeKeyHandler::phase2HidingSlimeKey);
        registry.registerTick(
                ENTITY_HIDING_SLIME_KEY,
                EntityTickStage.PHASE_2_HIDING_SLIME_KEY_2,
                HidingSlimeKeyHandler::phase2HidingSlimeKey2);
        registry.registerTick(
                ENTITY_HIDING_SLIME_KEY,
                EntityTickStage.PHASE_2_HIDING_SLIME_KEY_3,
                HidingSlimeKeyHandler::phase2HidingSlimeKey3);
    }

    private static boolean phase2HidingSlimeKey(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE
                && !state.wasInitializing
                && (state.roomInspection().entityRoomStatus() & 0x10) != 0) {
            // HidingSlimeKeyEntityHandler checks ROOM_STATUS_EVENT_1 before
            // calling the shared reveal helper and unloads without touching
            // the source persistence mask.
            state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
            return false;
        }
        return true;
    }

    private static boolean phase2HidingSlimeKey2(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            int slot = state.entity.slot();
            SlimeKeyTransitionUpdate keyUpdate = state.pickups()
                    .advanceHidingSlimeKey(state.updated, slot, state.linkEntityX, state.linkEntityY, state.linkZ);
            state.slot().setTransitionCountdown(slot, keyUpdate.nextTransitionCountdown());
            if (keyUpdate.reward() != null) {
                state.emit(keyUpdate.reward());
                state.emit(new DialogRequest(0, keyUpdate.reward().dialogLowId()));
            }
            if (keyUpdate.unload()) {
                state.lifecycle().disableEntityWithoutPersistence(slot);
                return false;
            }
            state.keyDropTransitionActive = keyUpdate.holdAboveLink();
            state.updated = keyUpdate.entity();
        }
        return true;
    }

    private static boolean phase2HidingSlimeKey3(EntityFrameExecution state) {
        if ((state.roomInspection().entityRoomStatus() & 0x10) != 0) {
            // The handler's first instruction is the completed-room
            // gate, including the INIT dispatch path.
            state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
            return false;
        }
        return true;
    }
}
