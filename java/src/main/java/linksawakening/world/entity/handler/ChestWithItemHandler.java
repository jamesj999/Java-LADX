package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_CHEST_WITH_ITEM;
import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;

import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for ChestWithItem. */
final class ChestWithItemHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_CHEST_WITH_ITEM, EntityTickStage.PHASE_2_CHEST_WITH_ITEM, state -> {
            if (state.wasInitializing) {
                state.pickups().initializeChestEntity(state.entity);
                state.updated = withPositionAndVariant(
                        state.entity, state.entity.x(), (state.entity.y() - 0x08) & 0xFF, state.entity.spriteVariant());
            }
            return true;
        });
        registry.registerTick(ENTITY_CHEST_WITH_ITEM, EntityTickStage.PHASE_2_CHEST_WITH_ITEM_2, state -> {
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
