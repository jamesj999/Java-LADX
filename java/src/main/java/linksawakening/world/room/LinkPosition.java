package linksawakening.world.room;

import linksawakening.state.Direction;

/** Link facing is closed; rawDirection preserves legacy/ROM probe bytes without normalization. */
public record LinkPosition(int x, int y, int z, Direction direction, int collisionType, int rawDirection) {
    public LinkPosition {
        direction = Direction.fromJavaOrUnknown(rawDirection);
    }

    public LinkPosition(int x, int y, int z, Direction direction, int collisionType) {
        this(x, y, z, direction, collisionType, direction.javaIndex());
    }
    /** @deprecated Pass Direction; retained for app/gameplay/EntityFrameInputsFactory.java. */
    @Deprecated(forRemoval = true)
    public LinkPosition(int x, int y, int z, int direction, int collisionType) {
        this(x, y, z, Direction.fromJavaOrUnknown(direction), collisionType, direction);
    }
}
