package linksawakening.world;

/**
 * Shared sword-recoil state used by the bank-$03 roaming enemies and the
 * bank-$06 Hard Hat Beetle handler.
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
        // GetEntityYDistanceToLink leaves the zero case on its UP branch;
        // GetVectorTowardsLink therefore negates a zero-distance Y result.
        if (distanceY <= 0) {
            vectorY = -vectorY;
        }

        // ConfigureEntityRecoil stores the negated vector: away from Link.
        recoilSpeedX[slot] = (-vectorX) & 0xFF;
        recoilSpeedY[slot] = (-vectorY) & 0xFF;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        active[slot] = true;
    }

    /** Mirrors func_003_77A7 copying an active projectile's speed to recoil. */
    void configureFromSpeed(int slot, int speedX, int speedY) {
        validateSlot(slot);
        recoilSpeedX[slot] = speedX & 0xFF;
        recoilSpeedY[slot] = speedY & 0xFF;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        active[slot] = true;
    }

    /**
     * Mirrors GetVectorTowardsOtherEntity's final bomb-explosion write.
     * Unlike {@link #configure}, the vector points from the active source
     * entity towards the target and is stored without negation.
     */
    void configureFromSource(int slot, int sourceX, int sourceY, int sourceZ,
                             int targetX, int targetY, int length) {
        validateSlot(slot);
        if (length < 0 || length > 0xFF) {
            throw new IllegalArgumentException("Recoil length must be an unsigned byte");
        }

        int distanceX = signedByte((targetX - sourceX) & 0xFF);
        int distanceY = signedByte((targetY - sourceY + sourceZ) & 0xFF);
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
        // GetEntityYDistanceToLink starts on the UP branch for a negative
        // distance, but treats zero as the nonnegative/DOWN branch.
        if (distanceY < 0) {
            vectorY = -vectorY;
        }

        recoilSpeedX[slot] = vectorX & 0xFF;
        recoilSpeedY[slot] = vectorY & 0xFF;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        active[slot] = true;
    }

    /** Applies one ROM fixed-point recoil step and returns a new entity value. */
    Update advance(RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision) {
        return advance(entity, backgroundCollision, true);
    }

    /**
     * Applies one recoil step, optionally preserving state after a background
     * block. Bank-$03 calls StopEntityRecoilOnCollision; bank-$06 Hard Hat does
     * not, so its handler uses {@code clearOnBlocked == false}.
     */
    Update advance(RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision,
                   boolean clearOnBlocked) {
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
        if (blocked && clearOnBlocked) {
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
        if (length == 0) {
            return 0;
        }
        // With both distances zero, the ROM's compare-with-zero path enters
        // the quotient increment on every iteration.
        if (largerDistance == 0) {
            return length;
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
