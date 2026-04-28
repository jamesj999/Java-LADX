package linksawakening.entity;

import linksawakening.gpu.Tile;

/**
 * Link's OAM sprite sheet, sourced from {@code LinkCharacter2Tiles} (base bank
 * $0C adjusted to CGB bank $2C, addr $5800 - the {@code oam_link_2} tileset)
 * and the animation-state lookup tables from bank $20 (addresses $5319 /
 * $5407).
 *
 * <p>Each animation state resolves to four 8x8 tiles forming a 16x16 sprite,
 * using Game Boy 8x16 OAM mode: two stacked-tile sprites side by side. Per-half
 * flip attributes come from the attrs table.
 *
 * <p>See LADX-Disassembly/src/code/bank20.asm:1768 (LinkAnimationStateTable)
 * and :1889 (data_020_5407).
 */
public final class LinkSpriteSheet {

    private static final int BANK_SIZE = 0x4000;

    private static final int LINK_CHARACTER_2_TILES_BANK = 0x0C | 0x20;
    private static final int LINK_CHARACTER_2_TILES_ADDR = 0x5800;
    private static final int LINK_CHARACTER_2_TILES_COUNT = 0x100;
    private static final int TILE_BYTES = 0x10;

    private static final int STATE_TABLE_BANK = 0x20;
    private static final int STATE_TABLE_ADDR = 0x5319;
    private static final int ATTRS_TABLE_ADDR = 0x5407;
    private static final int STATE_TABLE_LENGTH = 112; // entries, 2 bytes each

    private final Tile[] tiles;
    private final int[] stateTable;  // 2 bytes per entry: topPair, bottomPair
    private final int[] attrsTable;  // 2 bytes per entry: leftHalfAttrs, rightHalfAttrs

    private LinkSpriteSheet(Tile[] tiles, int[] stateTable, int[] attrsTable) {
        this.tiles = tiles;
        this.stateTable = stateTable;
        this.attrsTable = attrsTable;
    }

    public static LinkSpriteSheet loadFromRom(byte[] romData) {
        Tile[] tiles = new Tile[LINK_CHARACTER_2_TILES_COUNT];
        int tilesOffset = romOffset(LINK_CHARACTER_2_TILES_BANK, LINK_CHARACTER_2_TILES_ADDR);
        for (int t = 0; t < LINK_CHARACTER_2_TILES_COUNT; t++) {
            tiles[t] = decodeTile(romData, tilesOffset + t * TILE_BYTES);
        }

        int[] stateTable = new int[STATE_TABLE_LENGTH * 2];
        int stateOffset = romOffset(STATE_TABLE_BANK, STATE_TABLE_ADDR);
        for (int i = 0; i < stateTable.length; i++) {
            stateTable[i] = Byte.toUnsignedInt(romData[stateOffset + i]);
        }

        int[] attrsTable = new int[STATE_TABLE_LENGTH * 2];
        int attrsOffset = romOffset(STATE_TABLE_BANK, ATTRS_TABLE_ADDR);
        for (int i = 0; i < attrsTable.length; i++) {
            attrsTable[i] = Byte.toUnsignedInt(romData[attrsOffset + i]);
        }

        return new LinkSpriteSheet(tiles, stateTable, attrsTable);
    }

    /**
     * Fills {@code out} with the 4 composed tiles for a given animation state.
     * Indices are column-major (GB 8x16 sprite mode): [0]=upper-left,
     * [1]=lower-left, [2]=upper-right, [3]=lower-right.
     *
     * <p>Each entry in {@code LinkAnimationStateTable} is a pair of tile
     * offsets: the first identifies the left 8x16 column (top tile + next tile
     * below it in VRAM), the second identifies the right column. Per-half flip
     * attributes come from {@link #leftHalfFlipX(int)} /
     * {@link #rightHalfFlipX(int)} etc.
     */
    public void resolveTiles(int animationState, Tile[] out) {
        int leftCol = stateTable[(animationState * 2)];
        int rightCol = stateTable[(animationState * 2) + 1];
        out[0] = tiles[leftCol];
        out[1] = tiles[leftCol + 1];
        out[2] = tiles[rightCol];
        out[3] = tiles[rightCol + 1];
    }

    public boolean leftHalfFlipX(int animationState) {
        return (attrsTable[animationState * 2] & 0x20) != 0;
    }

    public boolean leftHalfFlipY(int animationState) {
        return (attrsTable[animationState * 2] & 0x40) != 0;
    }

    public boolean rightHalfFlipX(int animationState) {
        return (attrsTable[animationState * 2 + 1] & 0x20) != 0;
    }

    public boolean rightHalfFlipY(int animationState) {
        return (attrsTable[animationState * 2 + 1] & 0x40) != 0;
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
