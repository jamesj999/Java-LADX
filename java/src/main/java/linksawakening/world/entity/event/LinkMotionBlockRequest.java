package linksawakening.world.entity.event;

import linksawakening.world.entity.RoomEntity;

/** A ROM handler request to block Link's next interactive motion frame. */
public record LinkMotionBlockRequest(int sourceSlot, int positionDeltaX, int positionDeltaY, int value) implements EntityRequest {
    public LinkMotionBlockRequest(int sourceSlot) { this(sourceSlot, 0, 0, 2); }
    public LinkMotionBlockRequest(int sourceSlot, int deltaX, int deltaY) { this(sourceSlot, deltaX, deltaY, 2); }
    public LinkMotionBlockRequest {
        if ((value & ~255) != 0) throw new IllegalArgumentException("Interaction block must be an unsigned byte");
        if (sourceSlot < 0 || sourceSlot >= RoomEntity.SLOT_COUNT) {
            throw new IllegalArgumentException("Link motion-block source slot out of range: "
                + sourceSlot);
        }
    }
}
