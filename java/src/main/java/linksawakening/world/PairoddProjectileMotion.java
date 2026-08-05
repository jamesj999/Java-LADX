package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$04 PairoddProjectileEntityHandler's vector travel and animation. */
final class PairoddProjectileMotion {
    private static final int VECTOR_LENGTH = 0x18;

    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot) {
        reset(slot);
        initialized[slot] = true;
    }

    /** Mirrors the room initializer for a projectile that was present in a room stream. */
    void initialize(int slot, IntSupplier randomByteSupplier) {
        reset(slot);
        direction[slot] = randomByteSupplier.getAsInt() & 0x03;
        initialized[slot] = true;
    }

    void initializeSpawn(int slot, RoomEntity source, int linkEntityX, int linkEntityY) {
        reset(slot);
        Vector vector = vectorTowardsLink(source.x(), source.y(), source.z(),
            linkEntityX, linkEntityY, VECTOR_LENGTH);
        speedX[slot] = vector.x();
        speedY[slot] = vector.y();
        initialized[slot] = true;
    }

    RoomEntity advance(RoomEntity entity, int frameCounter) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        int variant = (frameCounter >>> 3) & 0x01;
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
    }

    void clear(int slot) {
        reset(slot);
        initialized[slot] = false;
    }

    int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    private void reset(int slot) {
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        direction[slot] = 0;
    }

    private static Vector vectorTowardsLink(int entityX, int entityY, int entityZ,
                                            int linkX, int linkY, int length) {
        int distanceY = signedByte(linkY - entityY + entityZ);
        int distanceX = signedByte(linkX - entityX);
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
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
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
        int signedSpeed = signedByte(speed);
        int delta = signedSpeed >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private record Vector(int x, int y) {
    }
}
