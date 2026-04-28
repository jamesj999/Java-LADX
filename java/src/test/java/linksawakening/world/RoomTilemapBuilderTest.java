package linksawakening.world;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RoomTilemapBuilderTest {

    @Test
    void overworldUsesGbcOverlayValuesForTileAndAttributeLookups() {
        byte[] rom = new byte[RomBank.romOffset(0x26, 0x4000) + 0x50];
        int tileTable = RomBank.romOffset(0x1A, 0x6B1D);
        rom = ensureLength(rom, tileTable + 0x100 * 4);
        int attrBankTable = RomBank.romOffset(0x1A, 0x6476);
        int attrPtrTable = RomBank.romOffset(0x1A, 0x5E76);
        int attrTable = RomBank.romOffset(0x21, 0x5000);
        rom = ensureLength(rom, attrTable + 0x100 * 4);

        rom[RomBank.romOffset(0x26, 0x4000)] = 0x22;
        rom[tileTable + 0x22 * 4] = 0x11;
        rom[tileTable + 0x22 * 4 + 1] = 0x12;
        rom[attrBankTable] = 0x01;
        rom[attrPtrTable] = 0x00;
        rom[attrPtrTable + 1] = 0x50;
        rom[attrTable + 0x22 * 4] = 0x03;

        int[] objects = new int[RoomConstants.ROOM_OBJECTS_AREA_SIZE];
        objects[RoomConstants.ROOM_OBJECTS_BASE] = 0x99;

        RoomTilemap tilemap = new RoomTilemapBuilder(rom).buildOverworld(0, objects);

        assertEquals(0x22, tilemap.renderValues()[RoomConstants.ROOM_OBJECTS_BASE]);
        assertEquals(0x11, tilemap.tileIds()[0]);
        assertEquals(0x12, tilemap.tileIds()[1]);
        assertEquals(0x03, tilemap.tileAttrs()[0]);
    }

    private static byte[] ensureLength(byte[] bytes, int length) {
        if (bytes.length >= length) {
            return bytes;
        }
        byte[] out = new byte[length];
        System.arraycopy(bytes, 0, out, 0, bytes.length);
        return out;
    }
}
