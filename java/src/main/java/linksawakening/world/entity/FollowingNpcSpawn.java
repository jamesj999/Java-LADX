package linksawakening.world.entity;

/** Inputs for FollowingNpcEntitySpawner.synchronize. */
public record FollowingNpcSpawn(
        RoomEntitySnapshot initial,
        FollowingNpcRoomContext room,
        FollowingNpcState requestedState,
        int linkX,
        int linkY,
        int linkZ,
        int entityYOffset,
        int linkDirection,
        LinkPositionHistory history) {}
