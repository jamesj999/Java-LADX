package linksawakening.equipment;

import linksawakening.state.PlayerState;

/**
 * Maps inventory ids to the single {@link EquippedItem} instance that handles
 * that item's behavior. Lookups by {@link PlayerState#itemA()} /
 * {@link PlayerState#itemB()} resolve to the same object whether the sword is
 * in slot A or B, so internal state (swing progress, timers) is shared and
 * doesn't duplicate if the same item ever appeared in both slots.
 */
public final class ItemRegistry {

    private final EquippedItem[] byInventoryId = new EquippedItem[0x100];

    public void register(int inventoryId, EquippedItem item) {
        byInventoryId[inventoryId & 0xFF] = item;
    }

    /** Returns null if no handler exists for this id (e.g. INVENTORY_EMPTY). */
    public EquippedItem lookup(int inventoryId) {
        return byInventoryId[inventoryId & 0xFF];
    }

    /** Advance every registered item's state machine once. Called once per frame. */
    public void tickAll(boolean aHeld, boolean bHeld, int itemAId, int itemBId) {
        for (int id = 0; id < byInventoryId.length; id++) {
            EquippedItem item = byInventoryId[id];
            if (item == null) {
                continue;
            }
            boolean held = (id == itemAId && aHeld) || (id == itemBId && bHeld);
            item.tick(held);
        }
    }
}
