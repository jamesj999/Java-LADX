package linksawakening.world.entity;



import linksawakening.world.entity.RoomEntityGroundInteraction;
import linksawakening.world.entity.RoomEntity;

public final class EntityGroundTestAccess {
    private EntityGroundTestAccess() {}
    public static RoomEntityGroundInteraction.Result unchanged(RoomEntity entity, int status) {
        return RoomEntityGroundInteraction.Result.unchanged(entity, status);
    }
}
