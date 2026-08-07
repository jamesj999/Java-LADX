package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$03 movement used after an Iron Mask's mask has been removed. */
final class UnmaskedIronMaskMotion {
    private static final int[] SPEED_X_BY_DIRECTION = {0x0C, 0xF4, 0x00, 0x00};
    private static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xF4, 0x0C};

    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initializeFromMasked(int slot, int maskedDirection, int maskedSpeedX,
                              int maskedSpeedY) {
        direction[slot] = maskedDirection & 0x03;
        speedX[slot] = maskedSpeedX & 0xFF;
        speedY[slot] = maskedSpeedY & 0xFF;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int frame, IntSupplier randomByteSupplier,
                   RoomEntityBackgroundInteraction backgroundInteraction,
                   RoomEntityBackgroundCollision backgroundCollision,
                   int ignoreHitsCountdown, int transitionCountdown) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initializeFromMasked(slot, 0, 0, 0);
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        if (x != entity.x() && isBlocked(entity, direction[slot], x, entity.y(),
            backgroundInteraction, backgroundCollision, ignoreHitsCountdown, frame)) {
            x = entity.x();
        }
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        if (y != entity.y() && isBlocked(entity, direction[slot], x, y,
            backgroundInteraction, backgroundCollision, ignoreHitsCountdown, frame)) {
            y = entity.y();
        }

        int nextTransitionCountdown = transitionCountdown;
        if (nextTransitionCountdown == 0) {
            nextTransitionCountdown = 0x20 | (randomByteSupplier.getAsInt() & 0x1F);
            direction[slot] = randomByteSupplier.getAsInt() & 0x03;
            speedX[slot] = SPEED_X_BY_DIRECTION[direction[slot]];
            speedY[slot] = SPEED_Y_BY_DIRECTION[direction[slot]];
        }

        return new Update(new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x, y, entity.status(), entity.spriteDefinition(), (frame >>> 4) & 0x01,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z()),
            nextTransitionCountdown);
    }

    void clear(int slot) {
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = false;
    }

    int direction(int slot) {
        return direction[slot];
    }

    int speedX(int slot) {
        return speedX[slot];
    }

    int speedY(int slot) {
        return speedY[slot];
    }

    private static boolean isBlocked(RoomEntity entity, int entityDirection, int nextX, int nextY,
                                     RoomEntityBackgroundInteraction backgroundInteraction,
                                     RoomEntityBackgroundCollision backgroundCollision,
                                     int ignoreHitsCountdown, int frame) {
        if (backgroundInteraction != null) {
            return backgroundInteraction.probe(entity, entityDirection, nextX, nextY,
                ignoreHitsCountdown, frame).blocked();
        }
        return backgroundCollision != null
            && backgroundCollision.blocks(entity, entityDirection, nextX, nextY);
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator,
                                          int slot) {
        speed &= 0xFF;
        if (speed == 0) {
            return position & 0xFF;
        }

        int fractionalSum = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = fractionalSum & 0xFF;
        int signedSpeed = speed < 0x80 ? speed : speed - 0x100;
        int delta = signedSpeed >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    record Update(RoomEntity entity, int transitionCountdown) {
    }
}
