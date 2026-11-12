package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$06 StalfosAggressiveEntityHandler's pursuit and jump states. */
public final class StalfosAggressiveMotion {
    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Mirrors EntityInitWithRandomDirection's consumed random byte. */
    public void initialize(int slot, IntSupplier randomByteSupplier) {
        initialize(slot);
        randomByteSupplier.getAsInt();
    }

    public void initialize(int slot) {
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

    public RoomEntity advance(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        return new StalfosAggressiveFrameStep(
                        this, entity, frameCounter, linkEntityX, linkEntityY, randomByteSupplier, backgroundCollision)
                .run();
    }

    public void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    public int state(int slot) {
        return state[slot];
    }

    public int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    public int speedX(int slot) {
        return speedX[slot];
    }

    public int speedY(int slot) {
        return speedY[slot];
    }

    public int speedZ(int slot) {
        return speedZ[slot];
    }

    static boolean distanceInJumpWindow(int distance) {
        return distance >= -0x1C && distance < 0x1C;
    }

    static Vector vectorTowardsLink(int entityX, int entityY, int entityZ, int linkX, int linkY, int length) {
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

    static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
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

    static int signedByte(int value) {
        int result = value & 0xFF;
        return result < 0x80 ? result : result - 0x100;
    }

    record Vector(int x, int y) {}
}
