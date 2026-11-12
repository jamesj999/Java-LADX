package linksawakening.app.wiring;

import java.util.Objects;
import linksawakening.app.wiring.PlayerWiring.PlayerGraphics;
import linksawakening.entity.Link;
import linksawakening.equipment.Bomb;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.RocsFeather;
import linksawakening.equipment.Shield;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.rom.RomTables;
import linksawakening.state.InventoryItem;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.room.RoomSession;

/** Registers equipment in ROM order. Constructed only during session wiring. */
final class EquipmentWiring {
    private EquipmentWiring() {}

    static void shield(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.SHIELD.romValue(),
                new Shield(c.gameplaySoundSink(), c.link().equipment()::canUseItems));
    }

    static void feather(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.ROCS_FEATHER.romValue(), new RocsFeather(c.link().airborne()::useRocsFeather));
    }

    static void bomb(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.BOMBS.romValue(),
                new Bomb(
                        c.playerState(),
                        c.gameplaySoundSink(),
                        new RoomBombPlacement(c.link(), c.roomSession().playerItems()),
                        c.link().equipment()::canUseItems));
    }

    /** Dependencies shared by equipment target adapters; no session reference is retained. */
    record EquipmentInputs(
            PlayerState playerState,
            Link link,
            RoomSession roomSession,
            RomTables romTables,
            PlayerGraphics graphics,
            GameplaySoundSink gameplaySoundSink,
            RomRandomByteSource random) {
        EquipmentInputs {
            Objects.requireNonNull(playerState);
            Objects.requireNonNull(link);
            Objects.requireNonNull(roomSession);
            Objects.requireNonNull(romTables);
            Objects.requireNonNull(graphics);
            Objects.requireNonNull(gameplaySoundSink);
            Objects.requireNonNull(random);
        }
    }
}
