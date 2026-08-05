package linksawakening.entity;

import linksawakening.rom.RomBank;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

final class LinkTunicPaletteTest {

    @Test
    void mapsTunicValuesToTheRomObjectPaletteRows() {
        byte[] rom = syntheticRom();
        writePaletteRow(rom, 0, 0x001F, 0x03E0, 0x7C00, 0x7FFF);
        writePaletteRow(rom, 2, 0x0210, 0x0421, 0x0632, 0x0843);
        writePaletteRow(rom, 3, 0x0A54, 0x0C65, 0x0E76, 0x1087);

        LinkTunicPalette palette = LinkTunicPalette.loadFromRom(rom);

        assertArrayEquals(new int[] {0xFF0000, 0x00FF00, 0x0000FF, 0xFFFFFF},
            palette.forTunic(PlayerState.TUNIC_GREEN));
        assertArrayEquals(new int[] {
                RomBank.decodeRgb555(0x0210), RomBank.decodeRgb555(0x0421),
                RomBank.decodeRgb555(0x0632), RomBank.decodeRgb555(0x0843)},
            palette.forTunic(PlayerState.TUNIC_RED));
        assertArrayEquals(new int[] {
                RomBank.decodeRgb555(0x0A54), RomBank.decodeRgb555(0x0C65),
                RomBank.decodeRgb555(0x0E76), RomBank.decodeRgb555(0x1087)},
            palette.forTunic(PlayerState.TUNIC_BLUE));
    }

    @Test
    void returnedRowsAreDefensiveCopies() {
        byte[] rom = syntheticRom();
        writePaletteRow(rom, 0, 0x001F, 0x03E0, 0x7C00, 0x7FFF);
        LinkTunicPalette palette = LinkTunicPalette.loadFromRom(rom);

        int[] returned = palette.forTunic(PlayerState.TUNIC_GREEN);
        returned[2] = 0;

        assertArrayEquals(new int[] {0xFF0000, 0x00FF00, 0x0000FF, 0xFFFFFF},
            palette.forTunic(PlayerState.TUNIC_GREEN));
    }

    @Test
    void noRomCompatibilityKeepsLegacyTunicStatesVisible() {
        LinkTunicPalette palette = LinkTunicPalette.greenCompatibility();

        assertArrayEquals(palette.forTunic(PlayerState.TUNIC_GREEN),
            palette.forTunic(PlayerState.TUNIC_RED));
        assertArrayEquals(palette.forTunic(PlayerState.TUNIC_GREEN),
            palette.forTunic(PlayerState.TUNIC_BLUE));
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
