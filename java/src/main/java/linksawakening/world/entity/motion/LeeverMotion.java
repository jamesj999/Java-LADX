package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$04 LeeverEntityHandler's hide, emerge, chase, and burrow states. */
public final class LeeverMotion {
    static final int[] EMERGING_VARIANTS = {0x01, 0x00};
    static final int[] BURROWING_VARIANTS = {0x00, 0x01};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public void initialize(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    public RoomEntity advance(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        return leeverFrameStep.run(
                this, entity, frameCounter, linkEntityX, linkEntityY, randomByteSupplier, backgroundCollision);
    }

    public void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    public boolean isChasing(int slot) {
        return state[slot] == 2;
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

    static Vector vectorTowardsLink(int entityX, int entityY, int entityZ, int linkX, int linkY, int length) {
        int distanceY = signedByte((linkY - entityY + entityZ) & 0xFF);
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        boolean swapped = Math.abs(distanceX) < Math.abs(distanceY);
        int smallerDistance = Math.min(Math.abs(distanceX), Math.abs(distanceY));
        int largerDistance = Math.max(Math.abs(distanceX), Math.abs(distanceY));
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

    static int directionForX(int speed) {
        return signedByte(speed) < 0 ? 1 : 0;
    }

    static int directionForY(int speed) {
        return signedByte(speed) < 0 ? 2 : 3;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    record Vector(int x, int y) {}

    final LeeverFrameStep leeverFrameStep = new LeeverFrameStep();
}
