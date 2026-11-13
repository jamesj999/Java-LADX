package linksawakening.world.entity;

/** The padded-room object cell sampled by an entity handler. */
public record RoomEntityObjectSample(int objectId, int physicsFlag, int objectLeft, int objectTop) {
    public RoomEntityObjectSample {
        objectId &= 0xFF;
        physicsFlag &= 0xFF;
        objectLeft &= 0xF0;
        objectTop &= 0xF0;
    }

    private static final RoomEntityObjectSample NONE = new RoomEntityObjectSample(0xFF, 0, 0, 0);

    /** No query was performed; distinct from a sampled $FF object cell. */
    public static final RoomEntityObjectSample UNSAMPLED = new RoomEntityObjectSample(0xFF, 0, 0, 0);

    public static RoomEntityObjectSample orAbsent(RoomEntityObjectSample value) {
        return java.util.Objects.requireNonNullElse(value, UNSAMPLED);
    }

    public boolean sampled() {
        return this != UNSAMPLED;
    }

    public static RoomEntityObjectSample none() {
        return NONE;
    }
}
