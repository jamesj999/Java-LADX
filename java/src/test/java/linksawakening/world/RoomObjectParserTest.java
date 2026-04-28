package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RoomObjectParserTest {

    @Test
    void fillsOnlyActiveRoomCellsInsidePaddedRoomObjectsArea() {
        byte[] rom = new byte[] { (byte) 0xFE };

        RoomObjectParseResult result = new RoomObjectParser(rom)
            .parseOverworld(0, 0xE5, 0);

        assertEquals(0x100, result.roomObjectsArea().length);
        assertEquals(0xFF, result.roomObjectsArea()[0x00]);
        assertEquals(0xE5, result.roomObjectsArea()[0x11]);
        assertEquals(0xE5, result.roomObjectsArea()[0x1A]);
        assertEquals(0xFF, result.roomObjectsArea()[0x1B]);
    }

    @Test
    void parsesOverworldOrdinaryObjectsStripsAndWarpRecords() {
        byte[] rom = new byte[] {
            0x23, 0x44,
            (byte) 0x82, 0x30, 0x55,
            (byte) 0xE1, 0x10, 0x77, 0x28, 0x48,
            (byte) 0xFE
        };

        RoomObjectParseResult result = new RoomObjectParser(rom)
            .parseOverworld(0, 0x00, 0);

        assertEquals(0x44, result.objectAtLocation(0x23));
        assertEquals(0x55, result.objectAtLocation(0x30));
        assertEquals(0x55, result.objectAtLocation(0x31));
        assertEquals(1, result.warps().size());
        Warp warp = result.warps().get(0);
        assertEquals(Warp.CATEGORY_INDOOR, warp.category());
        assertEquals(0x10, warp.destMap());
        assertEquals(0x77, warp.destRoom());
        assertEquals(0x28, warp.destX());
        assertEquals(0x48, warp.destY());
    }
}
