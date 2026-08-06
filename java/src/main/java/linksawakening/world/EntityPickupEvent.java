package linksawakening.world;

/** The ROM entity slot and type that completed a Link pickup collision. */
public record EntityPickupEvent(int slot, int type, int persistentClearMask,
                                int sourceVariant) {

    /** Compatibility constructor for ordinary pickable handlers. */
    public EntityPickupEvent(int slot, int type, int persistentClearMask) {
        this(slot, type, persistentClearMask, -1);
    }

    public EntityPickupEvent {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        if ((type & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity type must be an unsigned byte: " + type);
        }
        if ((persistentClearMask & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity clear mask must be an unsigned byte: "
                + persistentClearMask);
        }
        if (sourceVariant < -1 || sourceVariant > 0xFF) {
            throw new IllegalArgumentException("Entity source variant must be -1 or an unsigned byte: "
                + sourceVariant);
        }
    }
}
