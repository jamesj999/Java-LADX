package linksawakening.world;

/**
 * Bank-$03's common {@code BouncingEntityPhysics} state for an enemy drop.
 *
 * <p>The ROM keeps entity speeds in pixels per sixteen frames and stores a
 * separate fractional accumulator for each position axis. This helper owns
 * only the common drop's speed-X/Y/Z tables.</p>
 */
final class EnemyDropMotion {
    private static final int TOP_DOWN_GRAVITY = 0x02;
    private static final int SHALLOW_WATER_GROUND_STATUS = 0x02;
    private static final int[] SIDE_SCROLL_GRAVITY = {0x02, 0x01, 0x02, 0x02};
    private static final int[] SIDE_SCROLL_SPEED_CAP = {0x40, 0x08, 0x40, 0x40};

    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot, boolean sideScrolling) {
        initialize(slot, sideScrolling, 0x18);
    }

    /** Initializes a top-down drop with the source-specific initial Z speed. */
    void initialize(int slot, boolean sideScrolling, int initialSpeedZ) {
        validateSlot(slot);
        if (initialSpeedZ < 0 || initialSpeedZ > 0xFF) {
            throw new IllegalArgumentException("Initial drop Z speed out of range: "
                + initialSpeedZ);
        }
        speedX[slot] = 0;
        speedY[slot] = sideScrolling ? 0xEC : 0;
        speedZ[slot] = sideScrolling ? 0 : initialSpeedZ;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
    }

    /**
     * Port of the source's ApplyVectorTowardsLink followed by the shovel
     * branch's two's-complement writes. The item scatters the reward away
     * from Link using the ROM's infinity-norm vector approximation.
     */
    void initializeAwayFromLink(int slot, int entityX, int entityY,
                                int linkX, int linkY, int vectorLength) {
        validateSlot(slot);
        if (vectorLength < 0 || vectorLength > 0xFF) {
            throw new IllegalArgumentException("Drop vector length out of range: "
                + vectorLength);
        }
        int dx = signedByte((linkX - entityX) & 0xFF);
        int dy = signedByte((linkY - entityY) & 0xFF);
        int absX = Math.abs(dx);
        int absY = Math.abs(dy);
        boolean swapped = absX < absY;
        int smaller = swapped ? absX : absY;
        int larger = swapped ? absY : absX;
        int minorResult;
        if (larger == 0) {
            minorResult = vectorLength;
        } else {
            int remainder = 0;
            minorResult = 0;
            for (int i = 0; i < vectorLength; i++) {
                remainder += smaller;
                if (remainder >= larger) {
                    remainder -= larger;
                    minorResult++;
                }
            }
        }

        int towardX = swapped ? minorResult : vectorLength;
        int towardY = swapped ? vectorLength : minorResult;
        if (dx < 0) {
            towardX = (-towardX) & 0xFF;
        }
        if (dy < 0) {
            towardY = (-towardY) & 0xFF;
        }
        speedX[slot] = (-towardX) & 0xFF;
        speedY[slot] = (-towardY) & 0xFF;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
    }

    RoomEntity advance(RoomEntity entity, int frameCounter, int previousGroundStatus,
                       boolean sideScrolling) {
        int slot = entity.slot();
        validateSlot(slot);
        if (sideScrolling) {
            int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
            int status = groundStatusIndex(previousGroundStatus);
            speedY[slot] = (speedY[slot] + SIDE_SCROLL_GRAVITY[status]) & 0xFF;
            int cap = SIDE_SCROLL_SPEED_CAP[status];
            if (((speedY[slot] - cap) & 0x80) == 0) {
                speedY[slot] = cap;
            }
            return withY(entity, y);
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int z = addSpeedToPosition(entity.z(), speedZ[slot], speedZAccumulator, slot);
        speedZ[slot] = (speedZ[slot] - TOP_DOWN_GRAVITY) & 0xFF;
        return withXZ(entity, x, z);
    }

    RoomEntity bounce(RoomEntity entity, int groundStatus, boolean sideScrolling,
                      boolean groundCollision) {
        int slot = entity.slot();
        validateSlot(slot);
        if (sideScrolling) {
            if (!groundCollision) {
                return entity;
            }

            int y = ((entity.y() & 0xF0) + 0x05) & 0xFF;
            int bouncedSpeed = complementAfterArithmeticShift(speedY[slot]);
            speedY[slot] = bouncedSpeed < 0xF8 ? bouncedSpeed : 0;
            return withY(entity, y);
        }

        if ((entity.z() & 0x80) == 0) {
            return entity;
        }

        RoomEntity landed = withZ(entity, 0);
        if (groundStatusIndex(groundStatus) == SHALLOW_WATER_GROUND_STATUS) {
            speedY[slot] = 0;
            speedZ[slot] = 0;
            return landed;
        }

        int bouncedSpeed = complementAfterArithmeticShift(speedZ[slot]);
        speedZ[slot] = bouncedSpeed >= 0x07 ? bouncedSpeed : 0;
        speedY[slot] = 0;
        return landed;
    }

    int speedY(int slot) {
        validateSlot(slot);
        return speedY[slot];
    }

    int speedX(int slot) {
        validateSlot(slot);
        return speedX[slot];
    }

    int speedZ(int slot) {
        validateSlot(slot);
        return speedZ[slot];
    }

    void clear(int slot) {
        validateSlot(slot);
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator,
                                          int slot) {
        speed &= 0xFF;
        if (speed == 0) {
            return position & 0xFF;
        }

        int fractionalSum = accumulator[slot] + (((speed << 4) & 0xFF) & 0xF0);
        accumulator[slot] = fractionalSum & 0xFF;
        int delta = signedByte(speed) >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    private static int complementAfterArithmeticShift(int value) {
        int shifted = signedByte(value) >> 1;
        return (~shifted) & 0xFF;
    }

    private static int groundStatusIndex(int groundStatus) {
        int status = groundStatus & 0xFF;
        if (status >= SIDE_SCROLL_GRAVITY.length) {
            throw new IllegalArgumentException("Entity ground status out of range: " + status);
        }
        return status;
    }

    private static RoomEntity withY(RoomEntity entity, int y) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            entity.x(), y & 0xFF, entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            entity.z());
    }

    private static RoomEntity withXZ(RoomEntity entity, int x, int z) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, entity.y(), entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            z & 0xFF);
    }

    private static RoomEntity withZ(RoomEntity entity, int z) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            entity.x(), entity.y(), entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            z & 0xFF);
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private static void validateSlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
    }
}
