package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$15 StalfosEvasiveEntityHandler's normal and airborne states. */
final class StalfosEvasiveMotion {
    private static final int[] RANDOM_DIRECTION_SPEEDS = {0x00, 0x06, 0xFA, 0xFA, 0x06};

    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record Update(RoomEntity entity, int transitionCountdown,
                  boolean unloadRequested, boolean swordPokeRequested,
                  CloneRequest cloneRequest) {
    }

    record CloneRequest(int sourceSlot, int x, int y, int z, int speedX, int speedY) {
    }

    void initialize(int slot) {
        reset(slot);
        // EntityInitHandlersTable.$1E points to IncrementEntityState.
        state[slot] = 1;
        initialized[slot] = true;
    }

    private void initializeForActiveEntity(int slot) {
        reset(slot);
        initialized[slot] = true;
    }

    private void reset(int slot) {
        privateState1[slot] = 0;
        privateCountdown1[slot] = 0;
        state[slot] = 0;
        inertia[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
    }

    Update advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                   boolean actionButtonsHeld, int transitionCountdown,
                   int ignoreHitsCountdown, IntSupplier randomByteSupplier,
                   RoomEntityBackgroundCollision backgroundCollision, int mapId) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            // A Gibdo burn conversion calls ConfigureNewEntity.attributes and
            // returns ACTIVE without running EntityInitHandlersTable.$1E.
            initializeForActiveEntity(slot);
        }
        boolean cloneBranchReady = false;
        if (privateCountdown1[slot] > 0) {
            privateCountdown1[slot]--;
            cloneBranchReady = privateCountdown1[slot] == 1;
        }

        int frame = frameCounter & 0xFF;
        if (privateState1[slot] != 0) {
            return advanceFleeing(entity, frame, transitionCountdown, ignoreHitsCountdown,
                backgroundCollision);
        }

        CloneRequest cloneRequest = null;
        if (cloneBranchReady) {
            // The source returns immediately when hMapId is below
            // MAP_ANGLERS_TUNNEL, before its inertia dispatch or movement.
            if (mapId < 0x03) {
                int variant = (frame >>> 3) & 0x01;
                return new Update(withPositionAndVariant(
                    entity, entity.x(), entity.y(), entity.z(), variant),
                    transitionCountdown, false, false, null);
            }
            Vector vector = vectorTowardsLink(entity.x(), entity.y(), entity.z(),
                linkEntityX, linkEntityY, 0x18);
            cloneRequest = new CloneRequest(slot, entity.x(), entity.y(), entity.z(),
                vector.x(), vector.y());
        }
        if (inertia[slot] != 0) {
            return advanceAirborne(entity, transitionCountdown, backgroundCollision, cloneRequest);
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        MoveResult movement = applyBackground(entity, x, y, backgroundCollision);
        x = movement.x();
        y = movement.y();
        if (movement.xBlocked()) {
            speedX[slot] ^= 0xF0;
        }
        if (movement.yBlocked()) {
            speedY[slot] ^= 0xF0;
        }

        int distanceX = signedByte(linkEntityX - entity.x());
        int distanceY = signedByte(linkEntityY - entity.y());
        if (actionButtonsHeld && distanceInJumpWindow(distanceX)
            && distanceInJumpWindow(distanceY)) {
            transitionCountdown = 0x08;
            privateCountdown1[slot] = 0;
            inertia[slot] = 1;
            speedZ[slot] = 0x15;
            Vector vector = vectorTowardsLink(entity.x(), entity.y(), entity.z(),
                linkEntityX, linkEntityY, 0x12);
            speedX[slot] = (-vector.x()) & 0xFF;
            speedY[slot] = (-vector.y()) & 0xFF;
            return new Update(withPositionAndVariant(entity, x, y, entity.z(), 0x02),
                transitionCountdown, false, false, cloneRequest);
        }

        // jr_015_4FCE: the initialization state or random low six bits forces
        // a new choice; otherwise the current fixed-point speed is retained.
        if (state[slot] != 0 || (randomByteSupplier.getAsInt() & 0x2F) == 0) {
            chooseRandomDirection(slot, randomByteSupplier);
        }
        int variant = (frame >>> 3) & 0x01;
        return new Update(withPositionAndVariant(entity, x, y, entity.z(), variant),
            transitionCountdown, false, false, cloneRequest);
    }

    private Update advanceAirborne(RoomEntity entity, int transitionCountdown,
                                    RoomEntityBackgroundCollision backgroundCollision,
                                    CloneRequest cloneRequest) {
        int slot = entity.slot();
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        MoveResult movement = applyBackground(entity, x, y, backgroundCollision);
        x = movement.x();
        y = movement.y();

        int z = addSpeedToPosition(entity.z(), speedZ[slot], speedZAccumulator, slot);
        speedZ[slot] = (speedZ[slot] - 1) & 0xFF;
        if ((z & 0x80) != 0) {
            z = 0;
            speedZ[slot] = 0;
            speedX[slot] = 0x08;
            speedY[slot] = 0x08;
            inertia[slot] = 0;
            privateCountdown1[slot] = 0x10;
        }
        return new Update(withPositionAndVariant(entity, x, y, z, 0x02),
            transitionCountdown, false, false, cloneRequest);
    }

    private Update advanceFleeing(RoomEntity entity, int frame, int transitionCountdown,
                                   int ignoreHitsCountdown,
                                   RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        MoveResult movement = applyBackground(entity, x, y, backgroundCollision);
        RoomEntity updated = withPositionAndVariant(entity, movement.x(), movement.y(),
            entity.z(), (frame >>> 3) & 0x01);
        if (movement.blocked() || ignoreHitsCountdown != 0) {
            return new Update(updated, transitionCountdown, true, true, null);
        }
        if (movement.x() >= 0xA8 || ((movement.y() - updated.z()) & 0xFF) >= 0x84) {
            return new Update(updated, transitionCountdown, true, false, null);
        }
        return new Update(updated, transitionCountdown, false, false, null);
    }

    void clear(int slot) {
        privateState1[slot] = 0;
        privateCountdown1[slot] = 0;
        state[slot] = 0;
        inertia[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        initialized[slot] = false;
    }

    int privateState1(int slot) {
        return privateState1[slot];
    }

    int privateCountdown1(int slot) {
        return privateCountdown1[slot];
    }

    int state(int slot) {
        return state[slot];
    }

    int inertia(int slot) {
        return inertia[slot];
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

    void setPrivateCountdown1ForTest(int slot, int value) {
        initialized[slot] = true;
        privateCountdown1[slot] = value & 0xFF;
    }

    void setFleeingForTest(int slot, int newSpeedX, int newSpeedY) {
        reset(slot);
        initialized[slot] = true;
        privateState1[slot] = 1;
        speedX[slot] = newSpeedX & 0xFF;
        speedY[slot] = newSpeedY & 0xFF;
    }

    void initializeClone(int slot, int newSpeedX, int newSpeedY) {
        reset(slot);
        initialized[slot] = true;
        privateState1[slot] = 1;
        speedX[slot] = newSpeedX & 0xFF;
        speedY[slot] = newSpeedY & 0xFF;
    }

    boolean isAirborne(int slot) {
        return inertia[slot] != 0;
    }

    private void chooseRandomDirection(int slot, IntSupplier randomByteSupplier) {
        speedY[slot] = 0;
        speedX[slot] = RANDOM_DIRECTION_SPEEDS[randomByteSupplier.getAsInt() & 0x03];
        if (speedX[slot] == 0) {
            speedY[slot] = RANDOM_DIRECTION_SPEEDS[3 + (randomByteSupplier.getAsInt() & 0x01)];
        }
        // The disassembly writes zero to the entity state after choosing.
        state[slot] = 0;
    }

    private static MoveResult applyBackground(RoomEntity entity, int x, int y,
                                               RoomEntityBackgroundCollision backgroundCollision) {
        boolean xBlocked = false;
        boolean yBlocked = false;
        if (backgroundCollision != null) {
            if (x != entity.x() && backgroundCollision.blocks(entity,
                directionForX(x - entity.x()), x, entity.y())) {
                x = entity.x();
                xBlocked = true;
            }
            if (y != entity.y() && backgroundCollision.blocks(entity,
                directionForY(y - entity.y()), x, y)) {
                y = entity.y();
                yBlocked = true;
            }
        }
        return new MoveResult(x, y, xBlocked, yBlocked);
    }

    private static Vector vectorTowardsLink(int entityX, int entityY, int entityZ,
                                            int linkX, int linkY, int length) {
        int distanceY = signedByte(linkY - entityY + entityZ);
        int distanceX = signedByte(linkX - entityX);
        int absoluteY = Math.abs(distanceY);
        int absoluteX = Math.abs(distanceX);
        boolean swapped = absoluteX < absoluteY;
        int smaller = Math.min(absoluteX, absoluteY);
        int larger = Math.max(absoluteX, absoluteY);
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smaller;
            if (sum >= larger) {
                sum -= larger;
                result++;
            }
            remainder = sum & 0xFF;
        }
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

    private static boolean distanceInJumpWindow(int distance) {
        return distance >= -0x24 && distance < 0x24;
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator,
                                          int slot) {
        speed &= 0xFF;
        if (speed == 0) {
            return position & 0xFF;
        }
        int fractionalSum = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = fractionalSum & 0xFF;
        int signedSpeed = signedByte(speed);
        int delta = signedSpeed >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y, int z,
                                                       int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), variant, entity.entityFlipAttribute(),
            entity.spriteTileOffset(), z);
    }

    private static int directionForX(int delta) {
        return delta < 0 ? 1 : 0;
    }

    private static int directionForY(int delta) {
        return delta < 0 ? 2 : 3;
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private record MoveResult(int x, int y, boolean xBlocked, boolean yBlocked) {
        boolean blocked() {
            return xBlocked || yBlocked;
        }
    }

    private record Vector(int x, int y) {
    }
}
