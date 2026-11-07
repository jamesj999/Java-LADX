package linksawakening.world.entity.event;

import linksawakening.world.entity.RoomEntity;

/** Link HRAM writes emitted by Rooster's active lifted handler. */
public record RoosterLinkStateRequest(int sourceSlot, int positionZ, int velocityZ,
                                      int speedX, int speedY, int romDirection) implements EntityRequest {
    public RoosterLinkStateRequest {
        if (sourceSlot < 0 || sourceSlot >= RoomEntity.SLOT_COUNT) {
            throw new IllegalArgumentException("Rooster source slot out of range: "
                + sourceSlot);
        }
        if ((positionZ & ~0xFF) != 0 || (velocityZ & ~0xFF) != 0
            || (speedX & ~0xFF) != 0 || (speedY & ~0xFF) != 0) {
            throw new IllegalArgumentException("Rooster Link state must be byte-shaped");
        }
        if (romDirection < 0 || romDirection > 3) {
            throw new IllegalArgumentException("Rooster ROM direction must be between 0 and 3");
        }
    }
}
