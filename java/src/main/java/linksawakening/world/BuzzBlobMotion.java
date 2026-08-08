package linksawakening.world;

import java.util.Objects;
import java.util.function.IntSupplier;

/** Bank-$18 BuzzBlobEntityHandler movement and animation state. */
final class BuzzBlobMotion {
    static final int ENTITY_TYPE = 0xB9;
    static final int INITIAL_PHYSICS_FLAGS = 0x12;
    static final int OPTIONS1 = 0x00;

    // Data_018_777C and the bytes immediately preceding it in the ROM.
    private static final int[] SPEED_X = {
        0x00, 0x04, 0x06, 0x04, 0x00, 0xFC, 0xFA, 0xFC
    };
    private static final int[] SPEED_Y = {
        0xFA, 0xFC, 0x00, 0x04, 0x06, 0x04, 0x00, 0xFC
    };
    private static final int[] STATE_ZERO_VARIANTS = {0x00, 0x01, 0x02, 0x01};
    private static final int[] STATE_ONE_VARIANTS = {0x03, 0x04, 0x05, 0x04};
    private static final int[] PRIVATE_STATE_VARIANTS = {0x06, 0x07, 0x08, 0x07};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState2 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record Update(RoomEntity entity, int state, int privateState1, int privateState2,
                  int transitionCountdown, int speedX, int speedY,
                  boolean appliesBackgroundInteraction) {
    }

    void initialize(int slot) {
        reset(slot);
        initialized[slot] = true;
    }

    /** Seeds the alternate state used by the handler's non-default path. */
    void initializeStateOne(int slot) {
        reset(slot);
        state[slot] = 1;
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int frameCounter, int transitionCountdown,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundInteraction backgroundInteraction) {
        checkType(entity);
        validateByte(transitionCountdown, "Buzz Blob transition countdown");
        Objects.requireNonNull(randomByteSupplier, "Buzz Blob random-byte supplier");

        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        int countdown = transitionCountdown & 0xFF;
        RoomEntity updated = entity;
        boolean appliesBackgroundInteraction = false;
        switch (state[slot]) {
            case 0 -> {
                if (countdown == 0) {
                    countdown = (randomByteSupplier.getAsInt() & 0x3F) + 0x30;
                    int directionIndex = countdown & 0x07;
                    speedX[slot] = SPEED_X[directionIndex];
                    speedY[slot] = SPEED_Y[directionIndex];
                }

                PositionAndCollision moved = moveWithBackground(entity,
                    backgroundInteraction, slot);
                appliesBackgroundInteraction = true;
                updated = withPosition(entity, moved.x(), moved.y());
                int[] variants = privateState1[slot] == 0
                    ? STATE_ZERO_VARIANTS : PRIVATE_STATE_VARIANTS;
                updated = withVariant(updated,
                    variants[(frameCounter >>> 3) & 0x03]);
            }
            case 1 -> {
                if (countdown == 0) {
                    // The ROM increments state and returns before selecting a
                    // new display variant; the next state-$00 pass does that.
                    state[slot] = 0;
                } else {
                    updated = withVariant(entity,
                        STATE_ONE_VARIANTS[(frameCounter >>> 3) & 0x03]);
                }
            }
            default -> throw new IllegalStateException(
                "Invalid Buzz Blob state: " + state[slot]);
        }

        return new Update(updated, state[slot], privateState1[slot], privateState2[slot],
            countdown, speedX[slot] & 0xFF, speedY[slot] & 0xFF,
            appliesBackgroundInteraction);
    }

    int state(int slot) {
        return state[slot];
    }

    int privateState1(int slot) {
        return privateState1[slot];
    }

    int privateState2(int slot) {
        return privateState2[slot];
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

        if (interaction != null && x != entity.x()) {
            int direction = signedByte(speedX[slot]) < 0
                ? EntityBackgroundCollisionResult.LEFT
                : EntityBackgroundCollisionResult.RIGHT;
            EntityBackgroundCollisionResult result = interaction.probe(
                entity, direction, x, entity.y());
            if (result.blocked()) {
                x = entity.x() & 0xFF;
            }
        }
        if (interaction != null && y != entity.y()) {
            int direction = signedByte(speedY[slot]) < 0
                ? EntityBackgroundCollisionResult.UP
                : EntityBackgroundCollisionResult.DOWN;
            EntityBackgroundCollisionResult result = interaction.probe(
                entity, direction, x, y);
            if (result.blocked()) {
                y = entity.y() & 0xFF;
            }
        }
        return new PositionAndCollision(x, y);
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
        privateState2[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
    }

    private static void checkType(RoomEntity entity) {
        if (entity == null || (entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Buzz Blob entity");
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

    private record PositionAndCollision(int x, int y) {
    }

}
