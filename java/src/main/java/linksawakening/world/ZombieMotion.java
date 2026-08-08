package linksawakening.world;

import java.util.Objects;
import java.util.function.IntSupplier;

/** Bank-$18 ZombieEntityHandler hidden-parent and child state machine. */
final class ZombieMotion {
    static final int ENTITY_TYPE = 0xBF;
    static final int INITIAL_PHYSICS_FLAGS = 0x52;
    static final int OPTIONS1 = 0x00;
    static final int PROJECTILE_NOCLIP_FLAG = 0x40;

    // Data_018_637D and Data_018_6385.
    private static final int[] SPAWN_Y = {
        0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, 0x80
    };
    private static final int[] SPAWN_X = {
        0x18, 0x28, 0x38, 0x48, 0x58, 0x68, 0x78, 0x88
    };

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record SpawnRequest(int x, int y) {
    }

    record Update(RoomEntity entity, int state, int privateState1,
                  int transitionCountdown, int physicsFlags, int speedX, int speedY,
                  SpawnRequest spawnRequest, boolean unloadRequested,
                  boolean appliesBackgroundInteraction) {
    }

    void initialize(int slot) {
        reset(slot);
        initialized[slot] = true;
    }

    /** Seeds the private-state-$01 child created by SpawnNewEntityInRange. */
    void initializeChild(int slot) {
        reset(slot);
        privateState1[slot] = 1;
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int frameCounter, int transitionCountdown,
                   int physicsFlags, int linkEntityX, int linkEntityY,
                   IntSupplier randomByteSupplier, RoomEntityObjectQuery objectQuery,
                   RoomEntityBackgroundInteraction backgroundInteraction,
                   int ignoreHitsCountdown) {
        checkType(entity);
        Objects.requireNonNull(randomByteSupplier, "Zombie random-byte supplier");
        validateByte(transitionCountdown, "Zombie transition countdown");
        validateByte(physicsFlags, "Zombie physics flags");
        validateByte(ignoreHitsCountdown, "Zombie ignore-hits countdown");

        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        int countdown = transitionCountdown;
        int nextPhysicsFlags = physicsFlags;
        int nextSpeedX = speedX[slot];
        int nextSpeedY = speedY[slot];
        RoomEntity updated = entity;
        SpawnRequest spawnRequest = null;
        boolean unloadRequested = false;
        boolean appliesBackgroundInteraction = false;

        if (privateState1[slot] == 0) {
            // The room-loaded entity is a permanently hidden spawn marker. The
            // handler still relocates it before testing the object physics.
            int variant = -1;
            if (countdown == 0) {
                int index = randomByteSupplier.getAsInt() & 0x07;
                int x = SPAWN_X[index];
                int y = SPAWN_Y[index];
                updated = withPositionAndVariant(entity, x, y, variant);
                RoomEntityObjectSample object = objectQuery == null
                    ? null : objectQuery.sample(updated);
                if (object != null && isAllowedSpawnPhysics(object.physicsFlag())) {
                    countdown = (randomByteSupplier.getAsInt() & 0x3F) + 0x40;
                    spawnRequest = new SpawnRequest(x, y);
                }
            } else {
                updated = withVariant(entity, variant);
            }
            return update(updated, countdown, nextPhysicsFlags, nextSpeedX, nextSpeedY,
                spawnRequest, false, appliesBackgroundInteraction);
        }

        int variant = entity.spriteVariant();
        switch (state[slot]) {
            case 0 -> {
                countdown = 0x30;
                state[slot] = 1;
            }
            case 1 -> {
                if (countdown == 0) {
                    countdown = (randomByteSupplier.getAsInt() & 0x3F) + 0x70;
                    int vectorLength = (randomByteSupplier.getAsInt() & 0x07) + 0x05;
                    Vector vector = vectorTowardsLink(entity.x(), entity.y(),
                        linkEntityX, linkEntityY, vectorLength);
                    speedX[slot] = vector.x();
                    speedY[slot] = vector.y();
                    nextSpeedX = speedX[slot];
                    nextSpeedY = speedY[slot];
                    state[slot] = 2;
                }
                variant = countdown >= 0x18 ? 1 : 2;
            }
            case 2 -> {
                appliesBackgroundInteraction = true;
                PositionAndCollision moved = backgroundInteraction == null
                    ? new PositionAndCollision(
                        addSpeedToPosition(entity.x(), speedX[slot],
                            speedXAccumulator, slot),
                        addSpeedToPosition(entity.y(), speedY[slot],
                            speedYAccumulator, slot), 0)
                    : moveWithBackground(entity, backgroundInteraction,
                        ignoreHitsCountdown, frameCounter);
                int x = moved.x();
                int y = moved.y();
                int collisionMask = moved.collisionMask();
                updated = withPosition(entity, x, y);
                if ((collisionMask & 0x0F) != 0 || countdown == 0) {
                    countdown = 0x30;
                    state[slot] = 3;
                    nextPhysicsFlags |= PROJECTILE_NOCLIP_FLAG;
                }
                variant = 3 + ((frameCounter >>> 4) & 0x01);
            }
            case 3 -> {
                if (countdown == 0) {
                    unloadRequested = true;
                } else {
                    variant = countdown >= 0x18 ? 2 : 1;
                }
            }
            default -> throw new IllegalStateException("Invalid Zombie state: " + state[slot]);
        }

        updated = withVariant(updated, variant);
        return update(updated, countdown, nextPhysicsFlags, nextSpeedX, nextSpeedY,
            null, unloadRequested, appliesBackgroundInteraction);
    }

