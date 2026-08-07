package linksawakening.world;

/**
 * Handler-owned state for the ROM's player boomerang entity ({@code $01}).
 *
 * <p>The entity uses the same sixteen-subpixel speed accumulator as the other
 * bank-$19 entities, but changes its vector in two ROM-defined phases: a
 * player-facing launch for {@code $28} frames followed by a vector toward
 * Link. The room runtime owns collision and side effects; this class keeps the
 * byte-sized motion state faithful and independently testable.</p>
 */
final class BoomerangMotion {
    static final int ENTITY_TYPE = 0x01;
    static final int INITIAL_COUNTDOWN = 0x28;

    private static final int[] OFFSET_X = {0x00, 0x00, 0x00, 0x00};
    private static final int[] OFFSET_Y = {0x00, 0x00, 0x00, 0x00};
    private static final int[] SPEED_X = {0x20, 0xE0, 0x00, 0x00};
    private static final int[] SPEED_Y = {0x00, 0x00, 0xE0, 0x20};
    private static final int[] DIAGONAL_SPEED_X = {0x00, 0x18, 0xE8, 0x00};
    private static final int[] DIAGONAL_SPEED_Y = {0x00, 0xE8, 0x18, 0x00};

    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record SpawnData(int offsetX, int offsetY, int speedX, int speedY, int initialVariant) {}

    static SpawnData spawnData(int projectileDirection) {
        int direction = checkedDirection(projectileDirection);
        return new SpawnData(OFFSET_X[direction], OFFSET_Y[direction],
            SPEED_X[direction], SPEED_Y[direction], direction);
    }

    void initializeSpawn(int slot, int projectileDirection, int pressedButtonsMask) {
        validateSlot(slot);
        int direction = checkedDirection(projectileDirection);
        this.direction[slot] = direction;
        speedX[slot] = SPEED_X[direction];
        speedY[slot] = SPEED_Y[direction];
        transitionCountdown[slot] = INITIAL_COUNTDOWN;
        state[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = true;

        int mask = pressedButtonsMask & 0x0F;
        if (Integer.bitCount(mask) >= 2) {
            // func_020_4BFF indexes Data_020_4BF9 from one byte before its
            // label. The two unused horizontal combinations resolve to the
            // zero byte adjacent to that table in the assembled image.
            speedX[slot] = DIAGONAL_SPEED_X[mask & 0x03];
            speedY[slot] = DIAGONAL_SPEED_Y[(mask >>> 2) & 0x03];
        }
    }

    void initializeSpawn(int slot, int projectileDirection) {
        initializeSpawn(slot, projectileDirection, 0);
    }

    RoomEntity advancePosition(RoomEntity entity) {
        validateSlot(entity.slot());
        int slot = entity.slot();
        if (!initialized[slot]) {
            initializeSpawn(slot, entity.spriteVariant() < 0 ? 0 : entity.spriteVariant());
        }
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        return withPosition(entity, x, y);
    }

    void decrementTransitionCountdown(int slot) {
        validateSlot(slot);
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }
    }

    void setVectorTowardsLink(int slot, RoomEntity entity, int linkX, int linkY,
                              int linkZ, int vectorLength) {
        validateSlot(slot);
        if (!initialized[slot]) {
            initializeSpawn(slot, direction[slot]);
        }
        int length = vectorLength & 0xFF;
        if (length == 0) {
            speedX[slot] = 0;
            speedY[slot] = 0;
            return;
        }

        int dx = signedByte((linkX & 0xFF) - (entity.x() & 0xFF));
        int dy = signedByte((linkY & 0xFF) - (entity.y() & 0xFF) + (linkZ & 0xFF));
        int absoluteX = Math.abs(dx);
        int absoluteY = Math.abs(dy);
        boolean swapped = absoluteX < absoluteY;
        int smaller = swapped ? absoluteX : absoluteY;
        int larger = swapped ? absoluteY : absoluteX;
        int remainder = 0;
        int smallerResult = 0;
        for (int index = 0; index < length; index++) {
            remainder += smaller;
            if (remainder >= larger) {
                remainder -= larger;
                smallerResult++;
            }
        }

        int resultX = swapped ? smallerResult : length;
        int resultY = swapped ? length : smallerResult;
        speedX[slot] = signedResult(resultX, dx < 0);
        speedY[slot] = signedResult(resultY, dy < 0);
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
    }

    int direction(int slot) {
        validateSlot(slot);
        return direction[slot];
    }

    int speedX(int slot) {
        validateSlot(slot);
        return speedX[slot];
    }

    int speedY(int slot) {
        validateSlot(slot);
        return speedY[slot];
    }

    int transitionCountdown(int slot) {
        validateSlot(slot);
        return transitionCountdown[slot];
    }

    int state(int slot) {
        validateSlot(slot);
        return state[slot];
    }

    void setState(int slot, int newState) {
        validateSlot(slot);
        if (newState < 0 || newState > 0xFF) {
            throw new IllegalArgumentException("Boomerang state must be an unsigned byte: "
                + newState);
        }
        state[slot] = newState & 0xFF;
    }

    void setTransitionCountdown(int slot, int countdown) {
        validateSlot(slot);
        if (countdown < 0 || countdown > 0xFF) {
            throw new IllegalArgumentException("Boomerang countdown must be an unsigned byte: "
                + countdown);
        }
        transitionCountdown[slot] = countdown;
    }

    /** Mirrors CheckLinkCollisionWithEnemy's normal entity hitbox window. */
    static boolean overlapsLink(RoomEntity entity, int linkX, int linkY) {
        if (entity == null) {
            throw new IllegalArgumentException("Boomerang entity cannot be null");
        }
        int xDistance = unsignedByteAbs(entity.x() + 0x08 - linkX - 0x08);
        if (xDistance >= 0x09) {
            return false;
        }
        int yDistance = unsignedByteAbs(
            entity.y() - entity.z() + 0x08 - linkY - 0x08);
        return yDistance < 0x09;
    }

    void clear(int slot) {
        validateSlot(slot);
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        transitionCountdown[slot] = 0;
        state[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = false;
    }

    /**
     * Mirrors the broad physics-window in {@code ApplySwordIntersectionWithObjects}.
     * Zero, pit/water warp markers, fine-collision placeholders, and the
     * high non-solid object range are passable to this projectile.
     */
    static boolean objectPhysicsCollides(int physicsFlag) {
        int value = physicsFlag & 0xFF;
        if (value == 0 || value == 0x50 || value == 0x51
            || (value >= 0x7C && value < 0x90)
            || (value >= 0xA0 && value != 0xFF)) {
            return false;
        }
        return value == 0x01 || (value >= 0x10 && value < 0x7C)
            || (value >= 0x90 && value < 0xA0)
            || (value >= 0xD0 && value <= 0xD3)
            || value == 0xFF;
    }

    private static int signedResult(int magnitude, boolean negative) {
        int value = magnitude & 0xFF;
        return negative ? (-value) & 0xFF : value;
    }

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

    private static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), entity.spriteVariant(),
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z(),
            entity.deathSpriteVariant(), entity.powerRecoilDeath());
    }

    private static int checkedDirection(int direction) {
        if (direction < 0 || direction > 3) {
            throw new IllegalArgumentException("Boomerang direction out of range: " + direction);
        }
        return direction;
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private static int unsignedByteAbs(int value) {
        int difference = value & 0xFF;
        return difference < 0x80 ? difference : 0x100 - difference;
    }

    private static void validateSlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
    }
}
