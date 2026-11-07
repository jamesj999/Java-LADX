package linksawakening.equipment;

import linksawakening.state.PlayerState;

/**
 * Maps inventory ids to the single {@link EquipmentItem} instance that handles
 * that item's behavior. Lookups by {@link PlayerState#itemA()} /
 * {@link PlayerState#itemB()} resolve to the same object whether the sword is
 * in slot A or B, so internal state (swing progress, timers) is shared and
 * doesn't duplicate if the same item ever appeared in both slots.
 */
public final class ItemRegistry {

    private final EquipmentItem[] byInventoryId = new EquipmentItem[0x100];

    public void register(int inventoryId, EquipmentItem item) {
        byInventoryId[inventoryId & 0xFF] = item;
    }

    /** Returns null if no handler exists for this id (e.g. INVENTORY_EMPTY). */
    public EquipmentItem lookup(int inventoryId) {
        return byInventoryId[inventoryId & 0xFF];
    }

    /** Resolve the equipped sword, including its pre-inventory pickup spin. */
    public Sword activeSword(PlayerState playerState) {
        EquipmentItem a = lookup(playerState.itemA());
        if (a instanceof Sword sword) {
            return sword;
        }
        EquipmentItem b = lookup(playerState.itemB());
        if (b instanceof Sword sword) {
            return sword;
        }
        // The acquisition sequence spins the sword before it enters inventory.
        EquipmentItem registered = lookup(PlayerState.INVENTORY_SWORD);
        if (registered instanceof Sword sword && sword.spinAttackActive()) return sword;
        return null;
    }

    /** Finish item animation without starting a release-triggered spin. */
    public void tickWithoutInput(PlayerState playerState, int frameCounter) {
        Sword sword = activeSword(playerState);
        if (sword != null) sword.releaseHoldWithoutSpin();
        tickAll(false, false, playerState.itemA(), playerState.itemB(), frameCounter);
    }

    /** Advance every registered item's state machine once. Called once per frame. */
    public void tickAll(boolean aHeld, boolean bHeld, int itemAId, int itemBId) {
        tickAll(aHeld, bHeld, itemAId, itemBId, null);
    }

    /** Advance every registered item while exposing the global ROM frame. */
    public void tickAll(boolean aHeld, boolean bHeld, int itemAId, int itemBId,
                        int frameCounter) {
        tickAll(aHeld, bHeld, itemAId, itemBId, Integer.valueOf(frameCounter));
    }

    private void tickAll(boolean aHeld, boolean bHeld, int itemAId, int itemBId,
                         Integer frameCounter) {
        for (int id = 0; id < byInventoryId.length; id++) {
            EquipmentItem item = byInventoryId[id];
            if (item == null) {
                continue;
            }
            boolean held = (id == itemAId && aHeld) || (id == itemBId && bHeld);
            if (frameCounter == null) {
                item.tick(held);
            } else {
                item.tick(held, frameCounter);
            }
        }
    }
}
