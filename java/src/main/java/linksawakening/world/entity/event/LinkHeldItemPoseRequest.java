package linksawakening.world.entity.event;

import linksawakening.world.entity.RoomEntity;

public record LinkHeldItemPoseRequest(int sourceSlot, boolean powerUp) implements EntityRequest {
    public LinkHeldItemPoseRequest(int sourceSlot) { this(sourceSlot, false); }
    public LinkHeldItemPoseRequest {
        if (sourceSlot < 0 || sourceSlot >= RoomEntity.SLOT_COUNT) {
            throw new IllegalArgumentException("Held-item source slot out of range: "
                + sourceSlot);
        }
    }
}
