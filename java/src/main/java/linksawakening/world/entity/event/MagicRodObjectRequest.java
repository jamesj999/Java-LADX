package linksawakening.world.entity.event;

import linksawakening.world.entity.RoomEntity;
import linksawakening.vfx.TransientVfxType;

/** A Magic Rod fireball request to reveal the object it burned. */
public record MagicRodObjectRequest(int sourceSlot, int location,
                                    int objectLeft, int objectTop) implements EntityRequest {
    public MagicRodObjectRequest {
        if (sourceSlot < 0 || sourceSlot >= RoomEntity.SLOT_COUNT) {
            throw new IllegalArgumentException("Magic Rod source slot out of range: "
                + sourceSlot);
        }
        location &= 0xFF;
        objectLeft &= 0xF0;
        objectTop &= 0xF0;
    }
}
