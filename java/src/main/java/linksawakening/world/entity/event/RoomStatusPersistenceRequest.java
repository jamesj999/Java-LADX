package linksawakening.world.entity.event;

import linksawakening.world.*;
import linksawakening.vfx.TransientVfxType;

/** Deferred room-save writes that must not alter the live handler's room status. */
public record RoomStatusPersistenceRequest(int sourceSlot, int roomStatusMask,
                                           int tarinFlag) implements EntityRequest {
    public RoomStatusPersistenceRequest {
        if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES
            || (roomStatusMask & ~0xFF) != 0 || (tarinFlag & ~0xFF) != 0) {
            throw new IllegalArgumentException("Invalid room-status persistence request");
        }
    }
}
