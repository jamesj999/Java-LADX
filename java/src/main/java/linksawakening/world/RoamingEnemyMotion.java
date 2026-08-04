package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$03 state shared by Octorok and the ordinary Moblin roaming handler. */
final class RoamingEnemyMotion {
    private static final int[] SPEED_X_BY_DIRECTION = {0x08, 0xF8, 0x00, 0x00};
    private static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xF8, 0x08};
    private static final int[] VARIANT_BY_DIRECTION = {6, 4, 2, 0};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] collisionPending = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        privateState1[slot] = 0;
        inertia[slot] = 0;
        collisionPending[slot] = false;
        initialized[slot] = true;
    }

    RoomEntity advance(RoomEntity entity, int linkEntityX, int linkEntityY,
                       IntSupplier randomByteSupplier,
                       RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }

        if (state[slot] != 0) {
            if (transitionCountdown[slot] == 0) {
                transitionCountdown[slot] = 0x20 | (randomByteSupplier.getAsInt() & 0x1F);
                state[slot] = 0;
                privateState1[slot] = (privateState1[slot] + 1) & 0x03;
                direction[slot] = privateState1[slot] == 0
                    ? directionToLink(entity.x(), entity.y(), linkEntityX, linkEntityY)
                    : randomByteSupplier.getAsInt() & 0x03;
                speedX[slot] = SPEED_X_BY_DIRECTION[direction[slot]];
                speedY[slot] = SPEED_Y_BY_DIRECTION[direction[slot]];
            }
            return entity;
        }

        int x = entity.x();
        int y = entity.y();
        if (collisionPending[slot] || transitionCountdown[slot] == 0) {
            transitionCountdown[slot] = 0x10 | (randomByteSupplier.getAsInt() & 0x0F);
            state[slot] = 1;
            speedX[slot] = 0;
            speedY[slot] = 0;
            collisionPending[slot] = false;
        } else {
            int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
            if (nextX != x && backgroundCollision != null
                && backgroundCollision.blocks(entity, direction[slot], nextX, y)) {
                collisionPending[slot] = true;
            } else {
                x = nextX;
            }

            int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
            if (nextY != y && backgroundCollision != null
                && backgroundCollision.blocks(entity, direction[slot], x, nextY)) {
                collisionPending[slot] = true;
            } else {
                y = nextY;
            }
        }

        inertia[slot] = (inertia[slot] + 1) & 0xFF;
        int variant = VARIANT_BY_DIRECTION[direction[slot]] | ((inertia[slot] >>> 3) & 0x01);
        return withPositionAndVariant(entity, x, y, variant);
    }

    void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    int state(int slot) {
        return state[slot];
    }

    int transitionCountdown(int slot) {
        return transitionCountdown[slot];
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

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y,
                                                       int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), variant, entity.entityFlipAttribute(),
            entity.spriteTileOffset());
    }
}
