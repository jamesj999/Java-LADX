package linksawakening.world.entity.event;

import linksawakening.world.entity.RoomEntity;

/** A source handler's direct write clearing Link's attack animation countdown. */
public record LinkAttackClearRequest(int sourceSlot) implements EntityRequest {
    public LinkAttackClearRequest {
        if (sourceSlot < 0 || sourceSlot >= RoomEntity.SLOT_COUNT) {
            throw new IllegalArgumentException("Link attack-clear source slot out of range");
        }
    }
}
