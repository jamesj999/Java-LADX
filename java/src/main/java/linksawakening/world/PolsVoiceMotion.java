package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$06 PolsVoiceEntityHandler's alternating jump and landing states. */
final class PolsVoiceMotion {
    static final int ENTITY_TYPE = 0x18;

    private static final int[] RANDOM_SPEED_X = {0x08, 0x08, 0xF8, 0xF8, 0x04, 0xFC};
    private static final int[] RANDOM_SPEED_Y = {0xFC, 0x04, 0xFC, 0x04, 0x08, 0xF8};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] selectedVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot) {
        state[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        selectedVariant[slot] = 0;
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int linkEntityX, int linkEntityY,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundCollision backgroundCollision,
                   int transitionCountdown) {
        if ((entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Pols Voice entity type: 0x"
                + Integer.toHexString(entity.type()));
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Pols Voice random-byte supplier cannot be null");
        }

        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        int countdown = transitionCountdown & 0xFF;
        int x = entity.x() & 0xFF;
        int y = entity.y() & 0xFF;
        Position moved = move(entity, x, y, backgroundCollision);
        x = moved.x();
        y = moved.y();

        int z = entity.z() & 0xFF;
        int renderedVariant = selectedVariant[slot];
        if ((state[slot] & 0x01) == 0) {
            // func_006_73E0 selects the standing frame before checking the
            // landing countdown. A fresh jump then selects frame zero after
            // writing its new speed tables.
            selectedVariant[slot] = 1;
            if (countdown == 0) {
                state[slot]++;
                speedZ[slot] = 0x10 + (randomByteSupplier.getAsInt() & 0x07);
                int direction = randomByteSupplier.getAsInt() & 0x07;
                if (direction >= 0x06) {
                    Vector vector = vectorTowardsLink(x, y, linkEntityX, linkEntityY, 0x0A);
                    speedX[slot] = vector.x();
                    speedY[slot] = vector.y();
                } else {
                    speedX[slot] = RANDOM_SPEED_X[direction];
                    speedY[slot] = RANDOM_SPEED_Y[direction];
                }
                selectedVariant[slot] = 0;
            }
        } else {
            z = addSpeedToPosition(z, speedZ[slot], speedZAccumulator, slot);
            speedZ[slot] = (speedZ[slot] - 1) & 0xFF;
            if ((z & 0x80) != 0) {
                z = 0;
                state[slot]++;
                countdown = 0x18 + (randomByteSupplier.getAsInt() & 0x0F);
                clearHorizontalSpeed(slot);
            }
        }

        RoomEntity updated = new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x, y, entity.status(), entity.spriteDefinition(), renderedVariant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), z);
        return new Update(updated, countdown);
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

    int speedZ(int slot) {
        return speedZ[slot];
    }

    private void clearHorizontalSpeed(int slot) {
        speedX[slot] = 0;
        speedY[slot] = 0;
    }

    private Position move(RoomEntity entity, int x, int y,
                          RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        boolean collided = false;
        if (backgroundCollision != null && nextX != x
            && backgroundCollision.blocks(entity, directionForX(speedX[slot]), nextX, y)) {
            nextX = x;
            collided = true;
        }
        if (backgroundCollision != null && nextY != y
            && backgroundCollision.blocks(entity, directionForY(speedY[slot]), nextX, nextY)) {
            nextY = y;
            collided = true;
        }
        return new Position(nextX, nextY, collided);
    }

    private static Vector vectorTowardsLink(int entityX, int entityY,
                                             int linkX, int linkY, int length) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        int larger = Math.max(absoluteX, absoluteY);
        int smaller = Math.min(absoluteX, absoluteY);
        int result = divideComponent(smaller, larger, length);
        int x = absoluteX >= absoluteY ? length : result;
        int y = absoluteY > absoluteX ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new Vector(x & 0xFF, y & 0xFF);
    }

    private static int divideComponent(int smaller, int larger, int length) {
        if (larger == 0) {
            return length;
        }
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smaller;
            if (sum >= larger) {
                sum -= larger;
                result++;
            }
            remainder = sum;
        }
        return result;
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

    private static int directionForX(int speed) {
        return (speed & 0x80) != 0 ? 1 : 0;
    }

    private static int directionForY(int speed) {
        return (speed & 0x80) != 0 ? 2 : 3;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    private record Position(int x, int y, boolean collided) {
    }

    private record Vector(int x, int y) {
    }

    record Update(RoomEntity entity, int transitionCountdown) {
    }
}
