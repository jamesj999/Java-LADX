package linksawakening.world.entity.event;

import linksawakening.world.entity.RoomEntity;

public record LinkSwordFinalPoseRequest(int sourceSlot) implements EntityRequest {
    public LinkSwordFinalPoseRequest {
        if (sourceSlot < 0 || sourceSlot >= RoomEntity.SLOT_COUNT) {
            throw new IllegalArgumentException("Sword-final source slot out of range: "
                + sourceSlot);
        }
    }
}
