package linksawakening.world.entity.event;

import linksawakening.world.RoomEntity;
import linksawakening.vfx.TransientVfxType;

/** A boomerang request to reveal the object it intersected after movement. */
public record BoomerangObjectRequest(int sourceSlot, int location,
                                     int objectLeft, int objectTop) implements EntityRequest {
    public BoomerangObjectRequest {
        if (sourceSlot < 0 || sourceSlot >= RoomEntity.SLOT_COUNT) {
            throw new IllegalArgumentException("Boomerang source slot out of range: "
                + sourceSlot);
        }
        location &= 0xFF;
        objectLeft &= 0xF0;
        objectTop &= 0xF0;
    }
}
