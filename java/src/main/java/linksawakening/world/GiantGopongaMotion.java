package linksawakening.world;

/** Bank-$06 GiantGopongaFlowerEntityHandler state and projectile launch. */
final class GiantGopongaMotion {
    static final int ENTITY_TYPE = 0x7C;
    static final int PROJECTILE_TYPE = 0x7D;
    static final int INITIAL_PHYSICS_FLAGS = 0x00;
    static final int OPTIONS1 = 0x02;

    private static final int INITIAL_COUNTDOWN = 0xC0;
    private static final int OPEN_COUNTDOWN = 0x50;
    private static final int PROJECTILE_COUNTDOWN = 0x4A;
    private static final int PROJECTILE_VECTOR_LENGTH = 0x0C;

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** The request made by SpawnNewEntity plus ApplyVectorTowardsLink. */
    record ProjectileSpawn(int x, int y, int speedX, int speedY) {
    }

    record Update(RoomEntity entity, int state, int transitionCountdown,
                  ProjectileSpawn projectileSpawn) {
    }

    Update advance(RoomEntity entity, int frameCounter, int transitionCountdown,
                   int linkEntityX, int linkEntityY) {
        checkType(entity);
        int slot = entity.slot();
        if (!initialized[slot]) {
            state[slot] = 0;
            initialized[slot] = true;
        }

        int currentState = state[slot];
        int nextCountdown = transitionCountdown & 0xFF;
        ProjectileSpawn projectileSpawn = null;
        int variant = entity.spriteVariant();
        switch (currentState) {
            case 0 -> {
                // GiantGopongaState0Handler starts its first long opening
                // timer and immediately advances to state 1.
                nextCountdown = INITIAL_COUNTDOWN;
                currentState = 1;
            }
            case 1 -> {
                if (nextCountdown == 0) {
                    nextCountdown = OPEN_COUNTDOWN;
                    currentState = 2;
                } else {
                    variant = (nextCountdown >>> 4) & 0x01;
                }
            }
            case 2 -> {
                // State 2's terminal state is not used by the normal room
                // lifetime; retaining state 2 after the zero edge keeps the
                // render handler in its final mouth-open presentation.
                if (nextCountdown == PROJECTILE_COUNTDOWN) {
                    Vector vector = vectorTowardsLink(entity.x(), entity.y(), entity.z(),
                        linkEntityX, linkEntityY, PROJECTILE_VECTOR_LENGTH);
                    projectileSpawn = new ProjectileSpawn(entity.x(), entity.y(),
                        vector.x(), vector.y());
                }
                variant = 0x02;
            }
            default -> {
                currentState = 2;
                variant = 0x02;
            }
        }

        state[slot] = currentState;
        RoomEntity updated = withVariant(entity, variant);
        return new Update(updated, currentState, nextCountdown, projectileSpawn);
    }

    /** PushLinkOutOfEntity_06 uses collisionEvenInTheAir. */
    static boolean overlapsInteractiveLink(RoomEntity entity, int linkEntityX,
                                            int linkEntityY,
                                            boolean handlerLinkCollisionEnabled) {
        return handlerLinkCollisionEnabled
            && RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY);
    }

    void clear(int slot) {
        state[slot] = 0;
        initialized[slot] = false;
    }

    private static RoomEntity withVariant(RoomEntity entity, int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            entity.x(), entity.y(), entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
    }

    private static void checkType(RoomEntity entity) {
        if ((entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported giant Goponga type: 0x"
                + Integer.toHexString(entity.type()));
        }
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

    private record Vector(int x, int y) {
    }
}
