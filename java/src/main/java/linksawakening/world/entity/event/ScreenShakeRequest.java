package linksawakening.world.entity.event;

import linksawakening.world.*;
import linksawakening.vfx.TransientVfxType;

/** A ROM handler request to start the source screen-shake timer. */
public record ScreenShakeRequest(int sourceSlot, int countdown, int phase) implements EntityRequest {
    public ScreenShakeRequest {
        if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Screen-shake source slot out of range: "
                + sourceSlot);
        }
        if (countdown < 0 || countdown > 0xFF) {
            throw new IllegalArgumentException("Screen-shake countdown must be an unsigned byte");
        }
        if (phase < 0 || phase > 0x04 || (phase & 0x01) != 0) {
            throw new IllegalArgumentException("Screen-shake phase must be 0, 2, or 4");
        }
    }
}
