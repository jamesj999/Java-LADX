package linksawakening.world;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

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

    @Test
    void colorDungeonUsesDedicatedObjectTileAndAttributeTables() {
        byte[] rom = syntheticRom();
        int objectId = 0x22;
        int[] objects = new int[RoomConstants.ROOM_OBJECTS_AREA_SIZE];
        Arrays.fill(objects, 0x100);
        objects[RoomConstants.ROOM_OBJECTS_BASE] = objectId;

        int ordinary = RomBank.romOffset(0x08, 0x43B0) + objectId * 4;
        rom[ordinary] = 0x11;
        int colorTile = RomBank.romOffset(0x08, 0x4760) + objectId * 4;
        rom[colorTile] = 0x66;
        int colorAttr = RomBank.romOffset(0x23, 0x6000) + objectId * 4;
        rom[colorAttr] = 0x57;

        RoomTilemap result = new RoomTilemapBuilder(rom).buildIndoor(0xFF, 0x00, objects);

        assertEquals(0x66, result.tileIds()[0]);
        assertEquals(0x57, result.tileAttrs()[0]);
    }

    private static byte[] ensureLength(byte[] bytes, int length) {
        if (bytes.length >= length) {
            return bytes;
        }
        byte[] out = new byte[length];
        System.arraycopy(bytes, 0, out, 0, bytes.length);
        return out;
    }

    private static byte[] syntheticRom() {
        return new byte[RomBank.romOffset(0x26, 0x4000) + 0x50];
    }
}
