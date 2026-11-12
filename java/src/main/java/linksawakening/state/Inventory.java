package linksawakening.state;

import static linksawakening.state.PlayerNumbers.*;
import static linksawakening.state.PlayerState.*;

import java.util.Objects;

/** Inventory owns its player-state fields and their update rules. */
public final class Inventory {
    int itemA = InventoryItem.SWORD.romValue();
    int itemB = InventoryItem.EMPTY.romValue();
    final int[] subscreen = new int[SUBSCREEN_SLOT_COUNT];

    public Inventory() {
        for (int i = 0; i < subscreen.length; i++) subscreen[i] = InventoryItem.BOMBS.romValue() + i;
    }

    public int itemA() {
        return itemA;
    }

    public void setItemA(int inventoryId) {
        itemA = inventoryId & 0xFF;
    }

    public int itemB() {
        return itemB;
    }

    public void setItemB(int inventoryId) {
        itemB = inventoryId & 0xFF;
    }

    public void setSubscreenItems(int[] items) {
        Objects.requireNonNull(items, "items");
        if (items.length != SUBSCREEN_SLOT_COUNT) {
            throw new IllegalArgumentException(
                    "Expected " + SUBSCREEN_SLOT_COUNT + " subscreen items, got " + items.length);
        }
        for (int index = 0; index < items.length; index++) {
            subscreen[index] = items[index] & 0xFF;
        }
    }

    public int subscreenItem(int slotIndex) {
        return subscreen[slotIndex];
    }

    public void swapItemAWithSubscreen(int slotIndex) {
        int tmp = itemA;
        itemA = subscreen[slotIndex];
        subscreen[slotIndex] = tmp;
    }

    public void swapItemBWithSubscreen(int slotIndex) {
        int tmp = itemB;
        itemB = subscreen[slotIndex];
        subscreen[slotIndex] = tmp;
    }

    public void giveInventoryItem(int inventoryId) {
        if (itemB == inventoryId || itemA == inventoryId) {
            return;
        }
        for (int item : subscreen) {
            if (item == inventoryId) {
                return;
            }
        }
        if (itemB == InventoryItem.EMPTY.romValue()) {
            itemB = inventoryId;
            return;
        }
        if (itemA == InventoryItem.EMPTY.romValue()) {
            itemA = inventoryId;
            return;
        }
        for (int index = 0; index < subscreen.length; index++) {
            if (subscreen[index] == InventoryItem.EMPTY.romValue()) {
                subscreen[index] = inventoryId;
                return;
            }
        }
    }
}
