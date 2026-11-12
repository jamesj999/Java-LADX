package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.RocsFeather;
import linksawakening.state.PlayerState;

/** Registers the feather and its room-facing adapter. */
final class FeatherWiring {
    private FeatherWiring() {}

    static void register(EquipmentInputs c, ItemRegistry registry) {
        registry.register(PlayerState.INVENTORY_ROCS_FEATHER, new RocsFeather(c.link()));
    }
}
