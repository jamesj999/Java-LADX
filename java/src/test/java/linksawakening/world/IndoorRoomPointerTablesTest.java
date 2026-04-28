package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class IndoorRoomPointerTablesTest {

    @Test
    void selectsPointerTableFromMapId() {
        assertEquals(new RoomPointerTable(0x0A, 0x7B77), IndoorRoomPointerTables.forMap(0xFF));
        assertEquals(new RoomPointerTable(0x0B, 0x4000), IndoorRoomPointerTables.forMap(0x06));
        assertEquals(new RoomPointerTable(0x0B, 0x4000), IndoorRoomPointerTables.forMap(0x19));
        assertEquals(new RoomPointerTable(0x0A, 0x4000), IndoorRoomPointerTables.forMap(0x05));
        assertEquals(new RoomPointerTable(0x0A, 0x4000), IndoorRoomPointerTables.forMap(0x1A));
    }
}
