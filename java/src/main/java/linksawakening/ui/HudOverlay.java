package linksawakening.ui;

import linksawakening.state.PlayerState;

public final class HudOverlay {

    private static final int WIDTH = InventoryTilemapLoader.WIDTH;

    // The base inventory tilemap puts a label at col 0/5 and a divider at col 4/9,
    // giving each slot a 3-tile interior (cols 1-3 for B, 6-8 for A). The
    // disassembly places the icon at the leftmost interior cell and the "L-N"
    // level indicator in the two cells to its right.
    private static final int B_SLOT_COL = 1;
    private static final int A_SLOT_COL = 6;

    private static final int RUPEE_DIGITS_COL = 10;
    private static final int RUPEE_DIGITS_ROW = 1;
    private static final int RUPEE_DIGIT_TILE_BASE = 0xB0;

    private static final int HEART_COL = 13;
    private static final int HEART_ROW_TOP = 0;
    private static final int HEART_ROW_BOTTOM = 1;
    private static final int HEARTS_PER_ROW = 7;
    private static final int HEART_TILE_FULL = 0xA9;
    private static final int HEART_TILE_HALF = 0xCE;
    private static final int HEART_TILE_EMPTY = 0xCD;
    private static final int HEART_TILE_BLANK = 0x7F;

    private HudOverlay() {
    }

    public static void apply(int[] tilemap, int[] attrmap, PlayerState player, InventoryTilemapLoader loader) {
        loader.drawItem(tilemap, attrmap, B_SLOT_COL, 0, player.itemB(), player);
        loader.drawItem(tilemap, attrmap, A_SLOT_COL, 0, player.itemA(), player);
        patchRupees(tilemap, player.rupees());
        patchHearts(tilemap, player.health(), player.maxHearts());
    }

    private static void patchRupees(int[] tilemap, int rupees) {
        int clamped = Math.max(0, Math.min(999, rupees));
        int hundreds = clamped / 100;
        int tens = (clamped / 10) % 10;
        int ones = clamped % 10;
        writeTile(tilemap, RUPEE_DIGITS_COL,     RUPEE_DIGITS_ROW, RUPEE_DIGIT_TILE_BASE + hundreds);
        writeTile(tilemap, RUPEE_DIGITS_COL + 1, RUPEE_DIGITS_ROW, RUPEE_DIGIT_TILE_BASE + tens);
        writeTile(tilemap, RUPEE_DIGITS_COL + 2, RUPEE_DIGITS_ROW, RUPEE_DIGIT_TILE_BASE + ones);
    }

    private static void patchHearts(int[] tilemap, int health, int maxHearts) {
        for (int heartIndex = 0; heartIndex < HEARTS_PER_ROW * 2; heartIndex++) {
            int col = HEART_COL + (heartIndex % HEARTS_PER_ROW);
            int row = heartIndex < HEARTS_PER_ROW ? HEART_ROW_TOP : HEART_ROW_BOTTOM;

            int tile;
            if (heartIndex >= maxHearts) {
                tile = HEART_TILE_BLANK;
            } else if (health >= (heartIndex + 1) * PlayerState.HP_PER_HEART) {
                tile = HEART_TILE_FULL;
            } else if (health > heartIndex * PlayerState.HP_PER_HEART) {
                tile = HEART_TILE_HALF;
            } else {
                tile = HEART_TILE_EMPTY;
            }

            writeTile(tilemap, col, row, tile);
        }
    }

    private static void writeTile(int[] tilemap, int col, int row, int tileId) {
        int index = row * WIDTH + col;
        if (index >= 0 && index < tilemap.length) {
            tilemap[index] = tileId & 0xFF;
        }
    }
}
