package linksawakening.ui;

import linksawakening.state.PlayerState;

/**
 * Draws the 10 subscreen inventory items (everything below the top-row A/B
 * status bar) and the selection cursor into the inventory tilemap. The base
 * tilemap from ROM has only borders and dividers; item icons are painted
 * dynamically here, mirroring the disassembly's runtime subscreen drawing.
 *
 * <p>The cursor matches the OAM-sprite approach from {@code func_020_62A2}
 * (bank20.asm:62A2): two copies of tile {@code $BE}, the left one flipped
 * horizontally to form {@code [}, the right one plain for {@code ]}. Since
 * we have no OAM layer yet, we render the same tiles through the BG
 * attrmap's flipX bit instead.
 */
public final class InventorySubscreenOverlay {

    private static final int WIDTH = InventoryTilemapLoader.WIDTH;

    // 2-column grid. Item icons are 1 tile wide, 2 tiles tall, with an
    // optional "L-N" level indicator in the two cells to the right of the
    // bottom half. Both columns live in the left half of the screen
    // (cols 0-7), since the right half (cols 9-19) is reserved for the
    // dungeon items / map / quest-status area in the original.
    private static final int LEFT_COL = 1;
    private static final int RIGHT_COL = 5;
    private static final int FIRST_ROW = 3;
    private static final int ROW_STRIDE = 3;

    // The original's cursor is an 8x16 OAM sprite using tile $BE (the "]"
    // top-half from the HUD brackets) once per side — flipX on the left to
    // produce "[", plain on the right for "]". Reused here as BG tiles with
    // the attrmap's flipX bit. Tile $BF is the stacked bottom half used in
    // the same way.
    private static final int BRACKET_TILE_TOP = 0xBE;
    private static final int BRACKET_TILE_BOTTOM = 0xBF;
    private static final int ATTR_FLIP_X = 0x20;
    // Column offset from the icon's column to where the closing bracket sits,
    // leaving room for the 2-tile "L-N" level indicator when present.
    private static final int BRACKET_RIGHT_OFFSET = 3;
    private static final int LEVEL_DASH_TILE = 0xBA;
    private static final int DIGIT_TILE_BASE = 0xB0;
    private static final int DEFAULT_ITEM_LEVEL = 1;

    private static final int[][] ITEM_TILES = buildItemTiles();
    private static final int[][] ITEM_PALETTE_INDEXES = buildItemPaletteIndexes();

    private InventorySubscreenOverlay() {
    }

    public static void apply(int[] tilemap, int[] attrmap, PlayerState player,
                             int cursorSlot, boolean cursorVisible) {
        for (int slot = 0; slot < PlayerState.SUBSCREEN_SLOT_COUNT; slot++) {
            int col = slotColumn(slot);
            int row = slotRow(slot);
            drawItem(tilemap, attrmap, col, row, player.subscreenItem(slot));
        }
        if (cursorVisible) {
            int col = slotColumn(cursorSlot);
            int row = slotRow(cursorSlot);
            drawCursor(tilemap, attrmap, col, row);
        }
    }

    private static int slotColumn(int slot) {
        return (slot % 2 == 0) ? LEFT_COL : RIGHT_COL;
    }

    private static int slotRow(int slot) {
        return FIRST_ROW + (slot / 2) * ROW_STRIDE;
    }

    private static void drawItem(int[] tilemap, int[] attrmap, int col, int row, int itemId) {
        int itemIndex = itemId >= 0 && itemId < ITEM_TILES.length ? itemId : 0;
        int[] tiles = ITEM_TILES[itemIndex];
        int[] paletteIndexes = ITEM_PALETTE_INDEXES[itemIndex];
        writeTile(tilemap, col, row,     tiles[0]); // top-left icon
        writeTile(tilemap, col, row + 1, tiles[3]); // bottom-left icon (with L-glyph)
        writeAttr(attrmap, col, row,     paletteIndexes[0]);
        writeAttr(attrmap, col, row + 1, paletteIndexes[1]);
        if (tiles[4] == LEVEL_DASH_TILE) {
            writeTile(tilemap, col + 1, row + 1, LEVEL_DASH_TILE);
            writeTile(tilemap, col + 2, row + 1, DIGIT_TILE_BASE + DEFAULT_ITEM_LEVEL);
        }
    }

