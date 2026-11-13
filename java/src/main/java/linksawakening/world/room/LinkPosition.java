package linksawakening.world.room;

import linksawakening.state.Direction;

/** Link coordinates and an unnormalized direction probe byte; unknown bytes remain observable. */
public record LinkPosition(int x, int y, int z, int rawDirection, int collisionType) {
    public Direction direction() {
        return Direction.fromJavaOrUnknown(rawDirection);
    }

    public Direction knownDirection() {
        return direction();
    }
    /** Compatibility: rawDirection has always been authoritative. */
    @Deprecated(forRemoval = true)
    public LinkPosition(int x, int y, int z, Direction direction, int collisionType, int rawDirection) {
        this(x, y, z, rawDirection, collisionType);
    }
    /** Compatibility: callers with a closed facing can pass its javaIndex directly. */
    @Deprecated(forRemoval = true)
    public LinkPosition(int x, int y, int z, Direction direction, int collisionType) {
        this(x, y, z, direction.javaIndex(), collisionType);
    }
}
