package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$15 FishEntityHandler's hidden swim and jump loop. */
final class FishMotion {
    static final int ENTITY_TYPE = 0xCC;

    static final int INITIAL_PHYSICS_FLAGS =
        0x02 | 0x10 | 0x40;
    static final int JUMP_PHYSICS_FLAGS = 0x02 | 0x10;

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] physicsFlags = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Entity $CC uses EntityInitNoop; the handler seeds its own state. */
    void initialize(int slot) {
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
    Update advance(RoomEntity entity, int transitionCountdown,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundCollision backgroundCollision) {
        if ((entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Fish motion entity type: 0x"
                + Integer.toHexString(entity.type()));
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Fish random-byte supplier cannot be null");
        }

        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        int countdown = transitionCountdown & 0xFF;
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        boolean horizontalCollision = false;
        if (speedX[slot] != 0 && backgroundCollision != null
            && backgroundCollision.blocks(entity, horizontalDirection(speedX[slot]),
                x, entity.y())) {
            x = entity.x() & 0xFF;
            horizontalCollision = true;
        }

        int z = entity.z() & 0xFF;
        int variant = entity.spriteVariant();
        boolean waterSplash = false;
        switch (state[slot]) {
            case 0 -> {
                speedX[slot] = 0x08;
                countdown = 0x40 + (randomByteSupplier.getAsInt() & 0x7F);
                state[slot] = 1;
            }
            case 1 -> {
                if (countdown != 0) {
                    if (horizontalCollision) {
                        speedX[slot] = negate(speedX[slot]);
                    }
                    variant = speedX[slot] < 0x80 ? 0x06 : 0x05;
                } else {
                    physicsFlags[slot] = JUMP_PHYSICS_FLAGS;
                    speedZ[slot] = 0x18;
                    state[slot] = 2;
                    speedX[slot] = (speedX[slot] << 1) & 0xFF;
                    waterSplash = true;
                }
            }
            case 2 -> {
                z = addSpeedToPosition(z, speedZ[slot], speedZAccumulator, slot);
                speedZ[slot] = (speedZ[slot] - 1) & 0xFF;
                if ((z & 0x80) != 0) {
                    z = 0;
                    waterSplash = true;
                    countdown = 0x50 + (randomByteSupplier.getAsInt() & 0x7F);
                    speedX[slot] = arithmeticShiftRight(speedX[slot]);
                    physicsFlags[slot] = INITIAL_PHYSICS_FLAGS;
                    state[slot] = 1;
                } else {
                    variant = signedByte(speedX[slot]) < 0 ? 0x01 : 0x03;
                    if (signedByte(speedZ[slot]) < 0) {
                        variant++;
                    }
                }
            }
            default -> throw new IllegalStateException("Invalid Fish state: " + state[slot]);
        }

        RoomEntity updated = new RoomEntity(entity.slot(), entity.sourceLoadOrder(),
            entity.type(), x, entity.y(), entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), z,
            entity.deathSpriteVariant(), entity.powerRecoilDeath());
        return new Update(updated, countdown, physicsFlags[slot], waterSplash);
    }

    void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    int state(int slot) {
        return state[slot];
    }

    int speedX(int slot) {
        return speedX[slot];
    }

    int speedZ(int slot) {
        return speedZ[slot];
    }

    int physicsFlags(int slot) {
        return physicsFlags[slot];
    }

    boolean allowsEnemyCollision(int slot) {
        return state[slot] == 2;
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

    private static int horizontalDirection(int speed) {
        return signedByte(speed) < 0
            ? EntityBackgroundCollisionResult.LEFT
            : EntityBackgroundCollisionResult.RIGHT;
    }

    private static int negate(int speed) {
        return (-signedByte(speed)) & 0xFF;
    }

    private static int arithmeticShiftRight(int speed) {
        return (signedByte(speed) >> 1) & 0xFF;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    record Update(RoomEntity entity, int transitionCountdown, int physicsFlags,
                  boolean waterSplash) {
    }
}
