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

    @Test
    void replacesClosedChestsWithOpenChestsWhenTheRoomStatusBitIsSet() {
        byte[] rom = new byte[] {0x20, (byte) 0xA0, (byte) 0xFE};

        assertEquals(0xA0, new RoomObjectParser(rom)
            .parseOverworld(0, 0x00, 0)
            .objectAtLocation(0x20));
        assertEquals(0xA1, new RoomObjectParser(rom)
            .parseOverworld(0, 0x00, 0x10)
            .objectAtLocation(0x20));
    }

    @Test
    void recordsTheLastVisibleIndoorStaircaseInObjectStreamOrder() {
        byte[] rom = new byte[] {
            0x12, (byte) 0xBE,
            0x34, (byte) 0xCB,
            0x56, (byte) 0xC6,
            (byte) 0xFE
        };

        RoomObjectParseResult result = new RoomObjectParser(rom)
            .parseIndoor(0, 0x00, 0x10);

        assertEquals(0x56, result.staircaseLocation());
    }

    @Test
    void recordsTheLastStaircaseCellProducedByAnIndoorStrip() {
        byte[] rom = new byte[] {
            (byte) 0x83, 0x21, (byte) 0xC5,
            (byte) 0xFE
        };

        RoomObjectParseResult result = new RoomObjectParser(rom)
            .parseIndoor(0, 0x00, 0);

        assertEquals(0x23, result.staircaseLocation());
    }

    @Test
    void concealedHiddenStairsDoNotConfigureTheRoomStaircase() {
        byte[] rom = new byte[] {
            0x18, (byte) 0xBF,
            (byte) 0xFE
        };

        RoomObjectParseResult result = new RoomObjectParser(rom)
            .parseIndoor(0, 0x00, 0);

        assertEquals(-1, result.staircaseLocation());
        assertEquals(0x00, result.objectAtLocation(0x18));
    }
}
