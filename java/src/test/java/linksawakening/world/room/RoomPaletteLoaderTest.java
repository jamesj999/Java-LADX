package linksawakening.world.room;

import linksawakening.world.*;

import linksawakening.world.room.RoomPaletteLoader;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@linksawakening.testing.RequiresRom
final class RoomPaletteLoaderTest {

    @Test
    void overworldPaletteComesFromRoomPaletteMapAndPalettePointerTable() {
        byte[] rom = new byte[RomBank.romOffset(0x21, 0x5000) + 0x40];
        rom[RomBank.romOffset(0x21, 0x42EF) + 0x92] = 0x02;
        int pointerOffset = RomBank.romOffset(0x21, 0x42B1) + 0x04;
        rom[pointerOffset] = 0x00;
        rom[pointerOffset + 1] = 0x50;
        rom[RomBank.romOffset(0x21, 0x5000)] = 0x1F;
        rom[RomBank.romOffset(0x21, 0x5000) + 1] = 0x00;

        int[][] palettes = new RoomPaletteLoader(rom).loadOverworld(0x92);

        assertEquals(0xFF0000, palettes[0][0]);
    }

    @Test
    void colorDungeonUsesItsDedicatedPaletteBlock() {
        byte[] rom = new byte[RomBank.romOffset(0x21, 0x6800)];
        int palette = RomBank.romOffset(0x21, 0x67D0);
        rom[palette] = (byte) 0x1F;
        rom[palette + 1] = 0x00;
        int[][] fallback = {{0x123456, 0, 0, 0}};

        int[][] result = new RoomPaletteLoader(rom).loadIndoor(0xFF, 0x00, fallback);

        assertEquals(RomBank.decodeRgb555(0x001F), result[0][0]);
    }

    @Test
    void marinHouseSelectsSourcePaletteForTarinFlagAndShieldLevel() {
        byte[] rom = new byte[RomBank.romOffset(0x22, 0x4000)];
        int houseMap = RomBank.romOffset(0x21, 0x4413 + (0x10 - 0x0A) * 2);
        rom[houseMap] = 0x00;
        rom[houseMap + 1] = 0x50;
        rom[RomBank.romOffset(0x21, 0x5000 + 0xA3)] = 0x06;
        int interiorPalette = RomBank.romOffset(0x21, 0x443F + 0x06 * 2);
        rom[interiorPalette] = 0x00;
        rom[interiorPalette + 1] = 0x60;
        writeColor(rom, 0x73B0, 0x001F);
        writeColor(rom, 0x73B0 + 0x40, 0x03E0);
        writeColor(rom, 0x74A0, 0x7C00);
        writeColor(rom, 0x74A0 + 0x40, 0x7FFF);
        writeColor(rom, 0x6000, 0x4210);
        writeColor(rom, 0x6000 + 0x40, 0x56B5);
        RoomPaletteLoader loader = new RoomPaletteLoader(rom);

        assertEquals(RomBank.decodeRgb555(0x001F),
            loader.loadIndoor(0x10, 0xA3, null, 0, 0)[0][0]);
        assertEquals(RomBank.decodeRgb555(0x4210),
            loader.loadIndoor(0x10, 0xA3, null, 0, 1)[0][0]);
        assertEquals(RomBank.decodeRgb555(0x03E0),
            loader.loadIndoorObjectPalettes(0x10, 0xA3, false, 0, 0)[6][0]);
        assertEquals(RomBank.decodeRgb555(0x56B5),
            loader.loadIndoorObjectPalettes(0x10, 0xA3, false, 0, 1)[6][0]);
        for (int tarinFlag : new int[] { 1, 2 }) {
            for (int shieldLevel : new int[] { 0, 2 }) {
                assertEquals(RomBank.decodeRgb555(0x7C00),
                    loader.loadIndoor(
                        0x10, 0xA3, null, tarinFlag, shieldLevel)[0][0]);
                assertEquals(RomBank.decodeRgb555(0x7FFF),
                    loader.loadIndoorObjectPalettes(
                        0x10, 0xA3, false, tarinFlag, shieldLevel)[6][0]);
            }
        }
    }

