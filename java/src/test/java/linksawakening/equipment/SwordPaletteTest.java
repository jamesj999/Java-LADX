package linksawakening.equipment;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class SwordPaletteTest {

    @Test
    void loadsNormalAndChargedRowsFromRomObjectPalettes() {
        byte[] rom = syntheticRom();
        writePaletteRow(rom, 3, 0x001F, 0x03E0, 0x7C00, 0x7FFF);
        writePaletteRow(rom, 4, 0x0210, 0x0421, 0x0632, 0x0843);

        SwordPalette palette = SwordPalette.loadFromRom(rom);

        assertArrayEquals(new int[] {
                RomBank.decodeRgb555(0x001F), RomBank.decodeRgb555(0x03E0),
                RomBank.decodeRgb555(0x7C00), RomBank.decodeRgb555(0x7FFF)},
            palette.normal());
        assertArrayEquals(new int[] {
                RomBank.decodeRgb555(0x0210), RomBank.decodeRgb555(0x0421),
                RomBank.decodeRgb555(0x0632), RomBank.decodeRgb555(0x0843)},
            palette.charged());
    }

    @Test
    void returnedRowsAreDefensiveCopies() {
        byte[] rom = syntheticRom();
        writePaletteRow(rom, 3, 0x001F, 0x03E0, 0x7C00, 0x7FFF);
        writePaletteRow(rom, 4, 0x0210, 0x0421, 0x0632, 0x0843);
        SwordPalette palette = SwordPalette.loadFromRom(rom);

        int[] normal = palette.normal();
        int[] charged = palette.charged();
        normal[2] = 0;
        charged[2] = 0;

        assertEquals(RomBank.decodeRgb555(0x7C00), palette.normal()[2]);
        assertEquals(RomBank.decodeRgb555(0x0632), palette.charged()[2]);
    }

    private static byte[] syntheticRom() {
        return new byte[RomBank.romOffset(0x22, 0x4000)];
    }

    private static void writePaletteRow(byte[] rom, int palette, int... colors) {
        int offset = RomBank.romOffset(0x21, 0x5518) + palette * 8;
        for (int color : colors) {
            rom[offset++] = (byte) color;
            rom[offset++] = (byte) (color >>> 8);
        }
    }
}
