package linksawakening.world;

import java.util.Objects;
import java.util.function.IntSupplier;

/** Bank-$15 PokeyEntityHandler movement, segment spawning, and wall bounce. */
final class PokeyMotion {
    static final int ENTITY_TYPE = 0xE3;
    static final int INITIAL_PHYSICS_FLAGS = 0x12;
    static final int OPTIONS1 = 0x02;
    static final int SEGMENT_TRANSITION_COUNTDOWN = 0x18;

    private static final int VECTOR_LENGTH = 0x20;
    private static final int[] SPEED_X_BY_RANDOM_INDEX = {
        0x00, 0x04, 0x06, 0x04, 0x00, 0xFC, 0xFA, 0xFC
    };
    // Data_015_4BD7 is only declared as two bytes, so the handler's indexed
    // reads continue directly into Data_015_4BD9 in the assembled ROM.
    private static final int[] SPEED_Y_BY_RANDOM_INDEX = {
        0xFA, 0xFC, 0x00, 0x04, 0x06, 0x04, 0x00, 0xFC
    };

    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record SegmentSpawn(int x, int y, int z, int speedX, int speedY,
                        int transitionCountdown) {
    }

    record Update(RoomEntity entity, int transitionCountdown, int flashCountdown,
                  int inertia, int renderInertia, SegmentSpawn segmentSpawn,
                  boolean unloadRequested, boolean bumpJingle) {
    }

    /** Mirrors the main entity's normal room initialization. */
    void initialize(int slot) {
        reset(slot);
        initialized[slot] = true;
    }

    /** Seeds a SpawnNewEntity child with private state one and the away vector. */
    void initializeSegment(int slot, int newSpeedX, int newSpeedY) {
        validateByte(newSpeedX, "Pokey segment X speed");
        validateByte(newSpeedY, "Pokey segment Y speed");
        reset(slot);
        privateState1[slot] = 1;
        speedX[slot] = newSpeedX & 0xFF;
        speedY[slot] = newSpeedY & 0xFF;
        initialized[slot] = true;
    }

    Update advanceMain(RoomEntity entity, int frameCounter, int inertia,
                       int transitionCountdown, int flashCountdown,
                       int linkEntityX, int linkEntityY,
                       IntSupplier randomByteSupplier,
                       RoomEntityBackgroundCollision backgroundCollision) {
        checkType(entity);
        validateByte(inertia, "Pokey inertia");
        validateByte(transitionCountdown, "Pokey transition countdown");
        validateByte(flashCountdown, "Pokey flash countdown");
        Objects.requireNonNull(randomByteSupplier, "Pokey random-byte supplier");
        ensureMain(entity.slot());

        int renderInertia = inertia;
        int nextInertia = inertia;
        int nextFlashCountdown = flashCountdown;
        SegmentSpawn segmentSpawn = null;
        if (inertia < 0x02 && flashCountdown == 0x14) {
            // The ROM commits these writes before SpawnNewEntity reports a
            // full room, so the main body still advances its state even when
            // no detached segment slot is available.
            nextFlashCountdown = 0x13;
            nextInertia = inertia + 1;
            Vector vector = vectorTowardsLink(entity.x(), entity.y(), entity.z(),
                linkEntityX, linkEntityY, VECTOR_LENGTH);
            segmentSpawn = new SegmentSpawn(entity.x(), entity.y(), entity.z(),
                negate(vector.x()), negate(vector.y()), SEGMENT_TRANSITION_COUNTDOWN);
        }

        int nextTransitionCountdown = transitionCountdown;
        if (nextTransitionCountdown == 0) {
            int timer = (randomByteSupplier.getAsInt() & 0x3F) + 0x30;
            nextTransitionCountdown = timer;
            int index = timer & 0x07;
            speedX[entity.slot()] = SPEED_X_BY_RANDOM_INDEX[index];
            speedY[entity.slot()] = SPEED_Y_BY_RANDOM_INDEX[index];
        }

        int x = addSpeedToPosition(entity.x(), speedX[entity.slot()],
            speedXAccumulator, entity.slot());
        int y = addSpeedToPosition(entity.y(), speedY[entity.slot()],
            speedYAccumulator, entity.slot());
        RoomEntity moved = withPositionAndVariant(entity, x, y,
            (frameCounter >>> 3) & 0x03);
        // PokeyEntityHandler calls ApplyEntityInteractionWithBackground after
        // its position update. The room runtime owns the authoritative probe;
        // retain the probe ordering without inventing a snap coordinate in
        // this boolean-only motion seam.
        collisionMask(moved, backgroundCollision);
        return new Update(moved, nextTransitionCountdown, nextFlashCountdown,
            nextInertia, renderInertia, segmentSpawn, false, false);
    }

