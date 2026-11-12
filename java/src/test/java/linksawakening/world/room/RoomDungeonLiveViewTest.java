package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import linksawakening.rom.RomSymbols;
import org.junit.jupiter.api.Test;

class RoomDungeonLiveViewTest {
    @Test
    void minimapViewTracksStatusWritesRoomChangesAndTowerCollapse() {
        byte[] bytes = new byte[0x100000];
        // Synthetic ROM setup before the map view reads its immutable layout.
        bytes[RomSymbols.MapLayout0.offset()] = 7;
        bytes[RomSymbols.MapLayout0.offset() + 6 * 64] = 8;
        bytes[RomSymbols.MapLayout12.offset()] = 9;
        bytes[RomSymbols.MapLayout11.offset()] = 10;
        var session = RoomRenderAllocationTest.room(bytes);
        var world = session.components.roomState();
        world.indoorARoomStatus[7] = 21;
        world.indoorBRoomStatus[8] = 22;
        world.indoorBRoomStatus[9] = 23;
        world.colorDungeonRoomStatus[10] = 24;
        var view = session.dungeon().view();
        assertEquals(21, view.minimapStatus(0));
        world.indoorARoomStatus[7] = 25;
        assertEquals(25, view.minimapStatus(0));
        roomMap(session, 6);
        assertEquals(22, view.minimapStatus(0));
        world.dungeonProgressFlags[6] = 4;
        assertEquals(23, view.minimapStatus(0));
        roomMap(session, 255);
        assertEquals(24, view.minimapStatus(0));
        byte[] snapshot = session.dungeon().dungeonMinimapRoomStatusSnapshot();
        for (int i = 0; i < snapshot.length; i++) assertEquals(Byte.toUnsignedInt(snapshot[i]), view.minimapStatus(i));
        session.components.location().activeRoom = null;
        assertEquals(0, view.minimapStatus(0));
        assertThrows(IndexOutOfBoundsException.class, () -> view.minimapStatus(64));
        assertThrows(IndexOutOfBoundsException.class, () -> view.progressFlag(8));
    }

    private void roomMap(RoomSession session, int map) {
        var current = session.rooms().activeRoom();
        session.components.location().activeRoom = ActiveRoom.from(new LoadedRoom(new LoadedRoomSpec(
                new LoadedRoomSpec.Identity(1, 1, map, 0),
                new LoadedRoomSpec.Objects(new int[256], null, null),
                new LoadedRoomSpec.Graphics(new int[320], new int[320], new int[8][4]),
                new LoadedRoomSpec.Exits(List.of(), false, 0, -1),
                current.entityView())));
    }
}
