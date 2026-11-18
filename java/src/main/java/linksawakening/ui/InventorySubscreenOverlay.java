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
    private InventorySubscreenOverlay() {
    }

    public static void apply(int[] tilemap, int[] attrmap, PlayerState player,
                             int cursorSlot, boolean cursorVisible, InventoryTilemapLoader loader) {
        for (int slot = 0; slot < PlayerState.SUBSCREEN_SLOT_COUNT; slot++) {
            int col = slotColumn(slot);
            int row = slotRow(slot);
            loader.drawItem(tilemap, attrmap, col, row, player.subscreenItem(slot), player);
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
}