    private static void drawCursor(int[] tilemap, int[] attrmap, int col, int row) {
        int leftCol = col - 1;
        int rightCol = col + BRACKET_RIGHT_OFFSET;
        writeTile(tilemap, leftCol,  row,     BRACKET_TILE_TOP);
        writeTile(tilemap, leftCol,  row + 1, BRACKET_TILE_BOTTOM);
        writeTile(tilemap, rightCol, row,     BRACKET_TILE_TOP);
        writeTile(tilemap, rightCol, row + 1, BRACKET_TILE_BOTTOM);
        // Plain `]` on the right keeps its base attrmap; flip the left pair
        // so the same tile reads as `[`.
        setAttrFlipX(attrmap, leftCol, row);
        setAttrFlipX(attrmap, leftCol, row + 1);
    }

    private static void writeTile(int[] tilemap, int col, int row, int tileId) {
        if (col < 0 || col >= WIDTH) {
            return;
        }
        int index = row * WIDTH + col;
        if (index >= 0 && index < tilemap.length) {
            tilemap[index] = tileId & 0xFF;
        }
    }

    private static void setAttrFlipX(int[] attrmap, int col, int row) {
        if (col < 0 || col >= WIDTH) {
            return;
        }
        int index = row * WIDTH + col;
        if (index >= 0 && index < attrmap.length) {
            attrmap[index] = (attrmap[index] | ATTR_FLIP_X) & 0xFF;
        }
    }

    private static void writeAttr(int[] attrmap, int col, int row, int attributes) {
        if (col < 0 || col >= WIDTH) {
            return;
        }
        int index = row * WIDTH + col;
        if (index >= 0 && index < attrmap.length) {
            attrmap[index] = attributes & 0xFF;
        }
    }

    // Same layout as HudOverlay.INVENTORY_ITEM_TILES but kept local so the
    // two overlays can evolve independently. Each row is
    // [top[0..2], bottom[0..2]]; $BA in bottom[1] marks a leveled item.
    private static int[][] buildItemTiles() {
        return new int[][] {
            {0x7F, 0x7F, 0x7F, 0x7F, 0x7F, 0x7F}, // EMPTY
            {0x84, 0x7F, 0x7F, 0x85, 0xBA, 0x7F}, // SWORD
            {0x80, 0x7F, 0x7F, 0x81, 0x7F, 0x7F}, // BOMBS
            {0x82, 0x7F, 0x7F, 0x83, 0xBA, 0x7F}, // POWER_BRACELET
            {0x86, 0x7F, 0x7F, 0x87, 0xBA, 0x7F}, // SHIELD
            {0x88, 0x7F, 0x7F, 0x89, 0x7F, 0x7F}, // BOW
            {0x8A, 0x7F, 0x7F, 0x8B, 0x7F, 0x7F}, // HOOKSHOT
            {0x8C, 0x7F, 0x7F, 0x8D, 0x7F, 0x7F}, // MAGIC_ROD
            {0x98, 0x7F, 0x7F, 0x99, 0x7F, 0x7F}, // PEGASUS_BOOTS
            {0x90, 0x7F, 0x7F, 0x91, 0x7F, 0x7F}, // OCARINA
            {0x92, 0x7F, 0x7F, 0x93, 0x7F, 0x7F}, // ROCS_FEATHER
            {0x96, 0x7F, 0x7F, 0x97, 0x7F, 0x7F}, // SHOVEL
            {0x8E, 0x7F, 0x7F, 0x8F, 0x7F, 0x7F}, // MAGIC_POWDER
            {0xA4, 0x7F, 0x7F, 0xA5, 0x7F, 0x7F}, // BOOMERANG
        };
    }

    private static int[][] buildItemPaletteIndexes() {
        return new int[][] {
            {0x01, 0x01}, // EMPTY
            {0x01, 0x01}, // SWORD
            {0x01, 0x01}, // BOMBS
            {0x01, 0x01}, // POWER_BRACELET
            {0x01, 0x01}, // SHIELD
            {0x03, 0x03}, // BOW
            {0x01, 0x02}, // HOOKSHOT
            {0x02, 0x01}, // MAGIC_ROD
            {0x03, 0x03}, // PEGASUS_BOOTS
            {0x02, 0x02}, // OCARINA
            {0x03, 0x03}, // ROCS_FEATHER
            {0x03, 0x01}, // SHOVEL
            {0x03, 0x03}, // MAGIC_POWDER
            {0x02, 0x02}, // BOOMERANG
        };
    }
}
