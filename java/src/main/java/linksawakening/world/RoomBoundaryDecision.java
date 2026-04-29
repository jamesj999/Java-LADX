package linksawakening.world;

public record RoomBoundaryDecision(Type type,
                                   int direction,
                                   int linkTargetX,
                                   int linkTargetY) {
    public enum Type {
        NONE,
        CLAMP_LINK,
        OVERWORLD_SCROLL,
        INDOOR_SCROLL,
        INDOOR_FRONT_DOOR_WARP
    }

    public static RoomBoundaryDecision none() {
        return new RoomBoundaryDecision(Type.NONE, ScrollController.NONE, 0, 0);
    }

    public static RoomBoundaryDecision clamp(int x, int y) {
        return new RoomBoundaryDecision(Type.CLAMP_LINK, ScrollController.NONE, x, y);
    }

    public static RoomBoundaryDecision overworldScroll(int direction, int targetX, int targetY) {
        return new RoomBoundaryDecision(Type.OVERWORLD_SCROLL, direction, targetX, targetY);
    }

    public static RoomBoundaryDecision indoorScroll(int direction, int targetX, int targetY) {
        return new RoomBoundaryDecision(Type.INDOOR_SCROLL, direction, targetX, targetY);
    }

    public static RoomBoundaryDecision indoorFrontDoorWarp() {
        return new RoomBoundaryDecision(Type.INDOOR_FRONT_DOOR_WARP, ScrollController.NONE, 0, 0);
    }
}
