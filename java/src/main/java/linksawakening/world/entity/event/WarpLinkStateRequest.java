package linksawakening.world.entity.event;

import linksawakening.world.transition.Warp;

import linksawakening.world.*;
import linksawakening.vfx.TransientVfxType;

/** Link HRAM writes emitted by entity-$61's WarpState3Handler. */
public record WarpLinkStateRequest(int sourceSlot, int positionX, int visualPositionY,
                                   int immunityCountdown) implements EntityRequest {
    public WarpLinkStateRequest {
        if (sourceSlot < 0 || sourceSlot >= RoomEntity.SLOT_COUNT
            || (positionX & ~0xFF) != 0 || (visualPositionY & ~0xFF) != 0
            || (immunityCountdown & ~0xFF) != 0) {
            throw new IllegalArgumentException("Warp Link state must be byte-shaped");
        }
    }
}
