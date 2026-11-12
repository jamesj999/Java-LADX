package linksawakening.ui;

import linksawakening.state.InventoryItem;
import linksawakening.state.PlayerState;

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

    static int[] forItem(int itemId, PlayerState player) {
        if (itemId == InventoryItem.MAGIC_POWDER.romValue()
                && player.ammunition().hasToadstool()) {
            return new int[] {2, 3}; // $20:5BFD: red cap, brown stem.
        }
        if (itemId == InventoryItem.POWER_BRACELET.romValue()
                && player.equipmentLevels().powerBraceletLevel() == 2) {
            return new int[] {2, 2};
        }
        return forItem(itemId);
    }

    private InventoryItemPalettes() {}

    static int[] forItem(int itemId) {
        return itemId >= 0 && itemId < INDEXES.length ? INDEXES[itemId] : INDEXES[0];
    }

    static int palettePair(int itemId, PlayerState player) {
        if (itemId == InventoryItem.MAGIC_POWDER.romValue()
                && player.ammunition().hasToadstool()) return 0x0203;
        if (itemId == InventoryItem.POWER_BRACELET.romValue()
                && player.equipmentLevels().powerBraceletLevel() == 2) return 0x0202;
        int[] values = forItem(itemId);
        return (values[0] << 8) | values[1];
    }
}
