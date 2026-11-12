package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$15 FishEntityHandler's hidden swim and jump loop. */
public final class FishMotion {
    public static final int ENTITY_TYPE = 0xCC;

    public static final int INITIAL_PHYSICS_FLAGS = 0x02 | 0x10 | 0x40;
    static final int JUMP_PHYSICS_FLAGS = 0x02 | 0x10;

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] physicsFlags = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Entity $CC uses EntityInitNoop; the handler seeds its own state. */
    public void initialize(int slot) {
        state[slot] = 0;
        speedX[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        physicsFlags[slot] = INITIAL_PHYSICS_FLAGS;
        initialized[slot] = true;
    }

    /**
     * Advances the handler after the shared entity loop has decremented the
     * transition byte for this frame.
     */
    public Update advance(
            RoomEntity entity,
            int transitionCountdown,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        return new FishFrameStep(this, entity, transitionCountdown, randomByteSupplier, backgroundCollision).run();
    }

    public void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    public int state(int slot) {
        return state[slot];
    }

    public int speedX(int slot) {
        return speedX[slot];
    }

    public int speedZ(int slot) {
        return speedZ[slot];
    }

    int physicsFlags(int slot) {
        return physicsFlags[slot];
    }

    public boolean allowsEnemyCollision(int slot) {
        return state[slot] == 2;
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

    static int horizontalDirection(int speed) {
        return signedByte(speed) < 0 ? EntityBackgroundCollisionResult.LEFT : EntityBackgroundCollisionResult.RIGHT;
    }

    static int negate(int speed) {
        return (-signedByte(speed)) & 0xFF;
    }

    static int arithmeticShiftRight(int speed) {
        return (signedByte(speed) >> 1) & 0xFF;
    }

    static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    public record Update(RoomEntity entity, int transitionCountdown, int physicsFlags, boolean waterSplash) {}
}
