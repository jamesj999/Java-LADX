package linksawakening.world;

/**
 * Source-backed static room-object query for handlers that do not use the
 * ordinary one-axis background movement callback.
 */
@FunctionalInterface
public interface RoomEntityObjectCollision {
    boolean collides(RoomEntity entity);
}
