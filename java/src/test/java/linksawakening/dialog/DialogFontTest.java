package linksawakening.dialog;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DialogFontTest {

    private static final int FONT_OFFSET = RomBank.romOffset(0x0F, 0x5000);
    private static final int CODEPOINT_TABLE_OFFSET = RomBank.romOffset(0x1C, 0x4641);
    private static final int TILE_SIZE = 16;

    @Test
    void loadFromRomUsesFontTilesAndCodepointMapFromRomOffsets() {
        byte[] rom = new byte[CODEPOINT_TABLE_OFFSET + 256];
        rom = Arrays.copyOf(rom, Math.max(rom.length, FONT_OFFSET + 3 * TILE_SIZE));
        rom[CODEPOINT_TABLE_OFFSET + 'A'] = 0x02;
        byte[] expectedTile = new byte[TILE_SIZE];
        for (int i = 0; i < expectedTile.length; i++) {
            expectedTile[i] = (byte) (0xA0 + i);
        }
        System.arraycopy(expectedTile, 0, rom, FONT_OFFSET + 2 * TILE_SIZE, TILE_SIZE);

        DialogFont font = DialogFont.loadFromRom(rom);

        assertArrayEquals(expectedTile, font.tileFor('A'));
    }

    @Test
    void loadFromRomRejectsMissingRomData() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> DialogFont.loadFromRom(null));

        assertTrue(exception.getMessage().contains("ROM data"));
    }

    @Test
    void loadFromRomRejectsTruncatedFontTilesRange() {
        byte[] rom = new byte[FONT_OFFSET + TILE_SIZE - 1];

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> DialogFont.loadFromRom(rom));

        assertTrue(exception.getMessage().contains("FontTiles"));
    }

    @Test
    void loadFromRomRejectsTruncatedCodepointTableRange() {
        byte[] rom = new byte[CODEPOINT_TABLE_OFFSET + 255];

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> DialogFont.loadFromRom(rom));

        assertTrue(exception.getMessage().contains("CodepointToTileMap"));
    }
}
