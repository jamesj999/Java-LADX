package linksawakening.world;

public record RoomBoundaryState(int mapCategory,
                                int roomId,
                                boolean indoorHasSouthEntrance,
                                boolean hasWarps,
                                int shutterDoorMask,
                                int linkX,
                                int linkY) {
    public RoomBoundaryState(int mapCategory, int roomId, boolean indoorHasSouthEntrance,
                             boolean hasWarps, int linkX, int linkY) {
        this(mapCategory, roomId, indoorHasSouthEntrance, hasWarps, 0, linkX, linkY);
    }
}
