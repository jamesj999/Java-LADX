package linksawakening.app;

import linksawakening.equipment.Bomb;
import linksawakening.equipment.ItemRegistry;
import linksawakening.state.InventoryItem;

/** Registers the bomb and its room-facing adapter. */
final class BombWiring {
    private BombWiring() {}

    static void register(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.BOMBS.romValue(),
                new Bomb(
                        c.playerState(),
                        c.gameplaySoundSink(),
                        new RoomBombPlacement(c.link(), c.roomSession().playerItems()),
                        c.link()::canUseItems));
    }
}