    @Test
    void dungeonBackgroundUsesRomPointersForTopDownAndSideViewRatherThanPreviousRoomColors() {
        byte[] rom = new byte[RomBank.romOffset(0x22, 0x4000)];
        int[][] fallback = {{0x123456, 0, 0, 0}};
        RoomPaletteLoader loader = new RoomPaletteLoader(rom);
        for (int map = 0; map <= 8; map++) {
            for (boolean sideView : new boolean[]{false, true}) {
                int table = RomBank.romOffset(0x21, (sideView ? 0x4401 : 0x43EF) + map * 2);
                int address = 0x6000 + map * 0x100 + (sideView ? 0x50 : 0);
                rom[table] = (byte) address;
                rom[table + 1] = (byte) (address >> 8);
                for (int color = 0; color < 32; color++) writeColor(rom, address + color * 2,
                    0x1000 + map * 0x40 + color + (sideView ? 0x20 : 0));
                int[][] result = loader.loadIndoor(map, 0x17, fallback, 0, 1, sideView);
                for (int color = 0; color < 32; color++) assertEquals(
                    RomBank.decodeRgb555(0x1000 + map * 0x40 + color + (sideView ? 0x20 : 0)),
                    result[color / 4][color % 4]);
            }
        }
        writeColor(rom, 0x6750, 0x03E0);
        assertEquals(RomBank.decodeRgb555(0x03E0),
            loader.loadIndoor(7, 0x64, fallback, 0, 1, true)[0][0]);
    }

    @Test
    void indoorTransitionTablePreservesColorsAndSeparatesBackgroundFromObjectUpdates() throws Exception {
        byte[] rom;
        try (var input = linksawakening.testing.TestRom.open()) { rom = input.readAllBytes(); }
        RoomPaletteLoader loader = new RoomPaletteLoader(rom);
        int[][] baseBg = loader.loadIndoor(0, 0x17, null);
        int[][] baseObj = loader.loadIndoorObjectPalettes(0, 0x17);
        var entrance = loader.indoorTransition(0, 0x17, 4, baseBg, baseObj, null, null);
        assertPaletteRow(rom, 0x6080 + 0x30, entrance.background()[6]);
        assertPaletteRow(rom, 0x6080 + 0x40, entrance.objects()[6]);
        var retained = loader.indoorTransition(0, 0x16, 1, baseBg, baseObj,
            entrance.background(), entrance.objects());
        for (int row = 0; row < 8; row++) {
            assertArrayEquals(entrance.background()[row], retained.background()[row]);
            assertArrayEquals(entrance.objects()[row], retained.objects()[row]);
        }
        var bgOnly = loader.indoorTransition(0, 0x13, 1, baseBg, baseObj,
            retained.background(), retained.objects());
        for (int row = 0; row < 8; row++) {
            assertArrayEquals(baseBg[row], bgOnly.background()[row]);
            assertArrayEquals(retained.objects()[row], bgOnly.objects()[row]);
        }
        var objOnly = loader.indoorTransition(0, 0x10, 0, baseBg, baseObj,
            bgOnly.background(), bgOnly.objects());
        for (int row = 0; row < 8; row++) assertArrayEquals(baseBg[row], objOnly.background()[row]);
        for (int row = 0; row < 6; row++) assertArrayEquals(bgOnly.objects()[row], objOnly.objects()[row]);
        assertPaletteRow(rom, 0x60D0 + 0x40, objOnly.objects()[6]);
        assertPaletteRow(rom, 0x60D0 + 0x48, objOnly.objects()[7]);
        assertPaletteRow(rom, 0x6080 + 0x30, entrance.background()[6]);
    }

    private static void assertPaletteRow(byte[] rom, int address, int[] actual) {
        int offset = RomBank.romOffset(0x21, address);
        for (int c = 0; c < 4; c++) assertEquals(RomBank.decodeRgb555(
            (rom[offset + c * 2] & 0xFF) | ((rom[offset + c * 2 + 1] & 0xFF) << 8)), actual[c]);
    }

    private static void writeColor(byte[] rom, int address, int color) {
        int offset = RomBank.romOffset(0x21, address);
        rom[offset] = (byte) color;
        rom[offset + 1] = (byte) (color >> 8);
    }
}
