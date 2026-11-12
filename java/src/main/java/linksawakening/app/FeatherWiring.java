package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.RocsFeather;
import linksawakening.state.InventoryItem;

/** Registers the feather and its room-facing adapter. */
final class FeatherWiring {
    private FeatherWiring() {}

    static void register(EquipmentInputs c, ItemRegistry registry) {
        registry.register(InventoryItem.ROCS_FEATHER.romValue(), new RocsFeather(c.link()));
    }
}
