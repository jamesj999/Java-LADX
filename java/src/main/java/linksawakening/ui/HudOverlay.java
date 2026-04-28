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

    // Tile $BA is the "-" glyph: its presence in bottom-row[1] of the
    // disassembly's InventoryItemTiles table is how leveled items (sword,
    // shield, bracelet) are identified.
    private static final int LEVEL_DASH_TILE = 0xBA;
    private static final int DEFAULT_ITEM_LEVEL = 1;
    private static final int ITEM_SLOT_ROW_TOP = 0;
    private static final int ITEM_SLOT_ROW_BOTTOM = 1;

    private static final int RUPEE_DIGITS_COL = 10;
    private static final int RUPEE_DIGITS_ROW = 1;
    private static final int RUPEE_DIGITS_COUNT = 3;
    private static final int RUPEE_DIGIT_TILE_BASE = 0xB0;

    private static final int HEART_COL = 13;
    private static final int HEART_ROW_TOP = 0;
    private static final int HEART_ROW_BOTTOM = 1;
    private static final int HEARTS_PER_ROW = 7;
    private static final int HEART_TILE_FULL = 0xA9;
    private static final int HEART_TILE_HALF = 0xCE;
    private static final int HEART_TILE_EMPTY = 0xCD;
    private static final int HEART_TILE_BLANK = 0x7F;

    private static final int[][] INVENTORY_ITEM_TILES = buildInventoryItemTiles();

    private HudOverlay() {
    }

    public static void apply(int[] tilemap, PlayerState player) {
        patchItemSlot(tilemap, B_SLOT_COL, player.itemB());
        patchItemSlot(tilemap, A_SLOT_COL, player.itemA());
        patchRupees(tilemap, player.rupees());
        patchHearts(tilemap, player.health(), player.maxHearts());
    }

    private static void patchItemSlot(int[] tilemap, int col, int itemId) {
        // Item icons are 1 tile wide but 2 tiles tall: top-row[0] is the upper
        // half, bottom-row[0] is the lower half (which also contains the "L"
        // glyph for leveled items, merged into the same tile).
        // For leveled items the table sets bottom-row[1]=$BA (the "-" glyph),
        // and the actual level digit is computed at runtime.
        int[] itemTiles = INVENTORY_ITEM_TILES[itemId < INVENTORY_ITEM_TILES.length ? itemId : 0];
        writeTile(tilemap, col, ITEM_SLOT_ROW_TOP,    itemTiles[0]);
        writeTile(tilemap, col, ITEM_SLOT_ROW_BOTTOM, itemTiles[3]);

        if (itemTiles[4] == LEVEL_DASH_TILE) {
            writeTile(tilemap, col + 1, ITEM_SLOT_ROW_BOTTOM, LEVEL_DASH_TILE);
            writeTile(tilemap, col + 2, ITEM_SLOT_ROW_BOTTOM, RUPEE_DIGIT_TILE_BASE + DEFAULT_ITEM_LEVEL);
        }
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

    private static int[][] buildInventoryItemTiles() {
        // Mirrors InventoryItemTiles in bank20.asm: 6 tiles per item
        // ([top[0..2], bottom[0..2]]). Order matches PlayerState.INVENTORY_*.
        // A $BA glyph in bottom[1] marks a leveled item (sword/bracelet/shield).
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
}
