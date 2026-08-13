package linksawakening.world;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void marinHouseSelectsTarinFlagPaletteForBackgroundAndObjectRows() {
        byte[] rom = new byte[RomBank.romOffset(0x22, 0x4000)];
        writeColor(rom, 0x73B0, 0x001F);
        writeColor(rom, 0x73B0 + 0x40, 0x03E0);
        writeColor(rom, 0x74A0, 0x7C00);
        writeColor(rom, 0x74A0 + 0x40, 0x7FFF);
        RoomPaletteLoader loader = new RoomPaletteLoader(rom);

        assertEquals(RomBank.decodeRgb555(0x001F),
            loader.loadIndoor(0x10, 0xA3, null, 0)[0][0]);
        assertEquals(RomBank.decodeRgb555(0x7C00),
            loader.loadIndoor(0x10, 0xA3, null, 1)[0][0]);
        assertEquals(RomBank.decodeRgb555(0x7C00),
            loader.loadIndoor(0x10, 0xA3, null, 2)[0][0]);
        assertEquals(RomBank.decodeRgb555(0x03E0),
            loader.loadIndoorObjectPalettes(0x10, 0xA3, false, 0)[6][0]);
        assertEquals(RomBank.decodeRgb555(0x7FFF),
            loader.loadIndoorObjectPalettes(0x10, 0xA3, false, 1)[6][0]);
    }

    private static void writeColor(byte[] rom, int address, int color) {
        int offset = RomBank.romOffset(0x21, address);
        rom[offset] = (byte) color;
        rom[offset + 1] = (byte) (color >> 8);
    }
}
