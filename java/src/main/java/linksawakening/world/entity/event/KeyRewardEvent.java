package linksawakening.world.entity.event;

import linksawakening.world.*;
import linksawakening.vfx.TransientVfxType;

/** Reward application requested by PickDroppableKey. */
public record KeyRewardEvent(int slot, int itemType) implements EntityRequest {
    public KeyRewardEvent {
        if (slot < 0 || slot >= RoomEntity.SLOT_COUNT) {
            throw new IllegalArgumentException("Key entity slot out of range: " + slot);
        }
        if ((itemType & ~0xFF) != 0) {
            throw new IllegalArgumentException("Key item must be an unsigned byte: "
                + itemType);
        }
    }
}
