package linksawakening.world;

import java.util.function.IntSupplier;

/**
 * The state used by the bank-$06 ButterflyEntityHandler.
 *
 * <p>This is intentionally a small handler port, rather than a general
 * Game Boy CPU model. It mirrors the handler's byte-sized speed tables,
 * fractional accumulators, private state, and vector approximation.</p>
 */
final class ButterflyMotion {
    private static final int[] POSSIBLE_SPEEDS = {4, -4, 3, -3, 2, -2, 5, -6};

    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateStateX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateStateY = new int[EntityRoomLoader.MAX_ENTITIES];

    RoomEntity advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                       IntSupplier randomByteSupplier) {
        int slot = entity.slot();
        int state = (frameCounter + slot * 8) & 0xFF;

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);

        // The ROM refreshes X and Y independently, using the prior private
        // vector component as a bias. These writes happen after movement.
        if ((state & 0x1F) == 0) {
            speedX[slot] = (privateStateX[slot]
                + POSSIBLE_SPEEDS[randomByteSupplier.getAsInt() & 0x07]) & 0xFF;
        }
        if (((state + 0x10) & 0x1F) == 0) {
            speedY[slot] = (privateStateY[slot]
                + POSSIBLE_SPEEDS[randomByteSupplier.getAsInt() & 0x07]) & 0xFF;
        }

        // GetVectorTowardsLink uses an infinity-norm vector of length two and
        // stores it in private state for the next speed refresh.
        if ((state & 0x3F) == 0) {
            Vector vector = vectorTowardsLink(x, y, linkEntityX, linkEntityY);
            privateStateX[slot] = vector.x() & 0xFF;
            privateStateY[slot] = vector.y() & 0xFF;
        }

        return new RoomEntity(
            entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), entity.spriteVariant(),
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
    }

    void clear(int slot) {
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        privateStateX[slot] = 0;
        privateStateY[slot] = 0;
    }

    int privateStateX(int slot) {
        return privateStateX[slot];
    }

    int privateStateY(int slot) {
        return privateStateY[slot];
    }

    /** Mirrors AddEntitySpeedToPos_06, including its negative-speed phase. */
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

    /** Port of bank3.asm:GetVectorTowardsLink with vector length two. */
    private static Vector vectorTowardsLink(int entityX, int entityY, int linkX, int linkY) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);

        // GetVectorTowardsLink divides the smaller distance by the larger
        // distance for exactly two iterations. The larger component remains
        // the requested vector length; only the smaller component is derived.
        boolean yIsTheLargerAxis = absoluteY > absoluteX;
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < 2; count++) {
            int sum = remainder + smallerDistance;
            if (largerDistance == 0 || sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum;
        }

        int x = yIsTheLargerAxis ? result : 2;
        int y = yIsTheLargerAxis ? 2 : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new Vector(x, y);
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    private record Vector(int x, int y) {
    }
}
