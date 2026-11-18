package linksawakening.world.entity.event;

import linksawakening.world.entity.RoomEntity;

public record LinkSwordSpinPoseRequest(int sourceSlot, int countdown) implements EntityRequest {
    public LinkSwordSpinPoseRequest {
        if (sourceSlot < 0 || sourceSlot >= RoomEntity.SLOT_COUNT) {
            throw new IllegalArgumentException("Sword-spin source slot out of range: " + sourceSlot);
        }
        if (countdown < 0 || countdown > 0x20) {
            throw new IllegalArgumentException("Sword-spin countdown out of range: " + countdown);
        }
    }
}
