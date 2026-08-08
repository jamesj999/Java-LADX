package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$07 StarEntityHandler's random diagonal movement and bounce loop. */
final class StarMotion {
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

    RoomEntity advance(RoomEntity entity, int frameCounter, IntSupplier randomByteSupplier,
                       RoomEntityBackgroundInteraction backgroundInteraction,
                       int ignoreHitsCountdown) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot, randomByteSupplier);
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        boolean horizontalCollision = false;
        boolean verticalCollision = false;
        if (backgroundInteraction != null) {
            if (x != entity.x()) {
                EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                    entity, directionForHorizontalSpeed(speedX[slot]), x, entity.y(),
                    ignoreHitsCountdown, frameCounter);
                if (result.blocked()) {
                    x = entity.x();
                    horizontalCollision = true;
                }
            }
            if (y != entity.y()) {
                EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                    entity, directionForVerticalSpeed(speedY[slot]), x, y,
                    ignoreHitsCountdown, frameCounter);
                if (result.blocked()) {
                    y = entity.y();
                    verticalCollision = true;
                }
            }
        }

        // StarEntityHandler checks $03 before $0C, so a corner collision
        // reverses only X even when both axes were blocked.
        if (horizontalCollision) {
            speedX[slot] = negateByte(speedX[slot]);
        } else if (verticalCollision) {
            speedY[slot] = negateByte(speedY[slot]);
        }

        int variant = (frameCounter >>> 3) & 0x03;
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
        return signedByte(speed) < 0 ? EntityBackgroundCollisionResult.LEFT
            : EntityBackgroundCollisionResult.RIGHT;
    }

    private static int directionForVerticalSpeed(int speed) {
        return signedByte(speed) < 0 ? EntityBackgroundCollisionResult.UP
            : EntityBackgroundCollisionResult.DOWN;
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
