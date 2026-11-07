package linksawakening.world.entity.event;

import linksawakening.world.*;
import linksawakening.vfx.TransientVfxType;

public record LinkFacingRequest(int sourceSlot, int romDirection,
                                boolean preserveWalkingPhase) implements EntityRequest {
    public LinkFacingRequest(int sourceSlot, int romDirection) {
        this(sourceSlot, romDirection, false);
    }

    public LinkFacingRequest {
        if (sourceSlot < 0 || sourceSlot >= RoomEntity.SLOT_COUNT
            || romDirection < 0 || romDirection > 3) {
            throw new IllegalArgumentException("Invalid Link-facing request");
        }
    }
}
