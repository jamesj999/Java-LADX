package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.ScreenShakeRequest;

/** Bank-$06 ArmosKnightEntityHandler's active state machine. */
public final class ArmosKnightMotion {
    public record RubbleRequest(int x, int y) {}

    public record ScreenShakeRequest(int countdown, int phase) {}

    public record Update(
            RoomEntity entity,
            int transitionCountdown,
            int physicsFlags,
            int hitboxFlags,
            int options1,
            int jingleId,
            boolean linkMotionBlocked,
            ScreenShakeRequest screenShakeRequest,
            RubbleRequest rubbleRequest) {}

    static final int STATE_WAKE = 0;
    static final int STATE_CHARGE = 1;
    static final int STATE_CHARGE_MOVE = 2;
    static final int STATE_JUMP_DELAY = 3;
    static final int STATE_JUMP = 4;
    static final int STATE_LAND_DELAY = 5;
    static final int STATE_BOUNCE = 6;
    static final int STATE_BOUNCE_DELAY = 7;

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public void initialize(int slot) {
        state[slot] = STATE_WAKE;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        privateCountdown1[slot] = 0;
        privateState1[slot] = 0;
        inertia[slot] = 0;
        initialized[slot] = true;
    }

    public Update advance(ArmosKnightFrame inputs) {
        return armosKnightStep.run(this, inputs);
    }

    public void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    public int state(int slot) {
        return state[slot];
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

    public int privateCountdown1(int slot) {
        return privateCountdown1[slot];
    }

    public int inertia(int slot) {
        return inertia[slot];
    }

    static boolean withinDistance(int value, int target, int distance) {
        int delta = (value - target) & 0xFF;
        int absolute = delta < 0x80 ? delta : 0x100 - delta;
        return absolute < distance;
    }

    static Vector vectorTowardsLink(int entityX, int entityY, int linkX, int linkY, int length) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean yIsLargerAxis = absoluteY > absoluteX;
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (largerDistance == 0 || sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum & 0xFF;
        }

        int x = yIsLargerAxis ? result : length;
        int y = yIsLargerAxis ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
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

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    record Vector(int x, int y) {}

    final ArmosKnightStep armosKnightStep = new ArmosKnightStep();
}
