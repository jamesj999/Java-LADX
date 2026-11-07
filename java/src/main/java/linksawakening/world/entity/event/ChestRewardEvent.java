package linksawakening.world.entity.event;

import linksawakening.world.RoomEntity;
import linksawakening.vfx.TransientVfxType;

/** Reward application requested by EntityInitChestWithItem. */
public record ChestRewardEvent(int slot, int itemType) implements EntityRequest {
    public ChestRewardEvent {
        if (slot < 0 || slot >= RoomEntity.SLOT_COUNT) {
            throw new IllegalArgumentException("Chest entity slot out of range: " + slot);
        }
        if ((itemType & ~0xFF) != 0) {
            throw new IllegalArgumentException("Chest item must be an unsigned byte: "
                + itemType);
        }
    }
}
