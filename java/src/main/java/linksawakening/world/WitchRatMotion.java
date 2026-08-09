package linksawakening.world;

import java.util.Objects;
import java.util.function.IntSupplier;

/** Bank-$15 WitchRatEntityHandler movement, hopping, and facing state. */
final class WitchRatMotion {
    static final int ENTITY_TYPE = 0xE1;
    static final int INITIAL_PHYSICS_FLAGS = 0xD2;
    static final int OPTIONS1 = 0x02;

    // Data_015_78D6.
    private static final int[] SPEEDS = {
        0x02, 0x08, 0x0C, 0x08, 0xFE, 0xF8, 0xF4, 0xF8
    };

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] storedSpriteVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record Update(RoomEntity entity, int state, int transitionCountdown, int direction,
                  int speedX, int speedY, int speedZ, int collisionFlags,
                  boolean appliesBackgroundInteraction) {
    }

    void initialize(int slot) {
        validateSlot(slot);
        reset(slot);
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int frameCounter, int transitionCountdown,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundInteraction backgroundInteraction) {
        checkType(entity);
        validateByte(frameCounter, "Witch Rat frame counter");
        validateByte(transitionCountdown, "Witch Rat transition countdown");
        Objects.requireNonNull(randomByteSupplier, "Witch Rat random-byte supplier");

        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        int countdown = transitionCountdown & 0xFF;
        int renderedVariant = storedSpriteVariant[slot]
            + (direction[slot] == 0 ? 0x02 : 0x00);
        int z = addSpeedToPosition(entity.z(), speedZ[slot], speedZAccumulator, slot);
        speedZ[slot] = (speedZ[slot] - 2) & 0xFF;
        boolean wrappedZ = (z & 0x80) != 0;
        if (wrappedZ) {
            z = 0;
            speedZ[slot] = 0;
        }

        int collisionFlags = 0;
        boolean appliesBackgroundInteraction = false;
        switch (state[slot]) {
            case 0 -> {
                if (countdown == 0) {
                    int index = randomByteSupplier.getAsInt() & 0x07;
                    speedX[slot] = SPEEDS[index];
                    direction[slot] = index & 0x04;
                    int yIndex = randomByteSupplier.getAsInt() & 0x07;
                    speedY[slot] = SPEEDS[yIndex];
                    countdown = (randomByteSupplier.getAsInt() & 0x1F) + 0x30;
                    state[slot] = 1;
                } else {
                    storedSpriteVariant[slot] = 0x01;
                    if ((frameCounter & 0x1F) == 0
                        && (randomByteSupplier.getAsInt() & 0x01) == 0) {
                        direction[slot] ^= 0x04;
                    }
                }
            }
            case 1 -> {
                PositionAndCollision moved = moveWithBackground(entity,
                    backgroundInteraction, slot);
                entity = withPosition(entity, moved.x(), moved.y(), z, renderedVariant);
                collisionFlags = moved.collisionFlags();
                appliesBackgroundInteraction = true;
                if (wrappedZ) {
                    if (countdown == 0) {
                        countdown = 0x48;
                        state[slot] = 0;
                        // The ROM returns before its final SetEntitySpriteVariant.
                        return new Update(entity, state[slot], countdown,
                            direction[slot], speedX[slot] & 0xFF, speedY[slot] & 0xFF,
                            speedZ[slot] & 0xFF, collisionFlags, appliesBackgroundInteraction);
                    }
                    speedZ[slot] = 0x08;
                    z = (z + 1) & 0xFF;
                    entity = withPosition(entity, moved.x(), moved.y(), z, renderedVariant);
                }
                storedSpriteVariant[slot] = 0x00;
            }
            default -> throw new IllegalStateException("Invalid Witch Rat state: " + state[slot]);
        }

        // State 0 did not move in X/Y; state 1 already replaced those
        // coordinates with the background-probed values. Both paths still
        // need the top-of-handler Z update and the source-rendered variant.
        entity = withPosition(entity, entity.x(), entity.y(), z, renderedVariant);
        return new Update(entity, state[slot], countdown, direction[slot],
            speedX[slot] & 0xFF, speedY[slot] & 0xFF, speedZ[slot] & 0xFF,
            collisionFlags, appliesBackgroundInteraction);
    }

    void setStateForTest(int slot, int newState, int transitionCountdown, int newDirection,
                         int newSpeedX, int newSpeedY, int newSpeedZ) {
        validateSlot(slot);
        if (newState < 0 || newState > 1) {
            throw new IllegalArgumentException("Witch Rat state must be 0 or 1: " + newState);
        }
        validateByte(transitionCountdown, "Witch Rat transition countdown");
        if (newDirection != 0 && newDirection != 0x04) {
            throw new IllegalArgumentException("Witch Rat direction must be 0 or 4: "
                + newDirection);
        }
        validateByte(newSpeedX, "Witch Rat X speed");
        validateByte(newSpeedY, "Witch Rat Y speed");
        validateByte(newSpeedZ, "Witch Rat Z speed");
        reset(slot);
        state[slot] = newState;
        direction[slot] = newDirection;
        speedX[slot] = newSpeedX;
        speedY[slot] = newSpeedY;
        speedZ[slot] = newSpeedZ;
        initialized[slot] = true;
    }

    int state(int slot) {
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

    int speedZ(int slot) {
        validateSlot(slot);
        return speedZ[slot] & 0xFF;
    }

    void clear(int slot) {
        validateSlot(slot);
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
        return new PositionAndCollision(x, y, collisionFlags & 0x0F);
    }

    private void reset(int slot) {
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

    private static RoomEntity withPosition(RoomEntity entity, int x, int y, int z,
                                            int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), z & 0xFF,
            entity.deathSpriteVariant(), entity.powerRecoilDeath());
    }

    private static void checkType(RoomEntity entity) {
        if (entity == null || (entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Witch Rat entity");
        }
    }

    private static void validateSlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Witch Rat slot out of range: " + slot);
        }
    }

    private static void validateByte(int value, String name) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(name + " must be an unsigned byte: " + value);
        }
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

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private record PositionAndCollision(int x, int y, int collisionFlags) {
    }
}
