package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$05 CuccoEntityHandler flight, lift, and angry-spawn state. */
public final class CuccoMotion {
    static final int ENTITY_TYPE = 0x6C;
    public static final int INITIAL_PHYSICS_FLAGS = 0x02 | 0x10 | 0x80;
    public static final int ANGRY_PHYSICS_FLAGS = 0x02 | 0x10;
    public static final int ANGRY_HITBOX_FLAGS = 0x80;
    public static final int ANGRY_OPTIONS1 = 0x40;
    public static final int HEALTH_OVERRIDE = 0x4C;
    public static final int CUCCO_HURT_WAVE_SFX = 0x13;
    public static final int LIFT_WAVE_SFX = 0x02;

    static final int[] SPEED_BY_RANDOM = {0x00, 0x04, 0x06, 0x04, 0x00, 0xFC, 0xFA, 0xFC};
    static final int[] SPAWN_X = {
        0x28, 0x48, 0x68, 0x88, 0x18, 0x38, 0x58, 0x78,
        0x00, 0x00, 0x00, 0x00, 0xA0, 0xA0, 0xA0, 0xA0
    };
    static final int[] SPAWN_Y = {
        0x00, 0x00, 0x00, 0x00, 0x90, 0x90, 0x90, 0x90,
        0x20, 0x40, 0x60, 0x80, 0x20, 0x40, 0x60, 0x80
    };

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] spriteVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record SpawnRequest(int x, int y, int z, int speedX, int speedY) {
        public SpawnRequest {
            x &= 0xFF;
            y &= 0xFF;
            z &= 0xFF;
            speedX &= 0xFF;
            speedY &= 0xFF;
        }
    }

    public record Update(
            RoomEntity entity,
            int transitionCountdown,
            int flashCountdown,
            boolean liftRequested,
            boolean unloaded,
            boolean boomerangSound,
            int waveSoundId,
            int dialogTableId,
            int dialogLowId,
            SpawnRequest spawnRequest) {}

    public void initialize(int slot) {
        clear(slot);
        initialized[slot] = true;
    }

    public void initializeAngry(int slot, int newSpeedX, int newSpeedY) {
        initialize(slot);
        state[slot] = 3;
        speedX[slot] = newSpeedX & 0xFF;
        speedY[slot] = newSpeedY & 0xFF;
    }

    /**
     * Advances one active Cucco handler pass. Countdown and flash values are
     * the bytes after the shared entity-loop decrement, matching the source
     * call order in AnimateEntities.
     */
    public Update advance(CuccoFrame inputs) {
        return CuccoBehavior.advance(this, inputs);
    }

    public Update advance(CuccoInteractionFrame inputs) {
        return CuccoBehavior.advance(this, inputs);
    }

    public void clear(int slot) {
        state[slot] = 0;
        spriteVariant[slot] = 0;
        direction[slot] = 0;
        privateState1[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        initialized[slot] = false;
    }

    int state(int slot) {
        return state[slot];
    }

    int direction(int slot) {
        return direction[slot];
    }

    int privateState1(int slot) {
        return privateState1[slot];
    }

    int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    public int speedZ(int slot) {
        return speedZ[slot] & 0xFF;
    }

    Position move(RoomEntity entity, int x, int y, RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        if (backgroundCollision != null
                && nextX != x
                && backgroundCollision.blocks(entity, horizontalDirection(speedX[slot]), nextX, y)) {
            nextX = x;
        }
        if (backgroundCollision != null
                && nextY != y
                && backgroundCollision.blocks(entity, verticalDirection(speedY[slot]), nextX, nextY)) {
            nextY = y;
        }
        return new Position(nextX, nextY);
    }

    static RoomEntity withPosition(RoomEntity entity, int x, int y, int z, int variant) {
        return EntitySnapshots.withMotion(
                entity,
                x & 0xFF,
                y & 0xFF,
                variant,
                entity.entityFlipAttribute(),
                z & 0xFF,
                entity.deathSpriteVariant(),
                entity.powerRecoilDeath());
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

    static Vector vectorTowardsLink(int entityX, int entityY, int entityZ, int linkX, int linkY, int length) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY + entityZ) & 0xFF);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean swapped = absoluteX < absoluteY;
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int result = romDivide(length, smallerDistance, largerDistance);
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

    private static int romDivide(int length, int smallerDistance, int largerDistance) {
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
        return result;
    }

    static int negate(int value) {
        return (-signedByte(value)) & 0xFF;
    }

    private static int horizontalDirection(int speed) {
        return signedByte(speed) < 0 ? EntityBackgroundCollisionResult.LEFT : EntityBackgroundCollisionResult.RIGHT;
    }

    private static int verticalDirection(int speed) {
        return signedByte(speed) < 0 ? EntityBackgroundCollisionResult.UP : EntityBackgroundCollisionResult.DOWN;
    }

    static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    static int withUnsignedByte(int value) {
        return value & 0xFF;
    }

    void ensureInitialized(int slot) {
        if (!initialized[slot]) {
            initialize(slot);
        }
    }

    record Position(int x, int y) {}

    record Vector(int x, int y) {}
}
