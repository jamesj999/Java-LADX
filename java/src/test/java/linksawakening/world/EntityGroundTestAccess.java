package linksawakening.world;

public final class EntityGroundTestAccess {
    private EntityGroundTestAccess() {}
    public static RoomEntityGroundInteraction.Result unchanged(RoomEntity entity, int status) {
        return RoomEntityGroundInteraction.Result.unchanged(entity, status);
    }
}
