package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$18 BomberEntityHandler state and movement. */
final class BomberMotion {
    private static final int[] Z_BY_INERTIA = {0x10, 0x11, 0x12, 0x11};
    private static final int[] SPEED_X_BY_RANDOM = {
        0x00, 0x06, 0x08, 0x06, 0x00, 0xFA, 0xF8, 0xFA
    };

    // Data_018_7865 is immediately before Data_018_7867. The source indexes
    // the contiguous bytes with the same 0..7 value.
    private static final int[] SPEED_Y_BY_RANDOM = {
        0xF8, 0xFA, 0x00, 0x06, 0x08, 0x06, 0x00, 0xFA
    };

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record BombSpawn(int x, int y, int z, int speedX, int speedY, int speedZ,
                     int transitionCountdown) {
    }

    record Update(RoomEntity entity, int transitionCountdown, BombSpawn bombSpawn,
                  boolean playJingle) {
    }

    void initialize(int slot) {
        state[slot] = 0;
        privateState1[slot] = 0;
        inertia[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int transitionCountdown,
                   int linkEntityX, int linkEntityY, int romLinkDirection,
                   boolean swordAnimationActive,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        if (backgroundCollision != null) {
            if (x != entity.x()
                && backgroundCollision.blocks(entity, horizontalDirection(speedX[slot]),
                    x, entity.y())) {
                x = entity.x();
                speedX[slot] = 0;
            }
            if (y != entity.y()
                && backgroundCollision.blocks(entity, verticalDirection(speedY[slot]), x, y)) {
                y = entity.y();
                speedY[slot] = 0;
            }
        }

        int z = Z_BY_INERTIA[(inertia[slot] >>> 3) & 0x03];
        RoomEntity moved = withPosition(entity, x, y, z);
        int nextTransitionCountdown = transitionCountdown & 0xFF;

        switch (state[slot]) {
            case 0 -> {
                if (nextTransitionCountdown == 0) {
                    int random = randomByteSupplier.getAsInt() & 0xFF;
                    nextTransitionCountdown = 0x20 + (random & 0x1F);
                    int direction = random & 0x07;
                    speedX[slot] = SPEED_X_BY_RANDOM[direction];
                    speedY[slot] = SPEED_Y_BY_RANDOM[direction];
                    privateState1[slot] = (privateState1[slot] + 1) & 0xFF;
                    if ((privateState1[slot] & 0x07) == 0) {
                        Vector vector = vectorTowardsLink(moved.x(), moved.y(), moved.z(),
                            linkEntityX, linkEntityY, 0x0A);
                        speedX[slot] = vector.x();
                        speedY[slot] = vector.y();
                    }
                    state[slot] = 1;
                }
            }
            case 1 -> {
                if (nextTransitionCountdown == 0) {
                    nextTransitionCountdown = 0x20;
                    state[slot] = 0;
                    speedX[slot] = 0;
                    speedY[slot] = 0;
                }
            }
            case 2 -> {
                if (nextTransitionCountdown == 0) {
                    state[slot] = 0;
                }
            }
            default -> state[slot] = 0;
        }

        int oldInertia = inertia[slot];
        inertia[slot] = (oldInertia + 1) & 0xFF;
        BombSpawn bombSpawn = null;
        boolean playJingle = false;
        if ((oldInertia & 0x7F) == 0) {
            Vector vector = vectorTowardsLink(moved.x(), moved.y(), moved.z(),
                linkEntityX, linkEntityY, 0x10);
            bombSpawn = new BombSpawn(moved.x(), moved.y(), moved.z(),
                vector.x(), vector.y(), 0x08, 0x40);
            playJingle = true;
        }

        if (matchesSwordAttackWindow(moved, linkEntityX, linkEntityY,
                romLinkDirection, swordAnimationActive)) {
            state[slot] = 2;
            nextTransitionCountdown = 0x12;
            Vector vector = vectorTowardsLink(moved.x(), moved.y(), moved.z(),
                linkEntityX, linkEntityY, 0x20);
            speedX[slot] = (-signedByte(vector.x())) & 0xFF;
            speedY[slot] = (-signedByte(vector.y())) & 0xFF;
        }

        return new Update(moved, nextTransitionCountdown, bombSpawn, playJingle);
    }

    void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    int inertia(int slot) {
        return inertia[slot] & 0xFF;
    }

    private static boolean matchesSwordAttackWindow(RoomEntity entity,
                                                     int linkEntityX,
                                                     int linkEntityY,
                                                     int romLinkDirection,
                                                     boolean swordAnimationActive) {
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

    private static Vector vectorTowardsLink(int entityX, int entityY, int entityZ,
                                             int linkX, int linkY, int length) {
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

    private static int addSpeedToPosition(int position, int speed, int[] accumulator,
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

    private static int horizontalDirection(int speed) {
        return (speed & 0x80) != 0 ? 1 : 0;
    }

    private static int verticalDirection(int speed) {
        return (speed & 0x80) != 0 ? 2 : 3;
    }

    private static RoomEntity withPosition(RoomEntity entity, int x, int y, int z) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x, y, entity.status(), entity.spriteDefinition(), entity.spriteVariant(),
            entity.entityFlipAttribute(), entity.spriteTileOffset(), z);
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private static int unsignedByteAbs(int value) {
        int difference = value & 0xFF;
        return difference < 0x80 ? difference : 0x100 - difference;
    }

    private record Vector(int x, int y) {
    }
}
