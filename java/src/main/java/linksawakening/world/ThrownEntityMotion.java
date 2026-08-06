package linksawakening.world;

/**
 * ROM-shaped speed and fixed-point motion for the generic thrown-entity path.
 * The tables are Data_014_5313, Data_014_5323, and Data_014_5333 from bank
 * $14; the per-slot accumulators mirror the WRAM speed accumulator tables.
 */
final class ThrownEntityMotion {
    static final int ROM_DIRECTION_RIGHT = 0;
    static final int ROM_DIRECTION_LEFT = 1;
    static final int ROM_DIRECTION_UP = 2;
    static final int ROM_DIRECTION_DOWN = 3;

    private static final int ENTITY_BOMB = 0x02;

    private static final int[] SPEED_X = {
        0x30, 0xD0, 0x00, 0x00, 0x18, 0xE8, 0x00, 0x00,
        0x30, 0xD0, 0x00, 0x00, 0x18, 0xE8, 0x00, 0x00
    };
    private static final int[] SPEED_Y = {
        0x00, 0x00, 0xD0, 0x30, 0x00, 0x00, 0xE8, 0x18,
        0xF4, 0xF4, 0xD0, 0x00, 0xF8, 0xF8, 0xE8, 0x00
    };
    private static final int[] SPEED_Z = {
        0x04, 0x04, 0x04, 0x04, 0x10, 0x10, 0x10, 0x10,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    // PlayerProjectileSpeedX/YPerDirection at bank $00:$13A5/$13A9.
    private static final int[] PLACED_BOMB_SPEED_X = {0x20, 0xE0, 0x00, 0x00};
    private static final int[] PLACED_BOMB_SPEED_Y = {0x00, 0x00, 0xE0, 0x20};

    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] active = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record Update(RoomEntity entity, boolean blockedX, boolean blockedY) {
    }

    /** Mirrors func_014_53A3's table-index calculation. */
    void start(int slot, int romDirection, int entityType, boolean sideScrolling) {
        validateSlot(slot);
        validateDirection(romDirection);
        validateByte(entityType, "Entity type");

        int tableIndex = (sideScrolling ? 8 : 0)
            + (entityType == ENTITY_BOMB ? 4 : 0) + romDirection;
        speedX[slot] = SPEED_X[tableIndex];
        speedY[slot] = SPEED_Y[tableIndex];
        speedZ[slot] = SPEED_Z[tableIndex];
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        active[slot] = true;
    }

    /** Starts BombEntityHandler's placed-bomb motion from SpawnPlayerProjectile. */
    void startPlacedBomb(int slot, int romDirection) {
        validateSlot(slot);
        validateDirection(romDirection);
        speedX[slot] = PLACED_BOMB_SPEED_X[romDirection];
        speedY[slot] = PLACED_BOMB_SPEED_Y[romDirection];
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        active[slot] = true;
    }

    /** Applies UpdateEntityPosWithSpeed_03 and the generic top-view gravity. */
    Update advance(RoomEntity entity, boolean sideScrolling,
                   RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        validateSlot(slot);
        if (!active[slot]) {
            return new Update(entity, false, false);
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        boolean blockedX = false;
        boolean blockedY = false;

        if (backgroundCollision != null) {
            if (x != entity.x()
                && backgroundCollision.blocks(entity, horizontalDirection(speedX[slot]),
                    x, entity.y())) {
                x = entity.x();
                speedX[slot] = bounceSpeed(speedX[slot]);
                blockedX = true;
            }
            if (y != entity.y()
                && backgroundCollision.blocks(entity, verticalDirection(speedY[slot]),
                    x, y)) {
                y = entity.y();
                speedY[slot] = bounceSpeed(speedY[slot]);
                blockedY = true;
            }
        }

        int z = entity.z();
        if (!sideScrolling) {
            z = addSpeedToPosition(z, speedZ[slot], speedZAccumulator, slot);
            speedZ[slot] = (speedZ[slot] - 0x02) & 0xFF;
        }

        return new Update(withPosition(entity, x, y, z), blockedX, blockedY);
    }

    int speedX(int slot) {
        validateSlot(slot);
        return speedX[slot];
    }

    int speedY(int slot) {
        validateSlot(slot);
        return speedY[slot];
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
        active[slot] = false;
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

    private static int bounceSpeed(int speed) {
        return ((-signedByte(speed)) >> 3) & 0xFF;
    }

    private static int horizontalDirection(int speed) {
        return (speed & 0x80) != 0 ? ROM_DIRECTION_LEFT : ROM_DIRECTION_RIGHT;
    }

    private static int verticalDirection(int speed) {
        return (speed & 0x80) != 0 ? ROM_DIRECTION_UP : ROM_DIRECTION_DOWN;
    }

    private static RoomEntity withPosition(RoomEntity entity, int x, int y, int z) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x, y, entity.status(), entity.spriteDefinition(), entity.spriteVariant(),
            entity.entityFlipAttribute(), entity.spriteTileOffset(), z);
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

    private static void validateDirection(int direction) {
        if (direction < ROM_DIRECTION_RIGHT || direction > ROM_DIRECTION_DOWN) {
            throw new IllegalArgumentException("ROM direction must be between 0 and 3");
        }
    }

    private static void validateByte(int value, String label) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException(label + " must be an unsigned byte");
        }
    }
}
