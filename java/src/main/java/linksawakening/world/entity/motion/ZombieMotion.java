package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Bank-$18 ZombieEntityHandler hidden-parent and child state machine. */
public final class ZombieMotion {
    public static final int ENTITY_TYPE = 0xBF;
    public static final int INITIAL_PHYSICS_FLAGS = 0x52;
    public static final int OPTIONS1 = 0x00;
    public static final int PROJECTILE_NOCLIP_FLAG = 0x40;

    // Data_018_637D and Data_018_6385.
    static final int[] SPAWN_Y = {0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, 0x80};
    static final int[] SPAWN_X = {0x18, 0x28, 0x38, 0x48, 0x58, 0x68, 0x78, 0x88};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] selectedVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record SpawnRequest(int x, int y) {}

    public record Update(
            RoomEntity entity,
            int state,
            int privateState1,
            int transitionCountdown,
            int physicsFlags,
            int speedX,
            int speedY,
            SpawnRequest spawnRequest,
            boolean unloadRequested,
            boolean appliesBackgroundInteraction) {}

    public void initialize(int slot) {
        reset(slot);
        initialized[slot] = true;
    }

    /** Seeds the private-state-$01 child created by SpawnNewEntityInRange. */
    public void initializeChild(int slot) {
        reset(slot);
        privateState1[slot] = 1;
        initialized[slot] = true;
    }

    public Update advance(ZombieFrame inputs) {
        return zombieStep.run(this, inputs);
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
    public boolean allowsEnemyCollision(int slot) {
        return initialized[slot] && privateState1[slot] != 0 && state[slot] == 2;
    }

    int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    public void clear(int slot) {
        reset(slot);
        initialized[slot] = false;
    }

    Update update(
            RoomEntity entity,
            int countdown,
            int physicsFlags,
            int nextSpeedX,
            int nextSpeedY,
            SpawnRequest spawnRequest,
            boolean unloadRequested,
            boolean appliesBackgroundInteraction) {
        return new Update(
                entity,
                state[entity.slot()],
                privateState1[entity.slot()],
                countdown & 0xFF,
                physicsFlags & 0xFF,
                nextSpeedX & 0xFF,
                nextSpeedY & 0xFF,
                spawnRequest,
                unloadRequested,
                appliesBackgroundInteraction);
    }

    PositionAndCollision moveWithBackground(
            RoomEntity entity, RoomEntityBackgroundInteraction interaction, int ignoreHitsCountdown, int frameCounter) {
        int slot = entity.slot();
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        int collisionMask = 0;

        // ApplyEntityInteractionWithBackground probes nonzero speeds even
        // when fixed-point movement has not advanced a complete pixel.
        if (speedX[slot] != 0) {
            int direction =
                    speedX[slot] < 0x80 ? EntityBackgroundCollisionResult.RIGHT : EntityBackgroundCollisionResult.LEFT;
            EntityBackgroundCollisionResult result =
                    interaction.probe(entity, direction, x, y, ignoreHitsCountdown, frameCounter);
            if (result.blocked()) {
                collisionMask |= result.collisionFlag();
                x = entity.x();
            }
        }
        if (speedY[slot] != 0) {
            int direction =
                    speedY[slot] < 0x80 ? EntityBackgroundCollisionResult.DOWN : EntityBackgroundCollisionResult.UP;
            EntityBackgroundCollisionResult result =
                    interaction.probe(entity, direction, x, y, ignoreHitsCountdown, frameCounter);
            if (result.blocked()) {
                collisionMask |= result.collisionFlag();
                y = entity.y();
            }
        }
        return new PositionAndCollision(x, y, collisionMask);
    }

    static boolean isAllowedSpawnPhysics(int physicsFlag) {
        return physicsFlag == 0x00 || physicsFlag == 0x06 || physicsFlag == 0x09;
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

    static Vector vectorTowardsLink(int entityX, int entityY, int linkEntityX, int linkEntityY, int length) {
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

    static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y, int variant) {
        return EntitySnapshots.withMotion(
                entity, x & 0xFF, y & 0xFF, variant, entity.entityFlipAttribute(), entity.z(), -1, false);
    }

    static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return EntitySnapshots.withMotion(
                entity,
                x & 0xFF,
                y & 0xFF,
                entity.spriteVariant(),
                entity.entityFlipAttribute(),
                entity.z(),
                -1,
                false);
    }

    static RoomEntity withVariant(RoomEntity entity, int variant) {
        return EntitySnapshots.withMotion(
                entity, entity.x(), entity.y(), variant, entity.entityFlipAttribute(), entity.z(), -1, false);
    }

    private void reset(int slot) {
        state[slot] = 0;
        privateState1[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        selectedVariant[slot] = 0;
    }

    static void checkType(RoomEntity entity) {
        if (entity == null || (entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Zombie entity");
        }
    }

    static void validateByte(int value, String name) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(name + " must be an unsigned byte: " + value);
        }
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    record Vector(int x, int y) {}

    record PositionAndCollision(int x, int y, int collisionMask) {}

    final ZombieStep zombieStep = new ZombieStep();
}
