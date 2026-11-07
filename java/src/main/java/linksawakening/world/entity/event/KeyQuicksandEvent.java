package linksawakening.world.entity.event;

import linksawakening.world.entity.RoomEntity;
import linksawakening.vfx.TransientVfxType;

/** Status propagation requested by CheckForEntityFallingDownQuicksandHole. */
public record KeyQuicksandEvent(int slot) implements EntityRequest {
    public KeyQuicksandEvent {
        if (slot < 0 || slot >= RoomEntity.SLOT_COUNT) {
            throw new IllegalArgumentException("Key entity slot out of range: " + slot);
        }
    }
}
