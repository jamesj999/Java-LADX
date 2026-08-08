package linksawakening.world;

/** Bank-$06 GopongaProjectileEntityHandler movement, timers, and animation. */
final class GopongaProjectileMotion {
    static final int ENTITY_TYPE = 0x7D;
    static final int INITIAL_PHYSICS_FLAGS = 0x42;
    static final int OPTIONS1 = 0x02;
    static final int HEALTH_OVERRIDE = 0x30;

    static final int INITIAL_IGNORE_HITS_COUNTDOWN = 0x01;
    private static final int IGNORE_HITS_TRANSITION_COUNTDOWN = 0x10;

    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] flashing = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record Update(RoomEntity entity, int transitionCountdown, int ignoreHitsCountdown,
                  boolean unloadRequested) {
    }

    void initializeSpawn(int slot, RoomEntity source, int linkEntityX, int linkEntityY) {
        if ((source.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Goponga projectile type: 0x"
                + Integer.toHexString(source.type()));
        }
        Vector vector = vectorTowardsLink(source.x(), source.y(), source.z(),
            linkEntityX, linkEntityY, 0x0C);
        speedX[slot] = vector.x();
        speedY[slot] = vector.y();
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        flashing[slot] = false;
        initialized[slot] = true;
    }

    void initializeSpawn(int slot, int newSpeedX, int newSpeedY) {
        initializeSpawn(slot, newSpeedX, newSpeedY, false);
    }

    void initializeSpawn(int slot, int newSpeedX, int newSpeedY, boolean flash) {
        speedX[slot] = newSpeedX & 0xFF;
        speedY[slot] = newSpeedY & 0xFF;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        flashing[slot] = flash;
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int frameCounter, int transitionCountdown,
                   int ignoreHitsCountdown, boolean handlerLinkCollisionEnabled) {
        checkType(entity);
        int slot = entity.slot();
        if (!initialized[slot]) {
            initializeSpawn(slot, entity, entity.x(), entity.y());
        }

        int countdown = transitionCountdown & 0xFF;
        int ignoreHits = ignoreHitsCountdown & 0xFF;
        int renderFlipAttribute = renderFlipAttribute(entity, slot, frameCounter);
        if (countdown != 0) {
            int decremented = (countdown - 1) & 0xFF;
            if (decremented == 0) {
                return new Update(entity, 0, ignoreHits, true);
            }
            int variant = 0x02 + ((decremented >>> 3) & 0x01);
            // The handler only decrements A for its animation/unload test;
            // the WRAM transition byte was already decremented by the shared
            // entity ticker and is not written back here.
            return new Update(withVariant(entity, variant, renderFlipAttribute), countdown,
                ignoreHits, false);
        }

        if (ignoreHits >= 0x02) {
            return new Update(withFlip(entity, renderFlipAttribute),
                IGNORE_HITS_TRANSITION_COUNTDOWN, ignoreHits, false);
        }

        // The handler clears hActiveEntityIgnoreHitsCountdown before calling
        // DefaultEnemyDamageCollisionHandler and UpdateEntityPosWithSpeed_06.
        ignoreHits = 0;
        int variant = (frameCounter >>> 3) & 0x01;
        if (!handlerLinkCollisionEnabled) {
            return new Update(withVariant(entity, variant, renderFlipAttribute), 0,
                ignoreHits, false);
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        RoomEntity moved = withPositionAndVariant(entity, x, y, variant, renderFlipAttribute);
        int visualY = (moved.y() - moved.z()) & 0xFF;
        boolean unloaded = visualY >= 0x88 || moved.x() >= 0xA8;
        return new Update(moved, 0, ignoreHits, unloaded);
    }

    void clear(int slot) {
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        flashing[slot] = false;
        initialized[slot] = false;
    }

    int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    private static RoomEntity withVariant(RoomEntity entity, int variant, int flipAttribute) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            entity.x(), entity.y(), entity.status(), entity.spriteDefinition(), variant,
            flipAttribute, entity.spriteTileOffset(), entity.z());
    }

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y,
                                                      int variant, int flipAttribute) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), variant,
            flipAttribute, entity.spriteTileOffset(), entity.z());
    }

    private static RoomEntity withFlip(RoomEntity entity, int flipAttribute) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            entity.x(), entity.y(), entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), flipAttribute, entity.spriteTileOffset(), entity.z());
    }

    private int renderFlipAttribute(RoomEntity entity, int slot, int frameCounter) {
        return flashing[slot] ? ((frameCounter << 2) & 0x10)
            : entity.entityFlipAttribute();
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

    private static Vector vectorTowardsLink(int entityX, int entityY, int entityZ,
                                            int linkEntityX, int linkEntityY, int length) {
        int distanceY = signedByte(linkEntityY - entityY + entityZ);
        int distanceX = signedByte(linkEntityX - entityX);
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

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private static void checkType(RoomEntity entity) {
        if ((entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Goponga projectile type: 0x"
                + Integer.toHexString(entity.type()));
        }
    }

    private record Vector(int x, int y) {
    }
}
