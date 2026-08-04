package linksawakening.world;

/** Room/map inputs used by bank $01's follower-exclusion checks. */
public record FollowingNpcRoomContext(boolean indoor, boolean sideScrolling,
                                      int mapId, int roomId) {

    public FollowingNpcRoomContext {
        if (mapId < 0 || mapId > 0xFF || roomId < 0 || roomId > 0xFF) {
            throw new IllegalArgumentException("Follower room context must use unsigned bytes");
        }
    }

    public static FollowingNpcRoomContext overworld(int roomId) {
        return new FollowingNpcRoomContext(false, false, 0, roomId);
    }

    public static FollowingNpcRoomContext indoor(int mapId, int roomId) {
        return new FollowingNpcRoomContext(true, false, mapId, roomId);
    }
}
