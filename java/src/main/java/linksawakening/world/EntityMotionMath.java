package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import static linksawakening.world.EntityMotionMath.*;

/** Shared ROM byte arithmetic and collision geometry. */
final class EntityMotionMath {
    private EntityMotionMath() {}

    static int sourceGenieSteerStep(int currentSpeed, int desiredSpeed) {
        int difference = ((desiredSpeed & 0xFF) - (currentSpeed & 0xFF)) & 0xFF;
        return (currentSpeed + ((difference & 0x80) != 0 ? -1 : 1)) & 0xFF;
    }

    static int bombArrowXOffset(int romDirection) {
        return switch (romDirection) {
            case 0 -> 8;
            case 1 -> -8;
            case 2, 3 -> 0;
            default -> throw new IllegalArgumentException("ROM direction must be between 0 and 3");
        };
    }

    static int bombArrowYOffset(int romDirection) {
        return switch (romDirection) {
            case 0, 1 -> 0;
            case 2 -> -3;
            case 3 -> 4;
            default -> throw new IllegalArgumentException("ROM direction must be between 0 and 3");
        };
    }

    static int directionToLink(int xDistance, int yDistance) {
        return Math.abs(xDistance) >= Math.abs(yDistance)
            ? (xDistance < 0 ? 1 : 0) : (yDistance < 0 ? 2 : 3);
    }

    static int directionFromLinkToEntity(RoomEntity entity, int linkX, int linkY) {
        int dx = (byte) (entity.x() - linkX);
        int dy = (byte) (entity.y() - linkY);
        if (Math.abs(dy) >= Math.abs(dx)) {
            return dy < 0 ? 2 : 3;
        }
        return dx < 0 ? 1 : 0;
    }

    static int approachSignedByteZero(int value) {
        int signed = signedByte(value);
        if (signed > 0) {
            signed--;
        } else if (signed < 0) {
            signed++;
        }
        return signed & 0xFF;
    }

    static boolean isWithinBombExplosionWindow(int source, int target) {
        return (((((source - target) & 0xFF) + 0x18) & 0xFF) < 0x30);
    }

    static Vector vectorAwayFromLink(int entityX, int entityY,
                                             int linkEntityX, int linkEntityY,
                                             int vectorLength) {
        int distanceX = signedByte((linkEntityX - entityX) & 0xFF);
        int distanceY = signedByte((linkEntityY - entityY) & 0xFF);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        int smaller = Math.min(absoluteX, absoluteY);
        int larger = Math.max(absoluteX, absoluteY);
        int minor = larger == 0 ? vectorLength : divideVectorComponent(
            smaller, larger, vectorLength);
        int towardX = absoluteX >= absoluteY ? vectorLength : minor;
        int towardY = absoluteY > absoluteX ? vectorLength : minor;
        if (distanceX < 0) {
            towardX = -towardX;
        }
        if (distanceY < 0) {
            towardY = -towardY;
        }
        return new Vector((-towardX) & 0xFF, (-towardY) & 0xFF);
    }

    static int divideVectorComponent(int smaller, int larger, int length) {
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

    static boolean withinUnsignedWindow(int value, int center, int radius) {
        return (((value - center + radius) & 0xFF) < radius * 2);
    }

    static FallingVector vectorTowardsTarget(int entityX, int entityY, int entityZ,
                                                      int targetX, int targetY, int length) {
        int distanceX = signedByte(targetX - entityX);
        int distanceY = signedByte(targetY - entityY + entityZ);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean swapped = absoluteX < absoluteY;
        int smaller = Math.min(absoluteX, absoluteY);
        int larger = Math.max(absoluteX, absoluteY);
        int result = romDivide(length, smaller, larger);
        int x = swapped ? result : length;
        int y = swapped ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new FallingVector(x & 0xFF, y & 0xFF);
    }

    static int romDivide(int length, int smallerDistance, int largerDistance) {
        if (length == 0) {
            return 0;
        }
        if (largerDistance == 0) {
            return length;
        }
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum & 0xFF;
        }
        return result;
    }

    static int addFallingSpeedToPosition(int position, int speed, int[] accumulator,
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

    static int byteValue(int value) {
        return value & 0xFF;
    }

    static int unsignedByteAbs(int value) {
        int difference = value & 0xFF;
        return difference < 0x80 ? difference : 0x100 - difference;
    }

    static boolean sideViewPlatformOverlapsLink(RoomEntity entity,
                                                         int linkEntityX, int linkEntityY) {
        int xDistance = unsignedByteAbs(entity.x() + 0x10 - linkEntityX - 0x08);
        if (xDistance >= 0x10 + 0x04) {
            return false;
        }
        int yDistance = unsignedByteAbs(entity.y() - entity.z() + 0x0C
            - linkEntityY - 0x08);
        return yDistance < 0x12 + 0x04;
    }

    static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    static int romDirectionForJavaDirection(int javaDirection) {
        if (javaDirection < 0 || javaDirection > 3) {
            throw new IllegalArgumentException("Link direction out of range: " + javaDirection);
        }
        return switch (javaDirection) {
            case 0 -> LiftedEntityMotion.ROM_DIRECTION_DOWN;
            case 1 -> LiftedEntityMotion.ROM_DIRECTION_UP;
            case 2 -> LiftedEntityMotion.ROM_DIRECTION_LEFT;
            case 3 -> LiftedEntityMotion.ROM_DIRECTION_RIGHT;
            default -> throw new AssertionError(javaDirection);
        };
    }

    record Vector(int x, int y) {
    }

    record FallingVector(int x, int y) {
    }
}
