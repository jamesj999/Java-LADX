package linksawakening.app;

import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Shield;
import linksawakening.state.InventoryItem;

/** Registers the shield and its room-facing adapter. */
final class ShieldWiring {
    private ShieldWiring() {}

    static void register(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.SHIELD.romValue(),
                new Shield(c.gameplaySoundSink(), c.link().equipment()::canUseItems));
    }
}
