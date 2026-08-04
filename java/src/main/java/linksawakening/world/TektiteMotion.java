package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$06 TektiteEntityHandler's ordinary two-state jump loop. */
final class TektiteMotion {
    private static final int[] SPEED_X_BY_DIRECTION = {0x10, 0xF0, 0x10, 0xF0};
    private static final int[] SPEED_Y_BY_DIRECTION = {0x10, 0x10, 0xF0, 0xF0};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Mirrors EntityInitWithRandomDirection, which runs before the entity handler. */
    void initialize(int slot, IntSupplier randomByteSupplier) {
        initialize(slot);
        direction[slot] = randomByteSupplier.getAsInt() & 0x03;
    }

    void initialize(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        inertia[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    RoomEntity advance(RoomEntity entity, int linkEntityX, int linkEntityY,
                       IntSupplier randomByteSupplier,
                       RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        // The shared entity loop decrements this before dispatching the handler.
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);

        // ApplyEntityInteractionWithBackground populates directional collision
        // flags before TektiteHorizontal/VerticalCollision. The room model does
        // not yet expose those flags, so the generic query is intentionally not
        // treated as equivalent to the ROM's collision-point/object lookup.

        int z = entity.z();
        int variant = entity.spriteVariant();
        if (state[slot] == 0) {
            if ((z & 0x80) != 0) {
                z = 0;
                // ClearEntitySpeed resets X/Y only; the Z speed remains intact.
                speedX[slot] = 0;
                speedY[slot] = 0;
                state[slot] = 1;
                transitionCountdown[slot] = 0x10
                    + (randomByteSupplier.getAsInt() & 0x3F);
                variant = 1;
            } else {
                z = addSpeedToPosition(z, speedZ[slot], speedZAccumulator, slot);
                speedZ[slot] = (speedZ[slot] - 1) & 0xFF;
            }
        } else {
            inertia[slot] = (inertia[slot] + 1) & 0xFF;
            variant = (inertia[slot] & 0x10) >>> 4;
            if ((inertia[slot] & 0x10) == 0 && transitionCountdown[slot] == 0) {
                speedZ[slot] = 0x10 | (randomByteSupplier.getAsInt() & 0x07);
                z = addSpeedToPosition(z, speedZ[slot], speedZAccumulator, slot);

                direction[slot] = randomByteSupplier.getAsInt() & 0x03;
                speedX[slot] = SPEED_X_BY_DIRECTION[direction[slot]];
                speedY[slot] = SPEED_Y_BY_DIRECTION[direction[slot]];

                if ((randomByteSupplier.getAsInt() & 0x01) != 0) {
                    Vector vector = vectorTowardsLink(x, y, z, linkEntityX, linkEntityY,
                        0x14);
                    speedX[slot] = vector.x();
                    speedY[slot] = vector.y();
                }
                state[slot] = 0;
                variant = 0;
            }
        }

        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), variant, entity.entityFlipAttribute(),
            entity.spriteTileOffset(), z);
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

    int speedX(int slot) {
        return speedX[slot];
    }

    int speedY(int slot) {
        return speedY[slot];
    }

    int speedZ(int slot) {
        return speedZ[slot];
    }

    private static Vector vectorTowardsLink(int entityX, int entityY, int entityZ,
                                            int linkX, int linkY, int length) {
        int distanceY = signedByte((linkY - entityY + entityZ) & 0xFF);
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        boolean yNegative = distanceY < 0;
        boolean xNegative = distanceX < 0;
        int absoluteY = Math.abs(distanceY);
        int absoluteX = Math.abs(distanceX);

        // This is the divide loop in GetVectorTowardsLink (bank $03). It
        // returns an infinity-norm vector, with the larger axis equal to the
        // requested length and the other axis accumulated by ROM division.
        boolean swapped = absoluteX < absoluteY;
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (sum > 0xFF || sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum & 0xFF;
        }

        int x = swapped ? result : length;
        int y = swapped ? length : result;
        if (xNegative) {
            x = -x;
        }
        if (yNegative) {
            y = -y;
        }
        return new Vector(x & 0xFF, y & 0xFF);
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

    private record Vector(int x, int y) {
    }
}
