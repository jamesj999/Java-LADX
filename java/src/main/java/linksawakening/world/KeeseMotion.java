package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$06 KeeseEntityHandler state and movement. */
final class KeeseMotion {
    private static final int[] Y_SPEEDS = {
        0x00, 0x05, 0x0A, 0x0D,
        0x0E, 0x0D, 0x0A, 0x05,
        0x00, 0xFB, 0xF6, 0xF3,
        0xF2, 0xF3, 0xF6, 0xFB
    };
    private static final int[] X_SPEEDS = {
        0x0E, 0x0D, 0x0A, 0x05, 0x00, 0xFB, 0xF6, 0xF3,
        0xF2, 0xF3, 0xF6, 0xFB, 0x00, 0x05, 0x0A, 0x0D
    };
    private static final int[] INITIAL_ANGLE_BY_DIRECTION = {0x0C, 0x04, 0x08, 0x00};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] angle = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] turnDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] angleUpdateCounter = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] initialDirection = new int[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot, IntSupplier randomByteSupplier) {
        initialDirection[slot] = randomByteSupplier.getAsInt() & 0x03;
    }

    RoomEntity advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                       IntSupplier randomByteSupplier) {
        int slot = entity.slot();
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }

        if (state[slot] == 0) {
            if (transitionCountdown[slot] == 0
                && isWithinWakeWindow(entity.x(), entity.y(), linkEntityX, linkEntityY)) {
                int direction = directionToLink(entity.x(), entity.y(), linkEntityX, linkEntityY);
                angle[slot] = INITIAL_ANGLE_BY_DIRECTION[direction];
                transitionCountdown[slot] = 0x50 + (randomByteSupplier.getAsInt() & 0x3F);
                turnDirection[slot] = 1;
                state[slot] = 1;
            }
            return withVariant(entity, state[slot] == 0 ? 0 : (frameCounter >>> 3) & 0x01);
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);

        if (transitionCountdown[slot] == 0) {
            transitionCountdown[slot] = 0x20;
            state[slot] = 0;
            angleUpdateCounter[slot] = 0;
            return withPositionAndVariant(entity, x, y, 0);
        }

        angleUpdateCounter[slot]++;
        if (angleUpdateCounter[slot] >= 0x0A) {
            angleUpdateCounter[slot] = 0;
            angle[slot] = (angle[slot] + turnDirection[slot]) & 0x0F;
            speedY[slot] = Y_SPEEDS[angle[slot]];
            speedX[slot] = X_SPEEDS[angle[slot]];
            if ((randomByteSupplier.getAsInt() & 0x1F) == 0) {
                turnDirection[slot] = (randomByteSupplier.getAsInt() & 0x02) == 0 ? -1 : 1;
            }
        }

        return withPositionAndVariant(entity, x, y, (frameCounter >>> 3) & 0x01);
    }

    void clear(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        angle[slot] = 0;
        turnDirection[slot] = 0;
        angleUpdateCounter[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialDirection[slot] = 0;
    }

    int state(int slot) {
        return state[slot];
    }

    int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    int angle(int slot) {
        return angle[slot];
    }

    int speedX(int slot) {
        return speedX[slot];
    }

    int speedY(int slot) {
        return speedY[slot];
    }

    private static boolean isWithinWakeWindow(int entityX, int entityY,
                                               int linkX, int linkY) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
        return distanceX >= -0x20 && distanceX < 0x20
            && distanceY >= -0x20 && distanceY < 0x20;
    }

    private static int directionToLink(int entityX, int entityY, int linkX, int linkY) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
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

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    private static RoomEntity withVariant(RoomEntity entity, int variant) {
        return withPositionAndVariant(entity, entity.x(), entity.y(), variant);
    }

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y,
                                                     int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), variant, entity.entityFlipAttribute(),
            entity.spriteTileOffset(), entity.z());
    }
}
