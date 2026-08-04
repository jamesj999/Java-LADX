package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$06 StalfosAggressiveEntityHandler's pursuit and jump states. */
final class StalfosAggressiveMotion {
    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Mirrors EntityInitWithRandomDirection's consumed random byte. */
    void initialize(int slot, IntSupplier randomByteSupplier) {
        initialize(slot);
        randomByteSupplier.getAsInt();
    }

    void initialize(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    RoomEntity advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                       IntSupplier randomByteSupplier,
                       RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }

        int frame = frameCounter & 0xFF;
        int x = entity.x();
        int y = entity.y();
        int z = entity.z();
        int variant = entity.spriteVariant();

        switch (state[slot]) {
            case 0 -> {
                if (transitionCountdown[slot] == 0) {
                    state[slot] = 1;
                }
            }
            case 1 -> {
                if (((frame ^ slot) & 0x03) == 0) {
                    Vector vector = vectorTowardsLink(x, y, z, linkEntityX, linkEntityY, 0x08);
                    speedX[slot] = vector.x();
                    speedY[slot] = vector.y();
                }

                int distanceX = signedByte(linkEntityX - x);
                int distanceY = signedByte(linkEntityY - y + z);
                if (distanceInJumpWindow(distanceX) && distanceInJumpWindow(distanceY)) {
                    speedZ[slot] = 0x28;
                    Vector vector = vectorTowardsLink(x, y, z, linkEntityX, linkEntityY, 0x10);
                    speedX[slot] = vector.x();
                    speedY[slot] = vector.y();
                    state[slot] = 2;
                }

                x = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
                y = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
                variant = (frame >>> 2) & 0x01;
            }
            case 2 -> {
                x = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
                y = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
                z = addSpeedToPosition(z, speedZ[slot], speedZAccumulator, slot);
                speedZ[slot] = (speedZ[slot] - 2) & 0xFF;
                if (speedZ[slot] < 0x02) {
                    speedZ[slot] = 0xC0;
                    transitionCountdown[slot] = 0x10;
                    speedX[slot] = 0;
                    speedY[slot] = 0;
                    state[slot] = 3;
                }
                variant = 2;
            }
            case 3 -> {
                if (transitionCountdown[slot] == 0) {
                    z = addSpeedToPosition(z, speedZ[slot], speedZAccumulator, slot);
                    if (z == 0 || (z & 0x80) != 0) {
                        z = 0;
                        transitionCountdown[slot] = 0x20;
                        state[slot] = 0;
                        speedZ[slot] = 0;
                    }
                }
            }
            default -> throw new IllegalStateException(
                "Invalid aggressive Stalfos state: " + state[slot]);
        }

        // ApplyEntityInteractionWithBackground_06 supplies collision flags and
        // may spawn landing dust. The current room query has no equivalent
        // directional/object-under flags, so this handler leaves that side
        // effect pending instead of treating a generic collision as identical.
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

    private static boolean distanceInJumpWindow(int distance) {
        return distance >= -0x1C && distance < 0x1C;
    }

    private static Vector vectorTowardsLink(int entityX, int entityY, int entityZ,
                                            int linkX, int linkY, int length) {
        int distanceY = signedByte(linkY - entityY + entityZ);
        int distanceX = signedByte(linkX - entityX);
        boolean yNegative = distanceY < 0;
        boolean xNegative = distanceX < 0;
        int absoluteY = Math.abs(distanceY);
        int absoluteX = Math.abs(distanceX);

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
        int result = value & 0xFF;
        return result < 0x80 ? result : result - 0x100;
    }

    private record Vector(int x, int y) {
    }
}
