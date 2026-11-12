package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$18 BomberEntityHandler state and movement. */
public final class BomberMotion {
    static final int[] Z_BY_INERTIA = {0x10, 0x11, 0x12, 0x11};
    static final int[] SPEED_X_BY_RANDOM = {0x00, 0x06, 0x08, 0x06, 0x00, 0xFA, 0xF8, 0xFA};

    // Data_018_7865 is immediately before Data_018_7867. The source indexes
    // the contiguous bytes with the same 0..7 value.
    static final int[] SPEED_Y_BY_RANDOM = {0xF8, 0xFA, 0x00, 0x06, 0x08, 0x06, 0x00, 0xFA};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record BombSpawn(int x, int y, int z, int speedX, int speedY, int speedZ, int transitionCountdown) {}

    public record Update(RoomEntity entity, int transitionCountdown, BombSpawn bombSpawn, boolean playJingle) {}

    public void initialize(int slot) {
        state[slot] = 0;
        privateState1[slot] = 0;
        inertia[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    public Update advance(
            RoomEntity entity,
            int transitionCountdown,
            int linkEntityX,
            int linkEntityY,
            int romLinkDirection,
            boolean swordAnimationActive,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        return advance(new BomberFrame(
                entity,
                transitionCountdown,
                linkEntityX,
                linkEntityY,
                romLinkDirection,
                swordAnimationActive,
                randomByteSupplier,
                backgroundCollision));
    }

    public linksawakening.world.entity.motion.BomberMotion.Update advance(BomberFrame inputs) {
        return bomberFrameStep.run(this, inputs);
    }

    public void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    int inertia(int slot) {
        return inertia[slot] & 0xFF;
    }

    static boolean matchesSwordAttackWindow(
            RoomEntity entity, int linkEntityX, int linkEntityY, int romLinkDirection, boolean swordAnimationActive) {
        if (!swordAnimationActive
                || directionToLink(entity.x(), entity.y(), linkEntityX, linkEntityY)
                        != ((romLinkDirection ^ 0x01) & 0x03)) {
            return false;
        }
        int xDistance = unsignedByteAbs(entity.x() - linkEntityX);
        int visualY = (entity.y() - entity.z()) & 0xFF;
        int linkVisualY = linkEntityY & 0xFF;
        int yDistance = unsignedByteAbs(visualY - linkVisualY);
        return xDistance < 0x20 && yDistance < 0x20;
    }

    private static int directionToLink(int entityX, int entityY, int linkX, int linkY) {
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
        // GetVectorTowardsLink uses the UP branch for a zero Y distance.
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

    static int horizontalDirection(int speed) {
        return (speed & 0x80) != 0 ? 1 : 0;
    }

    static int verticalDirection(int speed) {
        return (speed & 0x80) != 0 ? 2 : 3;
    }

    static RoomEntity withPosition(RoomEntity entity, int x, int y, int z) {
        return EntitySnapshots.withMotion(
                entity, x, y, entity.spriteVariant(), entity.entityFlipAttribute(), z, -1, false);
    }

    static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private static int unsignedByteAbs(int value) {
        int difference = value & 0xFF;
        return difference < 0x80 ? difference : 0x100 - difference;
    }

    record Vector(int x, int y) {}

    final BomberFrameStep bomberFrameStep = new BomberFrameStep();
}
