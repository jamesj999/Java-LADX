package linksawakening.world.entity.event;

import linksawakening.vfx.TransientVfxType;

/** A ROM transient-VFX creation requested by an entity handler this frame. */
public record TransientVfxRequest(TransientVfxType type, int worldX, int worldY, int variant) implements EntityRequest {
    public TransientVfxRequest(TransientVfxType type, int worldX, int worldY) {
        this(type, worldX, worldY, 0);
    }
}
