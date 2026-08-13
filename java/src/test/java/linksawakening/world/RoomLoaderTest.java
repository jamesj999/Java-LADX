package linksawakening.world;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RoomLoaderTest {

    @Test
    void loadedRoomKeepsThePreStaircaseConstructorSignature() {
        LoadedRoom room = new LoadedRoom(
            0, Warp.CATEGORY_INDOOR, 0, 0,
            new int[0x100], null, null, new int[0], new int[0], null,
            List.of(), false, 0x03, null);

        assertEquals(0x03, room.shutterDoorMask());
        assertEquals(-1, room.staircaseLocation());
    }

    @Test
    void overworldLoaderReadsPointerHeaderObjectsTilemapAndPalette() {
        byte[] rom = new byte[0xA0000];
        int pointer = RomBank.romOffset(0x09, 0x4000);
        rom[pointer] = 0x00;
        rom[pointer + 1] = 0x42;
        int room = RomBank.romOffset(0x09, 0x4200);
        rom[room] = 0x0B;
        rom[room + 1] = (byte) 0xE5;
        rom[room + 2] = (byte) 0xFE;
        int entityPointer = RomBank.romOffset(0x16, 0x4000);
        rom[entityPointer] = 0x00;
        rom[entityPointer + 1] = 0x50;
        int entityStream = RomBank.romOffset(0x16, 0x5000);
        rom[entityStream] = (byte) 0xFF;

        LoadedRoom loaded = new RoomLoader(rom).loadOverworld(0);

        assertEquals(0, loaded.roomId());
        assertEquals(Warp.CATEGORY_OVERWORLD, loaded.mapCategory());
        assertEquals(0x0B, loaded.animatedTilesGroup());
        assertEquals(0xE5, loaded.roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE]);
        assertEquals(RoomConstants.ROOM_TILE_WIDTH * RoomConstants.ROOM_TILE_HEIGHT, loaded.tileIds().length);
    }

    @Test
    void indoorLoaderCanPreserveSideScrollingMapCategory() {
        LoadedRoom loaded = new RoomLoader(loadRom()).loadIndoor(
            0x00, 0x00, null, Warp.CATEGORY_SIDESCROLL);

        assertEquals(Warp.CATEGORY_SIDESCROLL, loaded.mapCategory());
    }

    @Test
    void overworldLoaderAttachesRomBackedEntitySnapshotAndSpriteSelection() {
        LoadedRoom loaded = new RoomLoader(loadRom()).loadOverworld(0x92);

        assertEquals(7, loaded.entities().loadedEntities().size());
        assertEquals(0x73, loaded.entities().loadedEntities().get(0).type());
        assertEquals(0x43, loaded.entities().spriteSelection().groupIndex());
        assertArrayEquals(new int[] { 0xA4, 0xE5, 0xE6, 0xDC },
            loaded.entities().spriteSelection().sheetValues());
    }

    @Test
    void ordinaryOverworldRoomAppendsItsTwoRomObjectPalettes() {
        byte[] rom = loadRom();

        int[][] palettes = new RoomLoader(rom).loadOverworld(0x00)
            .entities().spriteSelection().objectPalettes();

        assertEquals(8, palettes.length);
        assertArrayEquals(readPalette(rom, 0x21, 0x5BD0), palettes[6]);
        assertArrayEquals(readPalette(rom, 0x21, 0x5BD8), palettes[7]);
    }

    @Test
    void mabeVillageSquareRewritesObjectPaletteEightColorsOneThroughThree() {
        byte[] rom = loadRom();
        int[] rawRoomPalette = readPalette(rom, 0x21, 0x56D8);
        int[] specialColors = readPalette(rom, 0x21, 0x56C8);

        int[] palette = new RoomLoader(rom).loadOverworld(0x92)
            .entities().spriteSelection().objectPalettes()[7];

        assertArrayEquals(new int[] {
            rawRoomPalette[0], specialColors[3], specialColors[1], specialColors[0]
        }, palette);
    }

    @Test
    void ordinaryIndoorRoomAppendsItsTwoRomObjectPalettes() {
        byte[] rom = loadRom();

        int[][] palettes = new RoomLoader(rom).loadIndoor(0x10, 0xA4, null)
            .entities().spriteSelection().objectPalettes();

        assertEquals(8, palettes.length);
        assertArrayEquals(readPalette(rom, 0x21, 0x69F0), palettes[6]);
        assertArrayEquals(readPalette(rom, 0x21, 0x69F8), palettes[7]);
    }

    @Test
    void newGameHouseLoadsMarinAndTarinFromTheRomEntityStream() {
        LoadedRoom loaded = new RoomLoader(loadRom()).loadIndoor(0x10, 0xA3, null);

        assertEquals(4, loaded.entities().loadedEntities().size());
        assertEquals(0x3E, loaded.entities().loadedEntities().get(0).type());
        assertEquals(0x3F, loaded.entities().loadedEntities().get(1).type());
        assertEquals(0x58, loaded.entities().loadedEntities().get(0).x());
        assertEquals(0x40, loaded.entities().loadedEntities().get(0).y());
        assertEquals(0x78, loaded.entities().loadedEntities().get(1).x());
        assertEquals(0x50, loaded.entities().loadedEntities().get(1).y());
        assertTrue(loaded.entities().loadedEntities().get(1).spriteDefinition().supported());
    }

    @Test
    void ordinaryDungeonRoomAppendsItsTwoRomObjectPalettes() {
        byte[] rom = loadRom();

        int[][] palettes = new RoomLoader(rom).loadIndoor(0x00, 0x25, null)
            .entities().spriteSelection().objectPalettes();

        assertEquals(8, palettes.length);
        assertArrayEquals(readPalette(rom, 0x21, 0x6020), palettes[6]);
        assertArrayEquals(readPalette(rom, 0x21, 0x6028), palettes[7]);
    }

    @Test
    void sideScrollingDungeonRoomUsesDungeonPaletteBObjectRows() {
        byte[] rom = loadRom();

        int[][] palettes = new RoomLoader(rom).loadIndoor(
            0x00, 0x00, null, Warp.CATEGORY_SIDESCROLL)
            .entities().spriteSelection().objectPalettes();

        assertArrayEquals(readPalette(rom, 0x21, 0x6070), palettes[6]);
        assertArrayEquals(readPalette(rom, 0x21, 0x6078), palettes[7]);
    }

    @Test
    void turtleRockSideScrollingOverrideUsesItsDedicatedObjectRows() {
        byte[] rom = loadRom();

        int[][] palettes = new RoomLoader(rom).loadIndoor(
            0x07, 0x64, null, Warp.CATEGORY_SIDESCROLL)
            .entities().spriteSelection().objectPalettes();

        assertArrayEquals(readPalette(rom, 0x21, 0x6790), palettes[6]);
        assertArrayEquals(readPalette(rom, 0x21, 0x6798), palettes[7]);
    }

    private static int[] readPalette(byte[] rom, int bank, int address) {
        int[] palette = new int[4];
        int offset = RomBank.romOffset(bank, address);
        for (int color = 0; color < palette.length; color++) {
            int encoded = Byte.toUnsignedInt(rom[offset++])
                | (Byte.toUnsignedInt(rom[offset++]) << 8);
            palette[color] = RomBank.decodeRgb555(encoded);
        }
        return palette;
    }

    private static byte[] loadRom() {
        try (var stream = RoomLoaderTest.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ROM", e);
        }
    }
}
