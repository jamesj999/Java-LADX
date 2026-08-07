package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$06 WizrobeProjectileEntityHandler movement and palette pulse. */
final class WizrobeProjectileMotion {
    private static final int ENTITY_WIZROBE_PROJECTILE = 0x22;
    private static final int[] SPEED_X_BY_DIRECTION = {0x20, 0xE0, 0x00, 0x00};
    private static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xE0, 0x20};

    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot, IntSupplier randomByteSupplier) {
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException(
                "Wizrobe projectile random-byte supplier cannot be null");
        }
        reset(slot);
        direction[slot] = randomByteSupplier.getAsInt() & 0x03;
        initialized[slot] = true;
    }

    void initializeSpawn(int slot, int projectileDirection) {
        checkDirection(projectileDirection);
        reset(slot);
        direction[slot] = projectileDirection;
        speedX[slot] = SPEED_X_BY_DIRECTION[projectileDirection];
        speedY[slot] = SPEED_Y_BY_DIRECTION[projectileDirection];
        initialized[slot] = true;
    }

    RoomEntity advance(RoomEntity entity) {
        if ((entity.type() & 0xFF) != ENTITY_WIZROBE_PROJECTILE) {
            throw new IllegalArgumentException("Unsupported Wizrobe projectile type: 0x"
                + Integer.toHexString(entity.type()));
        }
        int slot = entity.slot();
        if (!initialized[slot]) {
            initializeSpawn(slot, 0);
        }
        int x = addSpeed(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeed(entity.y(), speedY[slot], speedYAccumulator, slot);
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), entity.spriteVariant(),
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
    }

    int flipAttribute(int frameCounter) {
        return ((frameCounter & 0xFF) & 0x04) << 2;
    }

    void setForTest(int slot, int newSpeedX, int newSpeedY, int newDirection) {
        if ((newSpeedX & ~0xFF) != 0 || (newSpeedY & ~0xFF) != 0) {
            throw new IllegalArgumentException("Wizrobe projectile speeds must be bytes");
        }
        checkDirection(newDirection);
        reset(slot);
        speedX[slot] = newSpeedX;
        speedY[slot] = newSpeedY;
        direction[slot] = newDirection;
        initialized[slot] = true;
    }

    void clear(int slot) {
        reset(slot);
        initialized[slot] = false;
    }

    int direction(int slot) {
        return direction[slot] & 0x03;
    }

    int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    private void reset(int slot) {
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
    }

    private static int addSpeed(int position, int speed, int[] accumulator, int slot) {
        if ((speed & 0xFF) == 0) {
            return position & 0xFF;
        }
        int fractionalSum = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = fractionalSum & 0xFF;
        int signedSpeed = signedByte(speed);
        int delta = signedSpeed >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private static void checkDirection(int direction) {
        if (direction < 0 || direction > 3) {
            throw new IllegalArgumentException("Wizrobe projectile direction out of range: "
                + direction);
        }
    }
}
