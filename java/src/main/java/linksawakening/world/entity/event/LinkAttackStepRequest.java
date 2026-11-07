package linksawakening.world.entity.event;

import linksawakening.world.entity.RoomEntity;

/** A lifted-throw request applied after AnimateEntities, like the ROM HRAM write. */
public record LinkAttackStepRequest(int sourceSlot) implements EntityRequest {
    public LinkAttackStepRequest {
        if (sourceSlot < 0 || sourceSlot >= RoomEntity.SLOT_COUNT) {
            throw new IllegalArgumentException(
                "Link attack-step source slot out of range: " + sourceSlot);
        }
    }
}
