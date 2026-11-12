package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;

/** Bank-$04 motion and state machines shared by the two Bombite handlers. */
public final class BombiteMotion {
    static final int ENTITY_BOUNCING_BOMBITE = 0x55;
    static final int ENTITY_TIMER_BOMBITE = 0x56;

    static final int[] TIMER_SPEED_X = {0x08, 0xF8, 0x00, 0x00};
    static final int[] TIMER_SPEED_Y = {0x00, 0x00, 0xF8, 0x00};
    static final int[] BOUNCING_SPEED_X = {0x08, 0xF8, 0x00, 0x00};
    static final int[] BOUNCING_SPEED_Y = {0x00, 0x00, 0xF8, 0x08};
    static final int[] TIMER_COUNTDOWN_VARIANTS = {5, 5, 4, 3, 2, 2, 2};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record Update(
            RoomEntity entity,
            int transitionCountdown,
            int slowTransitionCountdown,
            int privateCountdown1,
            int spriteVariant,
            boolean explode,
            boolean bumpJingle) {}

    public void initialize(int slot) {
        state[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    public Update advance(BombiteFrame inputs) {
        return new BombiteStep(this, inputs).run();
    }

    /** Starts the BouncingBombite state-$02 sword-recoil path. */
    public void enterBouncingLitFromSword(int slot, RoomEntity entity, int linkEntityX, int linkEntityY) {
        if (!initialized[slot]) {
            initialize(slot);
        }
        Vector vector = vectorTowardsLink(entity.x(), entity.y(), entity.z(), linkEntityX, linkEntityY, 0x30);
        speedX[slot] = negate(vector.x());
        speedY[slot] = negate(vector.y());
        state[slot] = 2;
    }

    /**
     * Mirrors func_003_75A2's target-side Bombite writes. The ROM copies the
     * active Bombite's current speed bytes directly; it does not recalculate
     * the vector or reset the fixed-point movement accumulators.
     */
    public void enterBouncingLitFromEntityCollision(int slot, int sourceSpeedX, int sourceSpeedY) {
        if (!initialized[slot]) {
            initialize(slot);
        }
        speedX[slot] = sourceSpeedX & 0xFF;
        speedY[slot] = sourceSpeedY & 0xFF;
        state[slot] = 2;
    }

    public void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    public int state(int slot) {
        return state[slot] & 0xFF;
    }

    public int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    public int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    static int directionToLink(int entityX, int entityY, int linkX, int linkY) {
        int distanceX = signedByte(linkX - entityX);
        int distanceY = signedByte(linkY - entityY);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    static Vector vectorTowardsLink(int entityX, int entityY, int entityZ, int linkX, int linkY, int length) {
        int distanceX = signedByte(linkX - entityX);
        int distanceY = signedByte(linkY - entityY + entityZ);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int smallerComponent = divideComponent(smallerDistance, largerDistance, length);
        int x = absoluteX >= absoluteY ? length : smallerComponent;
        int y = absoluteY > absoluteX ? length : smallerComponent;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY <= 0) {
            y = -y;
        }
        return new Vector(x & 0xFF, y & 0xFF);
    }

    private static int divideComponent(int smallerDistance, int largerDistance, int length) {
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
            remainder = sum;
        }
        return result;
    }

    static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
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

    static int negate(int speed) {
        return (-signedByte(speed)) & 0xFF;
    }

    static int horizontalDirection(int speed) {
        return (speed & 0x80) != 0 ? 1 : 0;
    }

    static int verticalDirection(int speed) {
        return (speed & 0x80) != 0 ? 2 : 3;
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    static int unsignedByteAbs(int value) {
        int difference = value & 0xFF;
        return difference < 0x80 ? difference : 0x100 - difference;
    }

    static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return EntitySnapshots.withMotion(
                entity, x, y, entity.spriteVariant(), entity.entityFlipAttribute(), entity.z(), -1, false);
    }

    record Vector(int x, int y) {}
}
