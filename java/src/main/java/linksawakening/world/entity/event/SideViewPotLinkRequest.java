package linksawakening.world.entity.event;

import linksawakening.world.entity.RoomEntity;

/** Link-side writes emitted by entity-$D6's state-0 collision handler. */
public record SideViewPotLinkRequest(
        int sourceSlot,
        boolean resetPegasusBoots,
        boolean restoreFinalPositionX,
        int ignoreCollisionCountdown,
        int speedX,
        boolean snapTop,
        int positionY,
        int speedY)
        implements EntityRequest {
    public SideViewPotLinkRequest {
        if (sourceSlot < 0
                || sourceSlot >= RoomEntity.SLOT_COUNT
                || ignoreCollisionCountdown < 0
                || ignoreCollisionCountdown > 0xFF
                || (speedX & ~0xFF) != 0
                || (positionY & ~0xFF) != 0
                || (speedY & ~0xFF) != 0) {
            throw new IllegalArgumentException("Invalid side-view pot Link request");
        }
    }
}
