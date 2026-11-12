package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for Type0X35. */
final class Type0X35Handler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(0x35, EntityTickStage.PHASE_1_ENTRY, 0, Type0X35Handler::phase1Type0X35);
        registry.registerTick(
                0x35, EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE, 20, Type0X35Handler::phase1Type0X352);
    }

    private static boolean phase1Type0X35(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if ((state.roomInspection().entityRoomStatus() & 0x10) != 0) {
            state.lifecycle().disableEntityWithoutPersistence(slotState.entity().slot());
            return false;
        }
        return true;
    }

    private static boolean phase1Type0X352(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE
                && state.pickups().hasHeartPiecePickup(slotState.entity().slot())) {
            state.pickups()
                    .advanceHeartPiecePickup(
                            slotState.entity(), frameInput.linkEntityX(), frameInput.linkEntityY(), frameInput.linkZ());
            return false;
        }
        return true;
    }
}
