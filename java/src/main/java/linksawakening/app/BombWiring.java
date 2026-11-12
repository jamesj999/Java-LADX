package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import linksawakening.equipment.Bomb;
import linksawakening.equipment.ItemRegistry;
import linksawakening.state.PlayerState;

/** Registers the bomb and its room-facing adapter. */
final class BombWiring {
    private BombWiring() {}

    static void register(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                PlayerState.INVENTORY_BOMBS,
                new Bomb(
                        c.playerState(),
                        c.gameplaySoundSink(),
                        new RoomBombPlacement(c.link(), c.roomSession().playerItems()),
                        c.link()::canUseItems));
    }
}
