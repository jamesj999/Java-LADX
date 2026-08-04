package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$06 AntiFairyEntityHandler's random-speed bounce loop. */
final class AntiFairyMotion {
    private static final int[] SPEED_X_BY_CHOICE = {0x0C, 0x0C, 0xF4, 0xF4};
    private static final int[] SPEED_Y_BY_CHOICE = {0x0C, 0xF4, 0x0C, 0xF4};

    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Mirrors EntityInitWithRandomSpeed in bank $03. */
    void initialize(int slot, IntSupplier randomByteSupplier) {
        reset(slot);
        int choice = randomByteSupplier.getAsInt() & 0x03;
        speedX[slot] = SPEED_X_BY_CHOICE[choice];
        speedY[slot] = SPEED_Y_BY_CHOICE[choice];
        initialized[slot] = true;
    }

    RoomEntity advance(RoomEntity entity, int frameCounter,
                       RoomEntityBackgroundCollision backgroundCollision,
                       IntSupplier randomByteSupplier) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot, randomByteSupplier);
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);

        // ApplyEntityInteractionWithBackground populates directional flags;
        // the handler prioritizes a horizontal collision over a vertical one.
        if (backgroundCollision != null) {
            if (x != entity.x() && backgroundCollision.blocks(entity,
                directionForHorizontalSpeed(speedX[slot]), x, y)) {
                x = entity.x();
                speedX[slot] = negateByte(speedX[slot]);
            } else if (y != entity.y() && backgroundCollision.blocks(entity,
                directionForVerticalSpeed(speedY[slot]), x, y)) {
                y = entity.y();
                speedY[slot] = negateByte(speedY[slot]);
            }
        }

        int variant = (frameCounter >>> 3) & 0x01;
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), variant, entity.entityFlipAttribute(),
            entity.spriteTileOffset(), entity.z());
    }

    void clear(int slot) {
        reset(slot);
        initialized[slot] = false;
    }

    int speedX(int slot) {
        return speedX[slot];
    }

    int speedY(int slot) {
        return speedY[slot];
    }

    private void reset(int slot) {
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
    }

    private static int negateByte(int value) {
        return (-signedByte(value)) & 0xFF;
    }

    private static int directionForHorizontalSpeed(int speed) {
        return signedByte(speed) < 0 ? 1 : 0;
    }

    private static int directionForVerticalSpeed(int speed) {
        return signedByte(speed) < 0 ? 2 : 3;
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator,
                                          int slot) {
        speed &= 0xFF;
        if (speed == 0) {
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
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }
}
