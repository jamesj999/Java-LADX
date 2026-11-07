package linksawakening.world.entity.event;

import linksawakening.world.entity.RoomEntity;

/** A ROM handler request to show Link's explicit fallen animation pose. */
public record LinkFallenPoseRequest(int sourceSlot) implements EntityRequest {
    public LinkFallenPoseRequest {
        if (sourceSlot < 0 || sourceSlot >= RoomEntity.SLOT_COUNT) {
            throw new IllegalArgumentException("Link fallen-pose source slot out of range: "
                + sourceSlot);
        }
    }
}
