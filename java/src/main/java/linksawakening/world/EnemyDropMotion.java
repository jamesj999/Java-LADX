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
        // BouncingEntityPhysics updates both planar positions before applying
        // top-view Z gravity or side-view Y gravity ($60B3 -> $6B7B).
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        if (sideScrolling) {
            EntitySideViewGravity.Speeds speeds = EntitySideViewGravity.advance(
                speedX[slot], speedY[slot], frameCounter, previousGroundStatus);
            speedX[slot] = speeds.x();
            speedY[slot] = speeds.y();
            return withPosition(entity, x, y, entity.z());
        }

        int z = addSpeedToPosition(entity.z(), speedZ[slot], speedZAccumulator, slot);
        speedZ[slot] = (speedZ[slot] - TOP_DOWN_GRAVITY) & 0xFF;
        return withPosition(entity, x, y, z);
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
            if (bouncedSpeed < 0xF8) {
                speedY[slot] = bouncedSpeed;
                speedX[slot] = halvePlanarSpeed(speedX[slot]);
            } else {
                speedX[slot] = 0;
                speedY[slot] = 0;
            }
            return withY(entity, y);
        }

        if ((entity.z() & 0x80) == 0) {
            return entity;
        }

        RoomEntity landed = withZ(entity, 0);
        int bouncedSpeed = complementAfterArithmeticShift(speedZ[slot]);
        if (EntitySideViewGravity.groundStatusIndex(groundStatus) == SHALLOW_WATER_GROUND_STATUS
            || bouncedSpeed < 0x07) {
            speedX[slot] = 0;
            speedY[slot] = 0;
            speedZ[slot] = 0;
            return landed;
        }

        speedZ[slot] = bouncedSpeed;
        speedX[slot] = halvePlanarSpeed(speedX[slot]);
        speedY[slot] = halvePlanarSpeed(speedY[slot]);
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

    /** $6137/$6149 clear the signed-shift result $FF so tiny drifts stop. */
    private static int halvePlanarSpeed(int speed) {
        int halved = (signedByte(speed) >> 1) & 0xFF;
        return halved == 0xFF ? 0 : halved;
    }

    private static RoomEntity withY(RoomEntity entity, int y) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            entity.x(), y & 0xFF, entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            entity.z());
    }

    private static RoomEntity withPosition(RoomEntity entity, int x, int y, int z) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(),
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
