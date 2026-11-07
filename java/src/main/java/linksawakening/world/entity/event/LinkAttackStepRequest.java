package linksawakening.world.entity.event;

import linksawakening.world.*;
import linksawakening.vfx.TransientVfxType;

/** A lifted-throw request applied after AnimateEntities, like the ROM HRAM write. */
public record LinkAttackStepRequest(int sourceSlot) implements EntityRequest {
    public LinkAttackStepRequest {
        if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException(
                "Link attack-step source slot out of range: " + sourceSlot);
        }
    }
}
