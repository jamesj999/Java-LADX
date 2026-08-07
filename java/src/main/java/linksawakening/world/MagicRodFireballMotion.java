package linksawakening.world;

/**
 * Handler-owned state for the ROM's Magic Rod fireball entity ({@code $04}).
 *
 * <p>The projectile uses the generic player-projectile speed tables and the
 * bank-$03 sixteen-subpixel position accumulator.  A solid wall changes the
 * entity into the shared fire display for {@code $30} handler ticks; the
 * private countdown is deliberately kept here rather than inferred from the
 * Java entity status.</p>
 */
final class MagicRodFireballMotion {
    static final int ENTITY_TYPE = 0x04;
    static final int FIRE_TRANSITION_COUNTDOWN = 0x30;

    private static final int[] OFFSET_X = {0x00, 0x00, 0x00, 0x00};
    private static final int[] OFFSET_Y = {0x00, 0x00, 0x00, 0x00};
    private static final int[] SPEED_X = {0x20, 0xE0, 0x00, 0x00};
    private static final int[] SPEED_Y = {0x00, 0x00, 0xE0, 0x20};

    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record SpawnData(int offsetX, int offsetY, int speedX, int speedY, int initialVariant) {}

    static SpawnData spawnData(int projectileDirection) {
        int direction = checkedDirection(projectileDirection);
        return new SpawnData(OFFSET_X[direction], OFFSET_Y[direction],
            SPEED_X[direction], SPEED_Y[direction], direction);
    }

    static int frameVariant(int frameCounter) {
        return (frameCounter >>> 3) & 0x01;
    }

    void initializeSpawn(int slot, int projectileDirection) {
        validateSlot(slot);
        int direction = checkedDirection(projectileDirection);
        this.direction[slot] = direction;
        speedX[slot] = SPEED_X[direction];
        speedY[slot] = SPEED_Y[direction];
        privateCountdown1[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = true;
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

    void beginFireTransition(int slot) {
        validateSlot(slot);
        if (!initialized[slot]) {
            initializeSpawn(slot, direction[slot]);
        }
        privateCountdown1[slot] = FIRE_TRANSITION_COUNTDOWN;
    }

    /** Decrements the ROM private countdown and reports the exact unload tick. */
    boolean tickFireTransition(int slot) {
        validateSlot(slot);
        if (privateCountdown1[slot] == 0) {
            return true;
        }
        privateCountdown1[slot] = (privateCountdown1[slot] - 1) & 0xFF;
        return privateCountdown1[slot] == 0;
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

    int privateCountdown1(int slot) {
        validateSlot(slot);
        return privateCountdown1[slot];
    }

    void clear(int slot) {
        validateSlot(slot);
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        privateCountdown1[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = false;
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
            throw new IllegalArgumentException("Magic Rod direction out of range: " + direction);
        }
        return direction;
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
