package linksawakening.vfx;

import linksawakening.gpu.Tile;

/**
 * ROM-backed OAM graphics used by transient effects.
 */
public final class TransientVfxSpriteSheet {

    private static final int BANK_SIZE = 0x4000;
    private static final int CHARACTER_VFX_BANK = 0x0C;
    private static final int CHARACTER_VFX_ADDR = 0x4200;
    private static final int CHARACTER_VFX_TILE_COUNT = 0x20;
    private static final int TILE_BYTES = 0x10;
    private static final int BASE_TILE_INDEX = 0x20;

    private final Tile[] tiles;

    private TransientVfxSpriteSheet(Tile[] tiles) {
        this.tiles = tiles;
    }

    public static TransientVfxSpriteSheet loadFromRom(byte[] romData) {
        Tile[] tiles = new Tile[CHARACTER_VFX_TILE_COUNT];
        int offset = romOffset(CHARACTER_VFX_BANK, CHARACTER_VFX_ADDR);
        for (int i = 0; i < tiles.length; i++) {
            tiles[i] = decodeTile(romData, offset + i * TILE_BYTES);
        }
        return new TransientVfxSpriteSheet(tiles);
    }

    public Tile tile(int tileIndex) {
        int localIndex = tileIndex - BASE_TILE_INDEX;
        if (localIndex < 0 || localIndex >= tiles.length) {
            return null;
        }
        return tiles[localIndex];
    }

    public int baseTileIndex() {
        return BASE_TILE_INDEX;
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
