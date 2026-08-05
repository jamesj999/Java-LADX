package linksawakening.world;

import java.util.Objects;

/** Rich entity/background probe corresponding to the ROM collision helper. */
@FunctionalInterface
public interface RoomEntityBackgroundInteraction {
    EntityBackgroundCollisionResult probe(RoomEntity entity, int direction,
                                           int nextX, int nextY);

    static RoomEntityBackgroundInteraction fromBoolean(
            RoomEntityBackgroundCollision backgroundCollision) {
        Objects.requireNonNull(backgroundCollision, "backgroundCollision");
        return (entity, direction, nextX, nextY) ->
            EntityBackgroundCollisionResult.fromBoolean(
                backgroundCollision.blocks(entity, direction, nextX, nextY),
                direction, nextX, nextY);
    }
}
