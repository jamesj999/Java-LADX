package linksawakening.world;

/**
 * Bank-$03's shared sword-recoil state for ordinary roaming enemies.
 *
 * <p>The ROM stores entity speeds as signed pixels per sixteen frames and
 * keeps a separate eight-bit fractional accumulator for each axis. This
 * helper keeps that handler-owned state outside the immutable room snapshot.
 * It intentionally models only the shared recoil path; family-specific motion
 * remains in the corresponding entity handler.</p>
 */
final class EnemyRecoilMotion {
    private final int[] recoilSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] recoilSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] active = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record Update(RoomEntity entity, boolean blocked) {
    }

    /** Mirrors ConfigureEntityRecoil after GetVectorTowardsLink returns. */
    void configure(int slot, int entityX, int entityY, int entityZ,
                   int linkX, int linkY, int length) {
        validateSlot(slot);
        if (length < 0 || length > 0xFF) {
            throw new IllegalArgumentException("Recoil length must be an unsigned byte");
        }

        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY + entityZ) & 0xFF);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean xIsDominant = absoluteX >= absoluteY;
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int smallerComponent = romDivide(length, smallerDistance, largerDistance);

        int vectorX = xIsDominant ? length : smallerComponent;
        int vectorY = xIsDominant ? smallerComponent : length;
        if (distanceX < 0) {
            vectorX = -vectorX;
        }
        if (distanceY < 0) {
            vectorY = -vectorY;
        }

        // ConfigureEntityRecoil stores the negated vector: away from Link.
        recoilSpeedX[slot] = (-vectorX) & 0xFF;
        recoilSpeedY[slot] = (-vectorY) & 0xFF;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        active[slot] = true;
    }

    /** Applies one ROM fixed-point recoil step and returns a new entity value. */
    Update advance(RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (!active[slot]) {
            return new Update(entity, false);
        }

        int x = addSpeedToPosition(entity.x(), recoilSpeedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), recoilSpeedY[slot], speedYAccumulator, slot);
        boolean blocked = false;
        if (backgroundCollision != null) {
            if (x != entity.x()
                && backgroundCollision.blocks(entity, horizontalDirection(recoilSpeedX[slot]),
                    x, entity.y())) {
                x = entity.x();
                blocked = true;
            }
            if (y != entity.y()
                && backgroundCollision.blocks(entity, verticalDirection(recoilSpeedY[slot]),
                    x, y)) {
                y = entity.y();
                blocked = true;
            }
        }
        if (blocked) {
            clear(slot);
        }

        return new Update(withPosition(entity, x, y), blocked);
    }

    boolean isActive(int slot) {
        validateSlot(slot);
        return active[slot];
    }

    int recoilSpeedX(int slot) {
        validateSlot(slot);
        return recoilSpeedX[slot];
    }

    int recoilSpeedY(int slot) {
        validateSlot(slot);
        return recoilSpeedY[slot];
    }

    void clear(int slot) {
        validateSlot(slot);
        recoilSpeedX[slot] = 0;
        recoilSpeedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        active[slot] = false;
    }

    /** Port of GetVectorTowardsLink's repeated-remainder division. */
    private static int romDivide(int length, int smallerDistance, int largerDistance) {
        if (length == 0 || largerDistance == 0) {
            return 0;
        }
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum;
        }
        return result;
    }

    /** Port of AddEntitySpeedToPos_03 for one position/accumulator pair. */
    private static int addSpeedToPosition(int position, int speed, int[] accumulator,
                                          int slot) {
        speed &= 0xFF;
        if (speed == 0) {
            return position & 0xFF;
        }

        int fractionalSum = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = fractionalSum & 0xFF;
        int delta = signedByte(speed) >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    private static int horizontalDirection(int speed) {
        return (speed & 0x80) != 0 ? 1 : 0;
    }

    private static int verticalDirection(int speed) {
        return (speed & 0x80) != 0 ? 2 : 3;
    }

    private static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x, y, entity.status(), entity.spriteDefinition(), entity.spriteVariant(),
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
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
