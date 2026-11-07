package linksawakening.world.entity.event;

import linksawakening.world.*;
import linksawakening.vfx.TransientVfxType;

/** A ROM bridge handler request to replace one padded object cell and draw its columns. */
public record HookshotBridgeUpdate(int objectLeft, int objectTop, int direction,
                                   boolean active) implements EntityRequest {
    public static final int PULL_DOWN_DIRECTION = 0;
    public static final int PULL_UP_DIRECTION = 1;

    public HookshotBridgeUpdate(int objectLeft, int objectTop, int direction) {
        this(objectLeft, objectTop, direction, true);
    }

    public HookshotBridgeUpdate {
        objectLeft &= 0xF0;
        objectTop &= 0xF0;
        if (direction < PULL_DOWN_DIRECTION
            || direction > PULL_UP_DIRECTION) {
            throw new IllegalArgumentException("Bridge direction must be 0 or 1: " + direction);
        }
    }
}
