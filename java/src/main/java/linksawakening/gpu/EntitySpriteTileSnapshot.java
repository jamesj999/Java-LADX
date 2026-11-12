package linksawakening.gpu;

/** Immutable decoded copy of the four standard entity OAM tile slots. */
public final class EntitySpriteTileSnapshot {
    public static final int BASE_TILE_INDEX = 0x40;
    public static final int TILE_COUNT = 0x40;

    private final Tile[] tiles;

    public EntitySpriteTileSnapshot(Tile[] sourceTiles) {
        if (sourceTiles == null || sourceTiles.length != TILE_COUNT) {
            throw new IllegalArgumentException("Exactly 64 entity tiles are required");
        }
        this.tiles = new Tile[TILE_COUNT];
        for (int i = 0; i < TILE_COUNT; i++) {
            if (sourceTiles[i] == null) {
                throw new IllegalArgumentException("Entity tile cannot be null: " + i);
            }
            this.tiles[i] = sourceTiles[i].frozenCopy();
        }
    }

    public Tile tile(int tileIndex) {
        int offset = tileIndex - BASE_TILE_INDEX;
        if (offset < 0 || offset >= TILE_COUNT) {
            return null;
        }
        return tiles[offset].copy();
    }
    /** Stable read-only tile for rendering; callers needing mutable storage use tile(). */
    public Tile readOnlyTile(int tileIndex) {
        int offset = tileIndex - BASE_TILE_INDEX;
        return offset < 0 || offset >= TILE_COUNT ? null : tiles[offset];
    }

    boolean matches(Tile[] source, int start) {
        for (int i = 0; i < TILE_COUNT; i++) {
            if (source[start + i] == null) return false;
            for (int y = 0; y < 8; y++)
                for (int x = 0; x < 8; x++)
                    if (source[start + i].getPixel(x, y) != tiles[i].getPixel(x, y)) return false;
        }
        return true;
    }
}
