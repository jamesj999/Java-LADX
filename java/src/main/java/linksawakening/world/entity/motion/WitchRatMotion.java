package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Bank-$15 WitchRatEntityHandler movement, hopping, and facing state. */
public final class WitchRatMotion {
    public static final int ENTITY_TYPE = 0xE1;
    public static final int INITIAL_PHYSICS_FLAGS = 0xD2;
    public static final int OPTIONS1 = 0x02;

    // Data_015_78D6.
    static final int[] SPEEDS = {0x02, 0x08, 0x0C, 0x08, 0xFE, 0xF8, 0xF4, 0xF8};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] storedSpriteVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record Update(
            RoomEntity entity,
            int state,
            int transitionCountdown,
            int direction,
            int speedX,
            int speedY,
            int speedZ,
            int collisionFlags,
            boolean appliesBackgroundInteraction) {}

    public void initialize(int slot) {
        validateSlot(slot);
        reset(slot);
        initialized[slot] = true;
    }

    public Update advance(
            RoomEntity entity,
            int frameCounter,
            int transitionCountdown,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundInteraction backgroundInteraction) {
        return new WitchRatFrameStep(
                        this, entity, frameCounter, transitionCountdown, randomByteSupplier, backgroundInteraction)
                .run();
    }

    public int state(int slot) {
        validateSlot(slot);
        return state[slot];
    }

    int direction(int slot) {
        validateSlot(slot);
        return direction[slot];
    }

    int speedX(int slot) {
        validateSlot(slot);
        return speedX[slot] & 0xFF;
    }

    int speedY(int slot) {
        validateSlot(slot);
        return speedY[slot] & 0xFF;
    }

    public int speedZ(int slot) {
        validateSlot(slot);
        return speedZ[slot] & 0xFF;
    }

    public void clear(int slot) {
        validateSlot(slot);
        reset(slot);
        initialized[slot] = false;
    }

    PositionAndCollision moveWithBackground(RoomEntity entity, RoomEntityBackgroundInteraction interaction, int slot) {
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        int collisionFlags = 0;
        if (interaction != null && x != entity.x()) {
            int direction = signedByte(speedX[slot]) < 0
                    ? EntityBackgroundCollisionResult.LEFT
                    : EntityBackgroundCollisionResult.RIGHT;
            EntityBackgroundCollisionResult result = interaction.probe(entity, direction, x, entity.y());
            if (result.blocked()) {
                collisionFlags |= result.collisionFlag();
                x = entity.x() & 0xFF;
            }
        }
        if (interaction != null && y != entity.y()) {
            int direction = signedByte(speedY[slot]) < 0
                    ? EntityBackgroundCollisionResult.UP
                    : EntityBackgroundCollisionResult.DOWN;
            EntityBackgroundCollisionResult result = interaction.probe(entity, direction, x, y);
            if (result.blocked()) {
                collisionFlags |= result.collisionFlag();
                y = entity.y() & 0xFF;
            }
        }
        return new PositionAndCollision(x, y, collisionFlags & 0x0F);
    }

    void reset(int slot) {
        state[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        storedSpriteVariant[slot] = 0;
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

    static void checkType(RoomEntity entity) {
        if (entity == null || (entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Witch Rat entity");
        }
    }

    static void validateSlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Witch Rat slot out of range: " + slot);
        }
    }

    static void validateByte(int value, String name) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(name + " must be an unsigned byte: " + value);
        }
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

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    record PositionAndCollision(int x, int y, int collisionFlags) {}
}
