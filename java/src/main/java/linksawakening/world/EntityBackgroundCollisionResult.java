package linksawakening.world;

/**
 * The value returned by the entity/background probe corresponding to the
 * disassembly's {@code ApplyEntityCollisionWithObject} call.
 */
public record EntityBackgroundCollisionResult(
    boolean blocked,
    int objectId,
    int physicsFlag,
    int direction,
    int collisionFlag,
    int sampleX,
    int sampleY) {

    public static final int NO_OBJECT = 0xFF;
    public static final int RIGHT = 0;
    public static final int LEFT = 1;
    public static final int UP = 2;
    public static final int DOWN = 3;

    public EntityBackgroundCollisionResult {
        if (direction < RIGHT || direction > DOWN) {
            throw new IllegalArgumentException("Entity collision direction must be 0..3: "
                + direction);
        }
        objectId &= 0xFF;
        physicsFlag &= 0xFF;
        collisionFlag &= 0xFF;
        sampleX &= 0xFF;
        sampleY &= 0xFF;
        int expectedFlag = blocked ? collisionFlagForDirection(direction) : 0;
        if (collisionFlag != expectedFlag) {
            throw new IllegalArgumentException("Collision flag does not match probe result: "
                + collisionFlag + " != " + expectedFlag);
        }
    }

    public static EntityBackgroundCollisionResult blocked(int direction, int objectId,
                                                            int physicsFlag,
                                                            int sampleX, int sampleY) {
        return new EntityBackgroundCollisionResult(true, objectId, physicsFlag, direction,
            collisionFlagForDirection(direction), sampleX, sampleY);
    }

    public static EntityBackgroundCollisionResult passable(int direction, int physicsFlag,
                                                             int sampleX, int sampleY) {
        return passableWithObject(direction, NO_OBJECT, physicsFlag, sampleX, sampleY);
    }

    public static EntityBackgroundCollisionResult passableWithObject(int direction,
                                                                      int objectId,
                                                                      int physicsFlag,
                                                                      int sampleX,
                                                                      int sampleY) {
        return new EntityBackgroundCollisionResult(false, objectId, physicsFlag, direction,
            0, sampleX, sampleY);
    }

    public static EntityBackgroundCollisionResult fromBoolean(boolean blocked, int direction,
                                                                int sampleX, int sampleY) {
        return blocked
            ? blocked(direction, NO_OBJECT, 0, sampleX, sampleY)
            : passable(direction, 0, sampleX, sampleY);
    }

    /** Mirrors {@code CollisionsTableFlagPerDirection} in bank $03. */
    public static int collisionFlagForDirection(int direction) {
        return switch (direction) {
            case RIGHT -> 0x01;
            case LEFT -> 0x02;
            case UP -> 0x04;
            case DOWN -> 0x08;
            default -> throw new IllegalArgumentException(
                "Entity collision direction must be 0..3: " + direction);
        };
    }
}
