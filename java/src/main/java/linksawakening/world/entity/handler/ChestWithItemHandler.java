package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_CHEST_WITH_ITEM;
import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for ChestWithItem. */
final class ChestWithItemHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_CHEST_WITH_ITEM,
                EntityTickStage.PHASE_2_BEFORE_RECOIL,
                1,
                ChestWithItemHandler::phase2ChestWithItem);
        registry.registerTick(
                ENTITY_CHEST_WITH_ITEM,
                EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION,
                1,
                ChestWithItemHandler::phase2ChestWithItem2);
    }

    private static boolean phase2ChestWithItem(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (slotState.wasInitializing()) {
            state.pickups().initializeChestEntity(slotState.entity());
            slotState.updated(withPositionAndVariant(
                    slotState.entity(),
                    slotState.entity().x(),
                    (slotState.entity().y() - 0x08) & 0xFF,
                    slotState.entity().spriteVariant()));
        }
        return true;
    }

    private static boolean phase2ChestWithItem2(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            if (state.pickups().advanceChestEntity(slotState.index(), slotState.entity())) {
                return false;
            }
            slotState.updated(state.entityAt(slotState.index()));
        }
        return true;
    }
}
