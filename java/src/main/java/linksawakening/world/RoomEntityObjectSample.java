package linksawakening.world;

/** The padded-room object cell sampled by an entity handler. */
public record RoomEntityObjectSample(int objectId, int physicsFlag,
                                     int objectLeft, int objectTop) {
    public RoomEntityObjectSample {
        objectId &= 0xFF;
        physicsFlag &= 0xFF;
        objectLeft &= 0xF0;
        objectTop &= 0xF0;
    }

    public static RoomEntityObjectSample none() {
        return new RoomEntityObjectSample(0xFF, 0, 0, 0);
    }
}
