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
        // Optional room geometry/history; null preserves the path without that room input.
        LinkPositionHistory history) {}
