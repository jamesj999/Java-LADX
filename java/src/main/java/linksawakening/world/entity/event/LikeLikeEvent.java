package linksawakening.world.entity.event;

/** Link capture/release side effects emitted by Like Like's handler. */
public record LikeLikeEvent(
        int slot,
        Kind kind,
        int entityX,
        int entityY,
        int stolenInventorySlot,
        int stolenShieldLevel,
        boolean pinLinkPosition)
        implements EntityRequest {
    public enum Kind {
        CAPTURE,
        RELEASE
    }

    public LikeLikeEvent(
            int slot, Kind kind, int entityX, int entityY, int stolenInventorySlot, int stolenShieldLevel) {
        this(slot, kind, entityX, entityY, stolenInventorySlot, stolenShieldLevel, kind == Kind.CAPTURE);
    }
}
