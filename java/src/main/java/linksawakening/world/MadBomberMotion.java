package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$06 MadBomberEntityHandler state machine and bomb producer. */
final class MadBomberMotion {
    private static final int[] POSITION_X = {
        0x28, 0x38, 0x58, 0x58, 0x78, 0x88, 0x28, 0x88
    };
    private static final int[] POSITION_Y = {
        0x40, 0x70, 0x20, 0x50, 0x70, 0x40, 0x40, 0x40
    };

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record BombSpawn(int x, int y, int z, int speedX, int speedY, int speedZ,
                     int transitionCountdown) {
    }

    record Update(RoomEntity entity, int transitionCountdown, int spriteVariant,
                  BombSpawn bombSpawn, boolean playJingle) {
    }

    void initialize(int slot) {
        state[slot] = 0;
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int transitionCountdown,
                   int linkEntityX, int linkEntityY, int flashCountdown,
                   IntSupplier randomByteSupplier) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        int countdown = transitionCountdown & 0xFF;
        RoomEntity positioned = entity;
        int spriteVariant = entity.spriteVariant();
        BombSpawn bombSpawn = null;

        switch (state[slot]) {
            case 0 -> {
                // MadBomberState0Handler starts the first wait and advances to
                // the hole-selection state without changing its hidden sprite.
                countdown = 0x40;
                state[slot] = 1;
            }
            case 1 -> {
                if (countdown == 0) {
                    int positionIndex = randomByteSupplier.getAsInt() & 0x07;
                    int x = POSITION_X[positionIndex];
                    int y = POSITION_Y[positionIndex];
                    positioned = withPosition(entity, x, y);
                    if (isFarEnoughFromLink(x, y, linkEntityX, linkEntityY)) {
                        countdown = 0x18;
                        state[slot] = 2;
                    }
                }
            }
            case 2 -> {
                if (countdown == 0) {
                    countdown = 0x30;
                    state[slot] = 3;
                } else {
                    spriteVariant = countdown >= 0x0C ? 1 : 2;
                }
            }
            case 3 -> {
                if (countdown == 0) {
                    countdown = 0x10;
                    state[slot] = 4;
                    if (flashCountdown == 0) {
                        Vector vector = vectorTowardsLink(
                            positioned.x(), positioned.y(), 0x04,
                            linkEntityX, linkEntityY, 0x10);
                        bombSpawn = new BombSpawn(
                            positioned.x(), positioned.y(), 0x04,
                            vector.x(), vector.y(), 0x18, 0x40);
                    }
                } else {
                    spriteVariant = (countdown & 0x20) != 0 ? 3 : 4;
                }
            }
            case 4 -> {
                if (countdown == 0) {
                    state[slot] = 0;
                    spriteVariant = -1;
                } else {
                    spriteVariant = countdown < 0x08 ? 1 : 2;
                }
            }
            default -> state[slot] = 0;
        }

        return new Update(positioned, countdown, spriteVariant, bombSpawn,
            bombSpawn != null);
    }

    void clear(int slot) {
        state[slot] = 0;
        initialized[slot] = false;
    }

    int state(int slot) {
        return state[slot] & 0xFF;
    }

    private static boolean isFarEnoughFromLink(int entityX, int entityY,
                                                int linkEntityX, int linkEntityY) {
        return unsignedByteAbs(entityX - linkEntityX) >= 0x20
            || unsignedByteAbs(entityY - linkEntityY) >= 0x20;
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
        // GetVectorTowardsLink's zero-distance branch selects UP.
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

    private static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x, y, entity.status(), entity.spriteDefinition(), entity.spriteVariant(),
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
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
