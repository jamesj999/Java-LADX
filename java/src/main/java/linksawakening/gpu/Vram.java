package linksawakening.gpu;

import linksawakening.rom.Rom;

/** ROM-independent VRAM storage and decoded tile cache. */
final class Vram {
    public static final int VRAM_SIZE = 0x2000;
    public static final int TILE_COUNT = 384;
    public static final int TILE_DATA_SIZE = 16;
    private final byte[] vram;
    private final Tile[] tiles;

    public Vram() {
        vram = new byte[VRAM_SIZE];
        tiles = new Tile[TILE_COUNT];

        for (int i = 0; i < TILE_COUNT; i++) {
            tiles[i] = new Tile();
            updateTile(i);
        }
    }

    public void writeVRAM(int address, byte value) {
        if (address >= 0 && address < VRAM_SIZE) {
            vram[address] = value;
            int tileIndex = address / TILE_DATA_SIZE;
            if (tileIndex < TILE_COUNT) {
                updateTile(tileIndex);
            }
        }
    }

    public byte readVRAM(int address) {
        if (address >= 0 && address < VRAM_SIZE) {
            return vram[address];
        }
        return 0;
    }

    final void updateTile(int tileIndex) {
        int address = tileIndex * TILE_DATA_SIZE;
        if (address + TILE_DATA_SIZE > VRAM_SIZE) {
            return;
        }

        Tile tile = tiles[tileIndex];
        for (int y = 0; y < 8; y++) {
            int lowByte = Byte.toUnsignedInt(vram[address + y * 2]);
            int highByte = Byte.toUnsignedInt(vram[address + y * 2 + 1]);

            for (int x = 0; x < 8; x++) {
                int bitIndex = 7 - x;
                int lowBit = (lowByte >> bitIndex) & 1;
                int highBit = (highByte >> bitIndex) & 1;
                tile.setPixel(x, y, (highBit << 1) | lowBit);
            }
        }
    }

    public Tile getTile(int index) {
        if (index >= 0 && index < TILE_COUNT) {
            return tiles[index];
        }
        return null;
    }

    private EntitySpriteTileSnapshot entitySnapshot;

    final void invalidateEntitySnapshot() {
        entitySnapshot = null;
    }

    public EntitySpriteTileSnapshot snapshotEntityTiles() {
        if (entitySnapshot != null && entitySnapshot.matches(tiles, EntitySpriteTileSnapshot.BASE_TILE_INDEX))
            return entitySnapshot;
        Tile[] entityTiles = new Tile[EntitySpriteTileSnapshot.TILE_COUNT];
        for (int i = 0; i < entityTiles.length; i++) {
            entityTiles[i] = tiles[EntitySpriteTileSnapshot.BASE_TILE_INDEX + i];
        }
        entitySnapshot = new EntitySpriteTileSnapshot(entityTiles);
        return entitySnapshot;
    }

    final void loadTilesFromROM(Rom romData, int bank, int address, int tileCount, int vramTileIndex) {
        if (bank < 0) throw new IllegalArgumentException("Bank cannot be negative");
        int romOffset = bank == 0 ? address : bank * 0x4000 + (address - 0x4000);
        int vramOffset = vramTileIndex * TILE_DATA_SIZE;
        int byteCount = tileCount * TILE_DATA_SIZE;

        if (romOffset < 0 || romOffset + byteCount > romData.size()) {
            throw new IllegalArgumentException("Tile source exceeds ROM bounds");
        }
        if (vramOffset < 0 || vramOffset + byteCount > VRAM_SIZE) {
            throw new IllegalArgumentException("Tile destination exceeds VRAM bounds");
        }

        // The legacy selector's bank-zero row uses a flat ROM offset,
        // including addresses outside ROM0's CPU window. Preserve that API.
        // Retain the original CPU-window validation for switchable banks.
        if (!(bank == 0 && address >= 0x4000)
                && (bank > Integer.MAX_VALUE / 0x4000
                        || address < (bank == 0 ? 0 : 0x4000)
                        || address >= (bank == 0 ? 0x4000 : 0x8000))) {
            throw new IllegalArgumentException("Invalid ROM bank/address");
        }
        romData.copyInto(romOffset, vram, vramOffset, byteCount);
        for (int tile = 0; tile < tileCount; tile++) {
            updateTile(vramTileIndex + tile);
        }
    }

    void copyTiles(byte[] source, int destinationTile) {
        System.arraycopy(source, 0, vram, destinationTile * TILE_DATA_SIZE, source.length);
        for (int tile = destinationTile; tile < destinationTile + source.length / TILE_DATA_SIZE; tile++)
            updateTile(tile);
    }
}
