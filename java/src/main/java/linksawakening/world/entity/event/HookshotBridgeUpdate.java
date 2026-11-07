package linksawakening.world.entity.event;

import linksawakening.world.*;
import linksawakening.vfx.TransientVfxType;

/** A ROM bridge handler request to replace one padded object cell and draw its columns. */
public record HookshotBridgeUpdate(int objectLeft, int objectTop, int direction,
                                   boolean active) implements EntityRequest {
    public HookshotBridgeUpdate(int objectLeft, int objectTop, int direction) {
        this(objectLeft, objectTop, direction, true);
    }

    public HookshotBridgeUpdate {
        objectLeft &= 0xF0;
        objectTop &= 0xF0;
        if (direction < HookshotBridgeMotion.PULL_DOWN_DIRECTION
            || direction > HookshotBridgeMotion.PULL_UP_DIRECTION) {
            throw new IllegalArgumentException("Bridge direction must be 0 or 1: " + direction);
        }
    }
}
