package linksawakening.world;

public record RoomBoundaryState(int mapCategory,
                                int roomId,
                                boolean indoorHasSouthEntrance,
                                boolean hasWarps,
                                int shutterDoorMask,
                                int linkX,
                                int linkY,
                                int mapId,
                                boolean linkAirborne,
                                int linkPhysicsModifier,
                                boolean sideViewEntityActive) {
    public RoomBoundaryState(int mapCategory, int roomId, boolean indoorHasSouthEntrance,
                             boolean hasWarps, int shutterDoorMask, int linkX, int linkY) {
        this(mapCategory, roomId, indoorHasSouthEntrance, hasWarps,
            shutterDoorMask, linkX, linkY, -1, false, 0, false);
    }

    public RoomBoundaryState(int mapCategory, int roomId, boolean indoorHasSouthEntrance,
                             boolean hasWarps, int linkX, int linkY) {
        this(mapCategory, roomId, indoorHasSouthEntrance, hasWarps, 0, linkX, linkY,
            -1, false, 0, false);
    }

    public RoomBoundaryState(int mapCategory, int roomId, boolean indoorHasSouthEntrance,
                             boolean hasWarps, int shutterDoorMask, int linkX, int linkY,
                             int mapId) {
        this(mapCategory, roomId, indoorHasSouthEntrance, hasWarps, shutterDoorMask,
            linkX, linkY, mapId, false, 0, false);
    }

    public RoomBoundaryState(int mapCategory, int roomId, boolean indoorHasSouthEntrance,
                             boolean hasWarps, int shutterDoorMask, int linkX, int linkY,
                             int mapId, boolean linkAirborne) {
        this(mapCategory, roomId, indoorHasSouthEntrance, hasWarps, shutterDoorMask,
            linkX, linkY, mapId, linkAirborne, 0, false);
    }

    public RoomBoundaryState(int mapCategory, int roomId, boolean indoorHasSouthEntrance,
                             boolean hasWarps, int shutterDoorMask, int linkX, int linkY,
                             int mapId, boolean linkAirborne, int linkPhysicsModifier) {
        this(mapCategory, roomId, indoorHasSouthEntrance, hasWarps, shutterDoorMask,
            linkX, linkY, mapId, linkAirborne, linkPhysicsModifier, false);
    }
}
