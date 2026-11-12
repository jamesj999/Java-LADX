package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import linksawakening.rom.Rom;
import linksawakening.rom.RomBank;
import linksawakening.testing.TestRoms;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
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

        RoomTilemap tilemap =
                new RoomTilemapBuilder((rom == null ? (Rom) null : TestRoms.partial(rom))).buildOverworld(0, objects);

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

        RoomTilemap result = new RoomTilemapBuilder((rom == null ? (Rom) null : TestRoms.partial(rom)))
                .buildIndoor(0xFF, 0x00, objects);

        assertEquals(0x66, result.tileIds()[0]);
        assertEquals(0x57, result.tileAttrs()[0]);
    }

    @Test
    void shippedCameraShopUsesColorDungeonTilesWithOrdinaryIndoorAttributes() throws Exception {
        byte[] rom = loadRom();
        int objectId = 0x40;
        int[] objects = new int[RoomConstants.ROOM_OBJECTS_AREA_SIZE];
        Arrays.fill(objects, 0x100);
        objects[RoomConstants.ROOM_OBJECTS_BASE] = objectId;

        RoomTilemap result = new RoomTilemapBuilder((rom == null ? (Rom) null : TestRoms.partial(rom)))
                .buildIndoor(0x10, 0xB5, objects);

        int colorTile = RomBank.romOffset(0x08, 0x4760) + objectId * 4;
        int pointer = RomBank.romOffset(0x1A, 0x6276 + 0x1FE);
        int attrAddress = Byte.toUnsignedInt(rom[pointer]) | (Byte.toUnsignedInt(rom[pointer + 1]) << 8);
        int ordinaryAttr = RomBank.romOffset(0x24, attrAddress) + objectId * 4;
        assertEquals(Byte.toUnsignedInt(rom[colorTile]), result.tileIds()[0]);
        assertEquals(Byte.toUnsignedInt(rom[ordinaryAttr]), result.tileAttrs()[0]);
    }

    @Test
    void indoorAttributeGroupsFollowMapIdsAndRomRoomExceptions() throws Exception {
        byte[] rom = loadRom();
        // map, room, pointer table, group byte offset, attribute bank.
        // GetBGAttributesAddressForObject, $1A:65B2..6633.
        int[][] cases = {
            {0x00, 0x17, 0x6076, 0x00, 0x23},
            {0x06, 0x40, 0x6076, 0x0C, 0x23},
            {0x07, 0x64, 0x6076, 0x0E, 0x23},
            {0x08, 0x70, 0x6076, 0x10, 0x23},
            {0x0A, 0xBD, 0x6276, 0x14, 0x24},
            {0x0A, 0xAB, 0x6276, 0x14, 0x24},
            {0x0A, 0xFD, 0x6276, 0x1E, 0x24},
            {0x11, 0xC0, 0x6276, 0x1E, 0x24},
            {0x11, 0xC1, 0x6276, 0x1E, 0x24},
            {0x0F, 0xA0, 0x6276, 0x00, 0x24},
            {0x1F, 0xEB, 0x6276, 0x28, 0x24},
            {0x1F, 0xEC, 0x6276, 0x28, 0x24},
            {0x10, 0xE9, 0x6276, 0x26, 0x24},
            {0x10, 0xA3, 0x6276, 0x20, 0x24},
            {0x16, 0x6F, 0x6276, 0x2C, 0x23},
            {0x16, 0x7F, 0x6276, 0x2C, 0x23},
            {0x16, 0x8F, 0x6276, 0x2C, 0x23},
            {0x16, 0x8E, 0x6276, 0x2C, 0x24},
        };
        for (int[] c : cases) {
            int pointer = RomBank.romOffset(0x1A, c[2] + c[3]);
            int address = Byte.toUnsignedInt(rom[pointer]) | Byte.toUnsignedInt(rom[pointer + 1]) << 8;
            int source = RomBank.romOffset(c[4], address);
            // Exercise the complete ordinary indoor object table, not just
            // a floor whose palette happens to match in several groups.
            for (int object = 0; object < 0xEC; object++) {
                int[] objects = new int[RoomConstants.ROOM_OBJECTS_AREA_SIZE];
                Arrays.fill(objects, 0x100);
                objects[RoomConstants.ROOM_OBJECTS_BASE] = object;
                var result = new RoomTilemapBuilder((rom == null ? (Rom) null : TestRoms.partial(rom)))
                        .buildIndoor(c[0], c[1], objects);
                for (int tile = 0; tile < 4; tile++) {
                    int index = tile / 2 * RoomConstants.ROOM_TILE_WIDTH + tile % 2;
                    assertEquals(
                            Byte.toUnsignedInt(rom[source + object * 4 + tile]),
                            result.tileAttrs()[index],
                            "map %02X room %02X object %02X tile %d".formatted(c[0], c[1], object, tile));
                }
            }
        }
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

    private static byte[] loadRom() throws Exception {
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        }
    }
}
