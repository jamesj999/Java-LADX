package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$06 GibdoEntityHandler's random walk and background-bounce loop. */
final class GibdoMotion {
    private static final int[] SPEED_X_BY_CHOICE = {0x00, 0x08, 0xF8, 0x00};
    private static final int[] SPEED_Y_BY_CHOICE = {0xF8, 0x08};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Mirrors EntityInitHandlersTable._1F -> IncrementEntityState. */
    void initialize(int slot) {
        state[slot] = 1;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    RoomEntity advance(RoomEntity entity, IntSupplier randomByteSupplier,
                       RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);

        // ApplyEntityInteractionWithBackground writes directional collision
        // flags. The room model exposes those flags as a directional blocking
        // query, so reverse only the axis whose proposed movement is blocked.
        if (backgroundCollision != null) {
            if (x != entity.x() && backgroundCollision.blocks(entity,
                directionForHorizontalSpeed(speedX[slot]), x, entity.y())) {
                x = entity.x();
                speedX[slot] ^= 0xF0;
            }
            if (y != entity.y() && backgroundCollision.blocks(entity,
                directionForVerticalSpeed(speedY[slot]), x, y)) {
                y = entity.y();
                speedY[slot] ^= 0xF0;
            }
        }

        // State 1 is written by the ROM init handler and forces an immediate
        // direction choice. Ordinary handler passes choose again only when
        // the random low six bits are zero.
        if (state[slot] != 0 || (randomByteSupplier.getAsInt() & 0x3F) == 0) {
            chooseDirection(slot, randomByteSupplier);
        }

        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), entity.spriteVariant(),
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
    }

    void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    int state(int slot) {
        return state[slot];
    }

    int speedX(int slot) {
        return speedX[slot];
    }

    int speedY(int slot) {
        return speedY[slot];
    }

    private void chooseDirection(int slot, IntSupplier randomByteSupplier) {
        speedY[slot] = 0;
        speedX[slot] = SPEED_X_BY_CHOICE[randomByteSupplier.getAsInt() & 0x03];
        if (speedX[slot] == 0) {
            speedY[slot] = SPEED_Y_BY_CHOICE[randomByteSupplier.getAsInt() & 0x01];
        }
        state[slot] = 0;
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
