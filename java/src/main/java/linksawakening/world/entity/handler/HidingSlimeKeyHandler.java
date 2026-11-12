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
                EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION,
                3,
                HidingSlimeKeyHandler::phase2HidingSlimeKey);
        registry.registerTick(
                ENTITY_HIDING_SLIME_KEY,
                EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION,
                5,
                HidingSlimeKeyHandler::phase2HidingSlimeKey2);
        registry.registerTick(
                ENTITY_HIDING_SLIME_KEY,
                EntityTickStage.PHASE_2_BEFORE_INITIALIZE,
                0,
                HidingSlimeKeyHandler::phase2HidingSlimeKey3);
    }

    private static boolean phase2HidingSlimeKey(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && (state.roomInspection().entityRoomStatus() & 0x10) != 0) {
            // HidingSlimeKeyEntityHandler checks ROOM_STATUS_EVENT_1 before
            // calling the shared reveal helper and unloads without touching
            // the source persistence mask.
            state.lifecycle().disableEntityWithoutPersistence(slotState.entity().slot());
            return false;
        }
        return true;
    }

    private static boolean phase2HidingSlimeKey2(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            int slot = slotState.entity().slot();
            SlimeKeyTransitionUpdate keyUpdate = state.pickups()
                    .advanceHidingSlimeKey(
                            slotState.updated(),
                            slot,
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.linkZ());
            state.slot().setTransitionCountdown(slot, keyUpdate.nextTransitionCountdown());
            if (keyUpdate.reward() != null) {
                state.emit(keyUpdate.reward());
                state.emit(new DialogRequest(0, keyUpdate.reward().dialogLowId()));
            }
            if (keyUpdate.unload()) {
                state.lifecycle().disableEntityWithoutPersistence(slot);
                return false;
            }
            slotState.keyDropTransitionActive(keyUpdate.holdAboveLink());
            slotState.updated(keyUpdate.entity());
        }
        return true;
    }

    private static boolean phase2HidingSlimeKey3(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if ((state.roomInspection().entityRoomStatus() & 0x10) != 0) {
            // The handler's first instruction is the completed-room
            // gate, including the INIT dispatch path.
            state.lifecycle().disableEntityWithoutPersistence(slotState.entity().slot());
            return false;
        }
        return true;
    }
}
