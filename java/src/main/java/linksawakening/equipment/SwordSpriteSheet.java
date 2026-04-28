package linksawakening.equipment;

import linksawakening.gpu.Tile;

/**
 * Sword blade OAM tiles, sourced from {@code LinkCharacterTiles} (bank $0C /
 * GBC $2C, addr $4000 — the {@code oam_link_1} tileset). The blade itself
 * lives at tile indices $04/$05 (vertical), $06/$07 and $08/$09 (horizontal
 * halves), $0A/$0B and $0C/$0D (diagonals).
 *
 * <p>Companion to {@link linksawakening.entity.LinkSpriteSheet}: that one
 * loads {@code oam_link_2} for Link's body, this one loads {@code oam_link_1}
 * for the sword blade (and other VFX tiles). The disassembly's
 * {@code func_020_4AB3} (bank20.asm:4AB3) picks two adjacent 8x16 sprites
 * per {@code wSwordDirection} from this tileset and places them next to Link.
 */
public final class SwordSpriteSheet {

    private static final int BANK_SIZE = 0x4000;
    private static final int LINK_CHARACTER_1_BANK_GBC = 0x2C;
    private static final int LINK_CHARACTER_1_ADDR = 0x4000;
    private static final int TILE_COUNT = 0x20;
    private static final int TILE_BYTES = 0x10;

    private final Tile[] tiles;

    private SwordSpriteSheet(Tile[] tiles) {
        this.tiles = tiles;
    }

    public static SwordSpriteSheet loadFromRom(byte[] romData) {
        Tile[] tiles = new Tile[TILE_COUNT];
        int offset = romOffset(LINK_CHARACTER_1_BANK_GBC, LINK_CHARACTER_1_ADDR);
        for (int t = 0; t < TILE_COUNT; t++) {
            tiles[t] = decodeTile(romData, offset + t * TILE_BYTES);
        }
        return new SwordSpriteSheet(tiles);
    }

    public Tile tile(int index) {
        return tiles[index & (TILE_COUNT - 1)];
    }

    private static Tile decodeTile(byte[] romData, int offset) {
        Tile tile = new Tile();
        for (int y = 0; y < 8; y++) {
            int lowByte = Byte.toUnsignedInt(romData[offset + y * 2]);
            int highByte = Byte.toUnsignedInt(romData[offset + y * 2 + 1]);
            for (int x = 0; x < 8; x++) {
                int bitIndex = 7 - x;
                int low = (lowByte >> bitIndex) & 1;
                int high = (highByte >> bitIndex) & 1;
                tile.setPixel(x, y, (high << 1) | low);
            }
        }
        return tile;
    }

    private static int romOffset(int bank, int address) {
        if (bank == 0) {
            return address;
        }
        return bank * BANK_SIZE + (address - BANK_SIZE);
    }
}
