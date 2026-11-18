package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityFrameInputLatch;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityPickupController;
import linksawakening.world.entity.EntityRoomInspection;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntitySlotLifecycle;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for Type0X35. */
final class Type0X35Handler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(
                0x35,
                EntityTickStage.PHASE_1_ENTRY,
                0,
                frame -> phase1Type0X35(frame.slotFrame(), frame.roomInspection(), frame.lifecycle()));
        registry.registerTick(
                0x35,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                20,
                frame -> phase1Type0X352(frame.slotFrame(), frame.inputs(), frame.pickups()));
    }

    private static boolean phase1Type0X35(
            EntitySlotExecution slotState, EntityRoomInspection roomInspection, EntitySlotLifecycle lifecycle) {
        if ((roomInspection.entityRoomStatus() & 0x10) != 0) {
            lifecycle.disableEntityWithoutPersistence(slotState.entity().slot());
            return false;
        }
        return true;
    }

    private static boolean phase1Type0X352(
            EntitySlotExecution slotState, EntityFrameInputLatch frameInput, EntityPickupController pickups) {
        if (slotState.status() == EntityStatus.ACTIVE
                && pickups.hasHeartPiecePickup(slotState.entity().slot())) {
            pickups.advanceHeartPiecePickup(
                    slotState.entity(), frameInput.linkEntityX(), frameInput.linkEntityY(), frameInput.linkZ());
            return false;
        }
        return true;
    }
}
