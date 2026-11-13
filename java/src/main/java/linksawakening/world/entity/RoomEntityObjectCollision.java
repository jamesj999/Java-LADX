package linksawakening.world.entity;

/**
 * Source-backed static room-object query for handlers that do not use the
 * ordinary one-axis background movement callback.
 */
@FunctionalInterface
public interface RoomEntityObjectCollision {
    RoomEntityObjectCollision NONE = entity -> false;

    static RoomEntityObjectCollision orNone(RoomEntityObjectCollision value) {
        return java.util.Objects.requireNonNullElse(value, NONE);
    }

    boolean collides(RoomEntity entity);
}
