package linksawakening.world;

/**
 * The ordinary overworld follow movement shared by the bank-$19 Ghost and
 * Rooster handlers. It ports their frame gates, direction display selection,
 * vector approximation, fixed-point position accumulator, and the Ghost's
 * eight-frame Z bob without introducing a general entity CPU.
 */
final class FollowingNpcMotion {
    private static final int ENTITY_GHOST = 0xD4;
    private static final int ENTITY_ROOSTER = 0xD5;

    private static final int[] GHOST_Z = {0x0C, 0x0D, 0x0E, 0x0F,
        0x0F, 0x0E, 0x0D, 0x0C};

    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot, int type) {
        clear(slot);
    }

    RoomEntity advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                       RoomEntityBackgroundCollision backgroundCollision) {
        return switch (entity.type()) {
            case ENTITY_GHOST -> advanceGhost(entity, frameCounter, linkEntityX, linkEntityY,
                backgroundCollision);
            case ENTITY_ROOSTER -> advanceRooster(entity, frameCounter, linkEntityX, linkEntityY,
                backgroundCollision);
            default -> entity;
        };
    }

    void clear(int slot) {
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
    }

    private RoomEntity advanceGhost(RoomEntity entity, int frameCounter,
                                    int linkEntityX, int linkEntityY,
                                    RoomEntityBackgroundCollision backgroundCollision) {
        int variant = ghostVariant(entity.x(), entity.y(), linkEntityX, linkEntityY,
            frameCounter);
        boolean outsideFollowWindow = !withinGhostWindow(
            entity.x(), entity.y(), linkEntityX, linkEntityY);
        int x = entity.x();
        int y = entity.y();
        if (outsideFollowWindow) {
            if ((frameCounter & 0x03) == 0) {
                // The handler temporarily adds $0C only for its proximity
                // check, restores hLinkPositionY, then aims at the original
                // Link coordinate.
                Vector vector = vectorTowardsLink(x, y, linkEntityX, linkEntityY, 0x08);
                speedX[entity.slot()] = vector.x() & 0xFF;
                speedY[entity.slot()] = vector.y() & 0xFF;
            }
            int[] position = move(entity, x, y, backgroundCollision);
            x = position[0];
            y = position[1];
        }
        int z = GHOST_Z[(frameCounter >>> 3) & 0x07];
        return withPositionAndVariant(entity, x, y, z, variant);
    }

    private RoomEntity advanceRooster(RoomEntity entity, int frameCounter,
                                      int linkEntityX, int linkEntityY,
                                      RoomEntityBackgroundCollision backgroundCollision) {
        int direction = directionToLink(entity.x(), entity.y(), linkEntityX, linkEntityY);
        int variant = direction * 2 + ((frameCounter >>> 3) & 0x01);
        boolean outsideFollowWindow = !withinRoosterWindow(
            entity.x(), entity.y(), linkEntityX, linkEntityY);
        int x = entity.x();
        int y = entity.y();
        if (outsideFollowWindow) {
            if ((frameCounter & 0x07) == 0) {
                Vector vector = vectorTowardsLink(x, y, linkEntityX, linkEntityY, 0x0C);
                speedX[entity.slot()] = vector.x() & 0xFF;
                speedY[entity.slot()] = vector.y() & 0xFF;
            }
            int[] position = move(entity, x, y, backgroundCollision);
            x = position[0];
            y = position[1];
        }
        return withPositionAndVariant(entity, x, y, 0, variant);
    }

    private int[] move(RoomEntity entity, int x, int y,
                       RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        if (backgroundCollision != null && nextX != x) {
            int direction = signedByte(speedX[slot]) < 0 ? 1 : 0;
            if (backgroundCollision.blocks(entity, direction, nextX, y)) {
                nextX = x;
                speedX[slot] = 0;
            }
        }
        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        if (backgroundCollision != null && nextY != y) {
            int direction = signedByte(speedY[slot]) < 0 ? 2 : 3;
            if (backgroundCollision.blocks(entity, direction, nextX, nextY)) {
                nextY = y;
                speedY[slot] = 0;
            }
        }
        return new int[] {nextX, nextY};
    }

    private static boolean withinGhostWindow(int entityX, int entityY,
                                              int linkX, int linkY) {
        int dx = signedByte((linkX - entityX) & 0xFF);
        int dy = signedByte((linkY + 0x0C - entityY) & 0xFF);
        return inSignedWindow(dx, 0x18) && inSignedWindow(dy, 0x18);
    }

    private static boolean withinRoosterWindow(int entityX, int entityY,
                                                int linkX, int linkY) {
        int dx = signedByte((linkX - entityX) & 0xFF);
        int dy = signedByte((linkY - entityY) & 0xFF);
        return inSignedWindow(dx, 0x12) && inSignedWindow(dy, 0x12);
    }

    private static boolean inSignedWindow(int distance, int halfOpen) {
        return distance >= -halfOpen && distance < halfOpen;
    }

    private static int ghostVariant(int entityX, int entityY, int linkX, int linkY,
                                    int frameCounter) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
        int base;
        if (Math.abs(distanceY) >= Math.abs(distanceX) && distanceY < 0) {
            base = 4;
        } else {
            base = distanceX < 0 ? 2 : 0;
        }
        return base + ((frameCounter >>> 4) & 0x01);
    }

    private static int directionToLink(int entityX, int entityY, int linkX, int linkY) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    private static Vector vectorTowardsLink(int entityX, int entityY,
                                             int linkX, int linkY, int length) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        int result = divideComponent(Math.min(absoluteX, absoluteY),
            Math.max(absoluteX, absoluteY), length);
        int x = absoluteX >= absoluteY ? length : result;
        int y = absoluteY > absoluteX ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new Vector(x, y);
    }

    private static int divideComponent(int smaller, int larger, int length) {
        if (larger == 0) {
            return 0;
        }
        int result = 0;
        int remainder = 0;
        // GetVectorTowardsLink's divide loop runs exactly `length` iterations,
        // using the smaller distance as its remainder increment.
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
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y, int z,
                                                     int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), variant, entity.entityFlipAttribute(),
            entity.spriteTileOffset(), z);
    }

    private record Vector(int x, int y) {
    }
}
