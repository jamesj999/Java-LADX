package linksawakening.world.room;

import static linksawakening.world.room.RoomTransitionTestSupport.*;
import static linksawakening.world.room.RoomTransitionTestSupport2.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.world.transition.ScrollController;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class RoomTransitionFollowersTest {

    @Test
    void bowWowHideoutDirectRoomIdsMoveNorthByOneHexRow() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(romTables);
        RoomSession session = newSession(rom, romTables, collision);
        session.loadIndoor(0x15, 0xF0);

        ScrollController scroll = new ScrollController();
        session.transitions().startAdjacentIndoorScroll(scroll, ScrollController.UP, 0x50, 0x00);

        assertEquals(0xE0, session.currentRoomId());
        assertEquals(
                4,
                session.activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x0B)
                        .count());
    }
}
