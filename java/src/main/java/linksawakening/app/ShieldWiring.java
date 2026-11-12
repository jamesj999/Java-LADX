package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Shield;
import linksawakening.state.PlayerState;

/** Registers the shield and its room-facing adapter. */
final class ShieldWiring {
    private ShieldWiring() {}

    static void register(EquipmentInputs c, ItemRegistry registry) {
        registry.register(PlayerState.INVENTORY_SHIELD, new Shield(c.gameplaySoundSink(), c.link()::canUseItems));
    }
}
