package linksawakening.ui;

/** Palette indexes from the ROM's {@code InventoryItemPaletteIndexes} table. */
final class InventoryItemPalettes {

    private static final int[][] INDEXES = {
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

    private InventoryItemPalettes() {
    }

    static int[] forItem(int itemId) {
        return itemId >= 0 && itemId < INDEXES.length ? INDEXES[itemId] : INDEXES[0];
    }
}
