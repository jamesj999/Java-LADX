package linksawakening.app;

import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.RocsFeather;
import linksawakening.state.InventoryItem;

/** Registers the feather and its room-facing adapter. */
final class FeatherWiring {
    private FeatherWiring() {}

    static void register(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.ROCS_FEATHER.romValue(), new RocsFeather(c.link().airborne()::useRocsFeather));
    }
}
