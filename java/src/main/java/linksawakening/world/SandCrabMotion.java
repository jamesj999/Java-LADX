package linksawakening.world;

import java.util.Objects;
import java.util.function.IntSupplier;

/** Bank-$15 SandCrabEntityHandler movement and direction selection. */
final class SandCrabMotion {
    static final int ENTITY_TYPE = 0xC6;
    static final int INITIAL_PHYSICS_FLAGS = 0x12;
    static final int OPTIONS1 = 0x02;

    // Data_015_7328 and Data_015_732C.
    private static final int[] SPEED_X = {0x10, 0xF0, 0x00, 0x00};
    private static final int[] SPEED_Y = {0x00, 0x00, 0xFB, 0x05};

    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record Update(RoomEntity entity, int transitionCountdown, int speedX, int speedY,
                  int collisionFlags, boolean appliesBackgroundInteraction) {
    }

    void initialize(int slot) {
        reset(slot);
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int frameCounter, int transitionCountdown,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundInteraction backgroundInteraction) {
        checkType(entity);
        validateByte(transitionCountdown, "Sand Crab transition countdown");
        Objects.requireNonNull(randomByteSupplier, "Sand Crab random-byte supplier");

        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        int countdown = transitionCountdown & 0xFF;
        PositionAndCollision moved = moveWithBackground(entity, backgroundInteraction, slot);
        RoomEntity updated = withPositionAndVariant(entity, moved.x(), moved.y(),
            (frameCounter >>> 3) & 0x01);

        if ((moved.collisionFlags() & 0x0F) != 0 || countdown == 0) {
            countdown = (randomByteSupplier.getAsInt() & 0x7F) + 0x30;
            int direction = randomByteSupplier.getAsInt() & 0x03;
            speedX[slot] = SPEED_X[direction];
            speedY[slot] = SPEED_Y[direction];
        }

        return new Update(updated, countdown, speedX[slot] & 0xFF,
            speedY[slot] & 0xFF, moved.collisionFlags() & 0x0F, true);
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

    private PositionAndCollision moveWithBackground(RoomEntity entity,
                                                     RoomEntityBackgroundInteraction interaction,
                                                     int slot) {
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        int collisionFlags = 0;

        if (interaction != null && x != entity.x()) {
            int direction = signedByte(speedX[slot]) < 0
                ? EntityBackgroundCollisionResult.LEFT : EntityBackgroundCollisionResult.RIGHT;
            EntityBackgroundCollisionResult result = interaction.probe(
                entity, direction, x, entity.y());
            if (result.blocked()) {
                collisionFlags |= result.collisionFlag();
                x = entity.x() & 0xFF;
            }
        }
        if (interaction != null && y != entity.y()) {
            int direction = signedByte(speedY[slot]) < 0
                ? EntityBackgroundCollisionResult.UP : EntityBackgroundCollisionResult.DOWN;
            EntityBackgroundCollisionResult result = interaction.probe(entity, direction, x, y);
            if (result.blocked()) {
                collisionFlags |= result.collisionFlag();
                y = entity.y() & 0xFF;
            }
        }
        return new PositionAndCollision(x, y, collisionFlags);
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

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y,
                                                       int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
    }

    private void reset(int slot) {
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
    }

    private static void checkType(RoomEntity entity) {
        if (entity == null || (entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Sand Crab entity");
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

    private record PositionAndCollision(int x, int y, int collisionFlags) {
    }
}
