package linksawakening.dialog;

import linksawakening.rom.RomBank;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

final class DialogFont {

    private static final int FONT_BANK = 0x0F;
    private static final int FONT_ADDRESS = 0x5000;
    private static final int FONT_TILE_COUNT = 0xA0;
    private static final int CODEPOINT_TABLE_BANK = 0x1C;
    private static final int CODEPOINT_TABLE_ADDRESS = 0x4641;
    private static final int TILE_SIZE_BYTES = 16;
    private static final int CODEPOINT_COUNT = 256;
    private static final String ROM_RESOURCE = "/rom/azle.gbc";

    private final byte[] fontTiles;
    private final int[] codepointToTile;

    private DialogFont(byte[] fontTiles, int[] codepointToTile) {
        this.fontTiles = fontTiles;
        this.codepointToTile = codepointToTile;
    }

    static DialogFont loadDefault() {
        try (InputStream stream = DialogFont.class.getResourceAsStream(ROM_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing classpath ROM resource " + ROM_RESOURCE);
            }
            return loadFromRom(stream.readAllBytes());
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read classpath ROM resource " + ROM_RESOURCE, exception);
        }
    }

    static DialogFont loadFromRom(byte[] romData) {
        if (romData == null) {
            throw new IllegalArgumentException("ROM data is required to load dialog font data");
        }

        int fontOffset = RomBank.romOffset(FONT_BANK, FONT_ADDRESS);
        int fontLength = FONT_TILE_COUNT * TILE_SIZE_BYTES;
        requireRange(romData, fontOffset, fontLength, "FontTiles bank 0x0F address 0x5000");

        int tableOffset = RomBank.romOffset(CODEPOINT_TABLE_BANK, CODEPOINT_TABLE_ADDRESS);
        requireRange(romData, tableOffset, CODEPOINT_COUNT, "CodepointToTileMap bank 0x1C address 0x4641");

        byte[] fontTiles = new byte[fontLength];
        System.arraycopy(romData, fontOffset, fontTiles, 0, fontTiles.length);

        int[] codepointToTile = new int[CODEPOINT_COUNT];
        for (int i = 0; i < codepointToTile.length; i++) {
            codepointToTile[i] = Byte.toUnsignedInt(romData[tableOffset + i]);
        }
        return new DialogFont(fontTiles, codepointToTile);
    }

    byte[] tileFor(char codepoint) {
        int tableIndex = codepoint & 0xFF;
        int tileIndex = codepointToTile[tableIndex];
        int offset = tileIndex * TILE_SIZE_BYTES;
        if (offset < 0 || offset + TILE_SIZE_BYTES > fontTiles.length) {
            throw new IllegalArgumentException(
                "CodepointToTileMap entry 0x" + Integer.toHexString(tableIndex)
                    + " points outside FontTiles: tile 0x" + Integer.toHexString(tileIndex));
        }
        byte[] tile = new byte[TILE_SIZE_BYTES];
        System.arraycopy(fontTiles, offset, tile, 0, tile.length);
        return tile;
    }

    private static void requireRange(byte[] romData, int offset, int length, String label) {
        if (offset < 0 || length < 0 || offset > romData.length || romData.length - offset < length) {
            throw new IllegalArgumentException(
                label + " requires ROM range 0x" + Integer.toHexString(offset)
                    + "..0x" + Integer.toHexString(offset + length)
                    + " but ROM length is 0x" + Integer.toHexString(romData.length));
        }
    }
}
