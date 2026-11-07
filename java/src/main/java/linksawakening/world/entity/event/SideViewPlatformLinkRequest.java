package linksawakening.world.entity.event;

import linksawakening.world.RoomEntity;
import linksawakening.vfx.TransientVfxType;

/** Link-side position/speed writes emitted by the side-view platform handler. */
public record SideViewPlatformLinkRequest(int sourceSlot, int horizontalDelta,
                                          int positionY, int speedY, boolean standing,
                                          boolean activate) implements EntityRequest {
    public SideViewPlatformLinkRequest {
        if (sourceSlot < 0 || sourceSlot >= RoomEntity.SLOT_COUNT
            || horizontalDelta < -0x80 || horizontalDelta > 0x7F
            || (positionY & ~0xFF) != 0
            || (speedY & ~0xFF) != 0) {
            throw new IllegalArgumentException("Invalid side-view platform Link request");
        }
    }
}
