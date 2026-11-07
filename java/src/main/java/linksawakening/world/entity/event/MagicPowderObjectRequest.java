package linksawakening.world.entity.event;

import linksawakening.world.*;
import linksawakening.world.entity.event.MagicPowderObjectAction;
import linksawakening.vfx.TransientVfxType;

/** A ROM powder-sprinkle object mutation applied by RoomSession. */
public record MagicPowderObjectRequest(int sourceSlot, int location,
                                       int objectLeft, int objectTop,
                                       MagicPowderObjectAction action) implements EntityRequest {
    public MagicPowderObjectRequest {
        if (sourceSlot < 0 || sourceSlot >= RoomEntity.SLOT_COUNT) {
            throw new IllegalArgumentException("Magic Powder source slot out of range: "
                + sourceSlot);
        }
        location &= 0xFF;
        objectLeft &= 0xF0;
        objectTop &= 0xF0;
        if (action == null) {
            throw new IllegalArgumentException("Magic Powder object action cannot be null");
        }
    }
}
