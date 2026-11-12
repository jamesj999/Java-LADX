package linksawakening.dialog;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomBank;
import linksawakening.rom.RomSymbols;

final class DialogFont {

    private static final int FONT_BANK = RomSymbols.FontTiles.bank();
    private static final int FONT_ADDRESS = RomSymbols.FontTiles.address();
    private static final int FONT_TILE_COUNT = 0xA0;
    private static final int CODEPOINT_TABLE_BANK = RomSymbols.CodepointToTileMap.bank();
    private static final int CODEPOINT_TABLE_ADDRESS = RomSymbols.CodepointToTileMap.address();
    private static final int TILE_SIZE_BYTES = 16;
    private static final int CODEPOINT_COUNT = 256;
    private static final String ROM_RESOURCE = "/rom/azle.gbc";

    private final byte[][] glyphTiles = new byte[FONT_TILE_COUNT][];
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
            return loadFromRom(Rom.of(stream.readAllBytes()));
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read classpath ROM resource " + ROM_RESOURCE, exception);
        }
    }

    static DialogFont loadFromRom(Rom romData) {
        if (romData == null) {
            throw new IllegalArgumentException("ROM data is required to load dialog font data");
        }

        int fontOffset = RomBank.romOffset(FONT_BANK, FONT_ADDRESS);
        int fontLength = FONT_TILE_COUNT * TILE_SIZE_BYTES;
        requireRange(romData, fontOffset, fontLength, "FontTiles bank 0x0F address 0x5000");

        int tableOffset = RomBank.romOffset(CODEPOINT_TABLE_BANK, CODEPOINT_TABLE_ADDRESS);
        requireRange(romData, tableOffset, CODEPOINT_COUNT, "CodepointToTileMap bank 0x1C address 0x4641");

        byte[] fontTiles = romData.bytes(RomAddress.fromOffset(fontOffset), fontLength);

        int[] codepointToTile = new int[CODEPOINT_COUNT];
        for (int i = 0; i < codepointToTile.length; i++) {
            codepointToTile[i] = romData.u8(RomAddress.fromOffset(tableOffset + i));
        }
        return new DialogFont(fontTiles, codepointToTile);
    }

    byte[] tileFor(char codepoint) {
        return tileView(codepoint).clone();
    }

    /** Borrowed immutable glyph bytes; renderers must not modify them. */
    byte[] tileView(char codepoint) {
        int tableIndex = codepoint & 0xFF;
        int tileIndex = codepointToTile[tableIndex];
        int offset = tileIndex * TILE_SIZE_BYTES;
        if (offset < 0 || offset + TILE_SIZE_BYTES > fontTiles.length) {
            throw new IllegalArgumentException("CodepointToTileMap entry 0x" + Integer.toHexString(tableIndex)
                    + " points outside FontTiles: tile 0x" + Integer.toHexString(tileIndex));
        }
        if (glyphTiles[tileIndex] != null) return glyphTiles[tileIndex];
        byte[] tile = new byte[TILE_SIZE_BYTES];
        System.arraycopy(fontTiles, offset, tile, 0, tile.length);
        glyphTiles[tileIndex] = tile;
        return tile;
    }

    private static void requireRange(Rom romData, int offset, int length, String label) {
        if (offset < 0 || length < 0 || offset > romData.size() || romData.size() - offset < length) {
            throw new IllegalArgumentException(label + " requires ROM range 0x" + Integer.toHexString(offset)
                    + "..0x" + Integer.toHexString(offset + length)
                    + " but ROM length is 0x" + Integer.toHexString(romData.size()));
        }
    }
}
