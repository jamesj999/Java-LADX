package linksawakening.equipment;

import java.util.Arrays;
import java.util.Objects;
import linksawakening.state.InventoryItem;
import linksawakening.state.PlayerState;

/**
 * Maps inventory ids to the single {@link EquipmentItem} instance that handles
 * that item's behavior. Lookups by {@code playerState.inventory().itemA()} /
 * {@code playerState.inventory().itemB()} resolve to the same object whether the sword is
 * in slot A or B, so internal state (swing progress, timers) is shared and
 * doesn't duplicate if the same item ever appeared in both slots.
 */
public final class ItemRegistry {

    private final EquipmentItem[] byInventoryId = new EquipmentItem[0x100];

    public ItemRegistry() {
        Arrays.fill(byInventoryId, EquipmentItem.none());
    }

    public void register(int inventoryId, EquipmentItem item) {
        byInventoryId[inventoryId & 0xFF] = Objects.requireNonNullElse(item, EquipmentItem.none());
    }

    /** Non-null handler, including the explicit empty value for unassigned/unknown inventory bytes. */
    public EquipmentItem item(int inventoryId) {
        return byInventoryId[inventoryId & 0xFF];
    }

    /** Legacy nullable lookup for external callers; prefer {@link #item(int)}. */
    public EquipmentItem lookup(int inventoryId) {
        EquipmentItem item = item(inventoryId);
        return item == EquipmentItem.none() ? null : item;
    }

    /** Resolve the equipped sword, including its pre-inventory pickup spin. */
    public Sword activeSword(PlayerState playerState) {
        return activeSwordOrEmpty(playerState) instanceof Sword sword ? sword : null;
    }

    /** Non-null equipped or acquisition sword value; empty when no sword is active. */
    public EquipmentItem activeSwordOrEmpty(PlayerState playerState) {
        EquipmentItem a = item(playerState.inventory().itemA());
        if (a instanceof Sword sword) {
            return sword;
        }
        EquipmentItem b = item(playerState.inventory().itemB());
        if (b instanceof Sword sword) {
            return sword;
        }
        // The acquisition sequence spins the sword before it enters inventory.
        EquipmentItem registered = item(InventoryItem.SWORD.romValue());
        if (registered instanceof Sword sword && sword.spinAttackActive()) return sword;
        return EquipmentItem.none();
    }

    /** Finish item animation without starting a release-triggered spin. */
    public void tickWithoutInput(PlayerState playerState, int frameCounter) {
        if (activeSwordOrEmpty(playerState) instanceof Sword sword) sword.releaseHoldWithoutSpin();
        tickAll(
                false,
                false,
                playerState.inventory().itemA(),
                playerState.inventory().itemB(),
                frameCounter);
    }

    /** Advance every registered item's state machine once. Called once per frame. */
    public void tickAll(boolean aHeld, boolean bHeld, int itemAId, int itemBId) {
        tickAll(aHeld, bHeld, itemAId, itemBId, false, 0);
    }

    /** Advance every registered item while exposing the global ROM frame. */
    public void tickAll(boolean aHeld, boolean bHeld, int itemAId, int itemBId, int frameCounter) {
        tickAll(aHeld, bHeld, itemAId, itemBId, true, frameCounter);
    }

    private void tickAll(boolean aHeld, boolean bHeld, int itemAId, int itemBId, boolean hasFrame, int frameCounter) {
        for (int id = 0; id < byInventoryId.length; id++) {
            EquipmentItem item = byInventoryId[id];
            if (item == EquipmentItem.none()) {
                continue;
            }
            boolean held = (id == itemAId && aHeld) || (id == itemBId && bHeld);
            if (!hasFrame) {
                item.tick(held);
            } else {
                item.tick(held, frameCounter);
            }
        }
    }
}
