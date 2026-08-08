package linksawakening.world;

/** Bank-$06 CrowEntityHandler's proximity, flight, and bounds state machine. */
final class CrowMotion {
    static final int ENTITY_TYPE = 0x7A;
    static final int INITIAL_PHYSICS_FLAGS = 0x02 | 0x10 | 0x40;
    static final int ACTIVE_PHYSICS_FLAGS = 0x02 | 0x10;

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] physicsFlags = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Entity $7A uses EntityInitNoop; the handler owns all of these fields. */
    void initialize(int slot) {
        state[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        physicsFlags[slot] = INITIAL_PHYSICS_FLAGS;
        initialized[slot] = true;
    }

    /**
     * Advances one handler pass. The shared entity loop has already decremented
     * the transition byte before this method is called.
     */
    Update advance(RoomEntity entity, int transitionCountdown,
                   int linkEntityX, int linkEntityY) {
        return advance(entity, transitionCountdown, 0, linkEntityX, linkEntityY);
    }

    Update advance(RoomEntity entity, int transitionCountdown, int frameCounter,
                   int linkEntityX, int linkEntityY) {
        if (entity == null || (entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Crow motion entity");
        }
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        int countdown = transitionCountdown & 0xFF;
        int x = entity.x() & 0xFF;
        int y = entity.y() & 0xFF;
        int z = entity.z() & 0xFF;
        int variant = entity.spriteVariant();
        boolean boomerangSound = false;
        boolean unloaded = false;

        // CrowEntityHandler selects the mirrored pair before dispatching its
        // state. The state handlers that animate flight then overwrite this
        // byte with countdown bit 3, so keep the two operations separate.
        if (direction[slot] == 0) {
            variant = ((variant & 0x01) + 2) & 0x03;
        }

        switch (state[slot]) {
            case 0 -> {
                // The first handler pass lifts the visual origin by four
                // pixels and advances the state before checking proximity.
                y = (y - 4) & 0xFF;
                direction[slot] = signedByte((linkEntityX - x) & 0xFF) < 0 ? 1 : 0;
                int distanceX = signedByte((linkEntityX - x) & 0xFF);
                int distanceY = signedByte((linkEntityY - y) & 0xFF);
                if (distanceX >= -0x18 && distanceX < 0x18
                    && distanceY >= -0x30 && distanceY < 0x30) {
                    physicsFlags[slot] = ACTIVE_PHYSICS_FLAGS;
                    countdown = 0x22;
                    state[slot] = 1;
                }
            }
            case 1 -> {
                if (countdown == 0) {
                    countdown = 0x30;
                    state[slot] = 2;
                } else {
                    speedX[slot] = 0;
                    speedY[slot] = 0;
                    speedZ[slot] = 0x08;
                    z = addSpeedToPosition(z, speedZ[slot], speedZAccumulator, slot);
                    boomerangSound = true;
                    variant = (countdown >>> 3) & 0x01;
                }
            }
            case 2 -> {
                if (countdown == 0) {
                    state[slot] = 3;
                } else {
                    if ((countdown & 0x01) == 0) {
                        Vector vector = vectorTowardsLink(x, y, linkEntityX, linkEntityY,
                            0x20);
                        speedY[slot] = approach(speedY[slot], vector.y());
                        speedX[slot] = approach(speedX[slot], vector.x());
                        direction[slot] = speedX[slot] & 0x80;
                    }
                    x = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
                    y = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
                    boomerangSound = true;
                    variant = (countdown >>> 3) & 0x01;
                }
            }
            case 3 -> {
                if ((frameCounter & 0x03) == 0) {
                    Vector vector = vectorTowardsLink(x, y, linkEntityX, linkEntityY,
                        0x20);
                    speedY[slot] = approach(speedY[slot], negate(vector.y()));
                    speedX[slot] = approach(speedX[slot], negate(vector.x()));
                    direction[slot] = speedX[slot] & 0x80;
                }
                x = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
                y = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
                boomerangSound = true;
                variant = (countdown >>> 3) & 0x01;
                unloaded = ((y - z) & 0xFF) >= 0x88 || x >= 0xA8;
            }
            default -> throw new IllegalStateException("Invalid Crow state: " + state[slot]);
        }

        RoomEntity updated = new RoomEntity(entity.slot(), entity.sourceLoadOrder(),
            entity.type(), x, y, entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), z,
            entity.deathSpriteVariant(), entity.powerRecoilDeath());
        return new Update(updated, countdown, physicsFlags[slot], unloaded, boomerangSound);
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

    int speedY(int slot) {
        return speedY[slot];
    }

    int speedZ(int slot) {
        return speedZ[slot];
    }

    int physicsFlags(int slot) {
        return physicsFlags[slot];
    }

    boolean allowsEnemyCollision(int slot) {
        return state[slot] >= 1;
    }

    void setStateForTest(int slot, int value) {
        ensureInitialized(slot);
        state[slot] = value & 0xFF;
    }

    void setSpeedForTest(int slot, int x, int y, int z) {
        ensureInitialized(slot);
        speedX[slot] = x & 0xFF;
        speedY[slot] = y & 0xFF;
        speedZ[slot] = z & 0xFF;
    }

    private void ensureInitialized(int slot) {
        if (!initialized[slot]) {
            initialize(slot);
        }
    }

    /** Mirrors AddEntitySpeedToPos_06's signed high-nibble fixed point step. */
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

    /** Port of GetVectorTowardsLink with the requested infinity norm. */
    private static Vector vectorTowardsLink(int entityX, int entityY,
                                            int linkX, int linkY, int length) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
        boolean xNegative = distanceX < 0;
        boolean yNegative = distanceY < 0;
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean swapped = absoluteX < absoluteY;
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
        int x = swapped ? result : length;
        int y = swapped ? length : result;
        if (xNegative) {
            x = -x;
        }
        if (yNegative) {
            y = -y;
        }
        return new Vector(x & 0xFF, y & 0xFF);
    }

    /** The Crow handler changes each component by one toward its target byte. */
    private static int approach(int current, int target) {
        int difference = (target - current) & 0xFF;
        if ((difference & 0x80) == 0) {
            return (current + 1) & 0xFF;
        }
        return (current - 1) & 0xFF;
    }

    private static int negate(int value) {
        return (-signedByte(value)) & 0xFF;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    private record Vector(int x, int y) {
    }

    record Update(RoomEntity entity, int transitionCountdown, int physicsFlags,
                  boolean unloaded, boolean boomerangSound) {
    }
}