    Update advanceSegment(RoomEntity entity, int inertia, int transitionCountdown,
                           RoomEntityBackgroundCollision backgroundCollision) {
        checkType(entity);
        validateByte(inertia, "Pokey segment inertia");
        validateByte(transitionCountdown, "Pokey segment transition countdown");
        ensureSegment(entity.slot());

        int slot = entity.slot();
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        RoomEntity moved = withPosition(entity, x, y);
        int collisionMask = collisionMask(moved, backgroundCollision);
        int nextInertia = inertia;
        boolean bumpJingle = false;

        if ((collisionMask & 0x03) != 0) {
            speedX[slot] = negate(speedX[slot]);
            nextInertia++;
            bumpJingle = true;
            if (nextInertia >= 0x03) {
                return new Update(moved, transitionCountdown, 0, nextInertia,
                    nextInertia, null, true, true);
            }
        }
        if ((collisionMask & 0x0C) != 0) {
            speedY[slot] = negate(speedY[slot]);
            nextInertia++;
            bumpJingle = true;
            if (nextInertia >= 0x03) {
                return new Update(moved, transitionCountdown, 0, nextInertia,
                    nextInertia, null, true, bumpJingle);
            }
        }
        return new Update(moved, transitionCountdown, 0, nextInertia,
            nextInertia, null, false, bumpJingle);
    }

    boolean isSegment(int slot) {
        return initialized[slot] && privateState1[slot] != 0;
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

    void clear(int slot) {
        reset(slot);
        initialized[slot] = false;
    }

    private void ensureMain(int slot) {
        if (!initialized[slot] || privateState1[slot] != 0) {
            initialize(slot);
        }
    }

    private void ensureSegment(int slot) {
        if (!initialized[slot]) {
            initializeSegment(slot, 0, 0);
        }
        if (privateState1[slot] == 0) {
            throw new IllegalStateException("Pokey segment state is not initialized");
        }
    }

    private void reset(int slot) {
        privateState1[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
    }

    private static int collisionMask(RoomEntity entity,
                                     RoomEntityBackgroundCollision backgroundCollision) {
        if (backgroundCollision == null) {
            return 0;
        }
        int mask = 0;
        if (backgroundCollision.blocks(entity, EntityBackgroundCollisionResult.RIGHT,
            entity.x(), entity.y())) {
            mask |= 0x01;
        }
        if (backgroundCollision.blocks(entity, EntityBackgroundCollisionResult.LEFT,
            entity.x(), entity.y())) {
            mask |= 0x02;
        }
        if (backgroundCollision.blocks(entity, EntityBackgroundCollisionResult.UP,
            entity.x(), entity.y())) {
            mask |= 0x04;
        }
        if (backgroundCollision.blocks(entity, EntityBackgroundCollisionResult.DOWN,
            entity.x(), entity.y())) {
            mask |= 0x08;
        }
        return mask;
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

    private static Vector vectorTowardsLink(int entityX, int entityY, int entityZ,
                                            int linkEntityX, int linkEntityY, int length) {
        int distanceX = signedByte(linkEntityX - entityX);
        int distanceY = signedByte(linkEntityY - entityY + entityZ);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean yIsDominant = absoluteX < absoluteY;
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int smallerComponent = romDivide(length, smallerDistance, largerDistance);
        int vectorX = yIsDominant ? smallerComponent : length;
        int vectorY = yIsDominant ? length : smallerComponent;
        if (distanceX < 0) {
            vectorX = -vectorX;
        }
        if (distanceY < 0) {
            vectorY = -vectorY;
        }
        return new Vector(vectorX & 0xFF, vectorY & 0xFF);
    }

    private static int romDivide(int length, int smallerDistance, int largerDistance) {
        if (length == 0) {
            return 0;
        }
        if (largerDistance == 0) {
            return length;
        }
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum & 0xFF;
        }
        return result;
    }

    private static int negate(int value) {
        return (-signedByte(value)) & 0xFF;
    }

    private static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            entity.z());
    }

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y,
                                                     int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
    }

    private static void checkType(RoomEntity entity) {
        if (entity.type() != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Pokey type: 0x"
                + Integer.toHexString(entity.type()));
        }
    }

    private static void validateByte(int value, String label) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(label + " must be an unsigned byte: " + value);
        }
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private record Vector(int x, int y) {
    }
}
