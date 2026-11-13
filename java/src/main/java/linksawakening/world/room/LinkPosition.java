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
}