    boolean isChild(int slot) {
        return initialized[slot] && privateState1[slot] != 0;
    }

    int state(int slot) {
        return state[slot];
    }

    int privateState1(int slot) {
        return privateState1[slot];
    }

    /** State 2 is the only Zombie handler branch that calls enemy damage collision. */
    boolean allowsEnemyCollision(int slot) {
        return initialized[slot] && privateState1[slot] != 0 && state[slot] == 2;
    }

    int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    void clear(int slot) {
        reset(slot);
        initialized[slot] = false;
    }

    private Update update(RoomEntity entity, int countdown, int physicsFlags,
                          int nextSpeedX, int nextSpeedY, SpawnRequest spawnRequest,
                          boolean unloadRequested, boolean appliesBackgroundInteraction) {
        return new Update(entity, state[entity.slot()], privateState1[entity.slot()],
            countdown & 0xFF, physicsFlags & 0xFF, nextSpeedX & 0xFF,
            nextSpeedY & 0xFF, spawnRequest, unloadRequested,
            appliesBackgroundInteraction);
    }

    private PositionAndCollision moveWithBackground(RoomEntity entity,
                                                     RoomEntityBackgroundInteraction interaction,
                                                     int ignoreHitsCountdown, int frameCounter) {
        int slot = entity.slot();
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        int collisionMask = 0;

        if (x != entity.x()) {
            int direction = speedX[slot] < 0x80
                ? EntityBackgroundCollisionResult.RIGHT
                : EntityBackgroundCollisionResult.LEFT;
            EntityBackgroundCollisionResult result = interaction.probe(entity, direction,
                x, entity.y(), ignoreHitsCountdown, frameCounter);
            if (result.blocked()) {
                collisionMask |= result.collisionFlag();
                x = entity.x();
            }
        }
        if (y != entity.y()) {
            int direction = speedY[slot] < 0x80
                ? EntityBackgroundCollisionResult.DOWN
                : EntityBackgroundCollisionResult.UP;
            EntityBackgroundCollisionResult result = interaction.probe(entity, direction,
                x, y, ignoreHitsCountdown, frameCounter);
            if (result.blocked()) {
                collisionMask |= result.collisionFlag();
                y = entity.y();
            }
        }
        return new PositionAndCollision(x, y, collisionMask);
    }

    private static boolean isAllowedSpawnPhysics(int physicsFlag) {
        return physicsFlag == 0x00 || physicsFlag == 0x06 || physicsFlag == 0x09;
    }

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

    private static Vector vectorTowardsLink(int entityX, int entityY,
                                            int linkEntityX, int linkEntityY, int length) {
        int distanceX = signedByte(linkEntityX - entityX);
        int distanceY = signedByte(linkEntityY - entityY);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean yIsDominant = absoluteX < absoluteY;
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
        int x = yIsDominant ? result : length;
        int y = yIsDominant ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new Vector(x & 0xFF, y & 0xFF);
    }

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y,
                                                       int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
    }

    private static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            entity.z());
    }

    private static RoomEntity withVariant(RoomEntity entity, int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            entity.x(), entity.y(), entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
    }

    private void reset(int slot) {
        state[slot] = 0;
        privateState1[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
    }

    private static void checkType(RoomEntity entity) {
        if (entity == null || (entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Zombie entity");
        }
    }

    private static void validateByte(int value, String name) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(name + " must be an unsigned byte: " + value);
        }
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private record Vector(int x, int y) {
    }

    private record PositionAndCollision(int x, int y, int collisionMask) {
    }
}
