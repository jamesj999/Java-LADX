package linksawakening.world.entity.event;

import linksawakening.world.*;
import linksawakening.vfx.TransientVfxType;

/** A ROM handler request to block Link's next interactive motion frame. */
public record LinkMotionBlockRequest(int sourceSlot, int positionDeltaX, int positionDeltaY) implements EntityRequest {
    public LinkMotionBlockRequest(int sourceSlot) { this(sourceSlot, 0, 0); }
    public LinkMotionBlockRequest {
        if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Link motion-block source slot out of range: "
                + sourceSlot);
        }
    }
}
