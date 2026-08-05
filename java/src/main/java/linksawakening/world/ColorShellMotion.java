package linksawakening.world;

import java.util.List;
import java.util.function.IntSupplier;

/** Bank-$36 Color Shell state machine and fixed-point movement tables. */
final class ColorShellMotion {
    static final int ENTITY_COLOR_SHELL_RED = 0xE9;
    static final int ENTITY_COLOR_SHELL_BLUE = 0xEB;

    private static final int[] SPEED_X_BY_DIRECTION = {0x03, 0xFD, 0x00, 0x00};
    private static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xFD, 0x03};
    private static final int[] CHARGE_DIRECTION_BY_DIRECTION = {0x02, 0x03, 0x01, 0x00};
    private static final int[] PUZZLE_EXPECTED_OBJECTS = {0x5E, 0x59, 0x63};
    private static final int[] PUZZLE_FINAL_OBJECTS = {0x5F, 0x5A, 0x64};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] spriteVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] physicsFlags = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] positionX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] positionY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] positionZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        spriteVariant[slot] = 0;
        physicsFlags[slot] = 0;
        positionX[slot] = 0;
        positionY[slot] = 0;
        positionZ[slot] = 0;
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundCollision backgroundCollision) {
        return advance(entity, frameCounter, linkEntityX, linkEntityY,
            randomByteSupplier, backgroundCollision, 0,
            ColorShellWorld.none(), List.of(entity));
    }

    Update advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundCollision backgroundCollision,
                   int ignoreHitsCountdown) {
        return advance(entity, frameCounter, linkEntityX, linkEntityY,
            randomByteSupplier, backgroundCollision, ignoreHitsCountdown,
            ColorShellWorld.none(), List.of(entity));
    }

    Update advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundCollision backgroundCollision,
                   int ignoreHitsCountdown, ColorShellWorld world,
                   List<RoomEntity> allEntities) {
        if (entity == null || randomByteSupplier == null) {
            throw new IllegalArgumentException("Color Shell motion inputs cannot be null");
        }
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }
        positionX[slot] = entity.x();
        positionY[slot] = entity.y();
        positionZ[slot] = entity.z();
        decrementTransitionCountdown(slot);

        ColorShellWorld effects = world == null ? ColorShellWorld.none() : world;
        List<RoomEntity> entities = allEntities == null ? List.of(entity) : allEntities;
        int frame = frameCounter & 0xFF;
        int dispatchedState = state[slot];
        int nextIgnoreHitsCountdown = ignoreHitsCountdown & 0xFF;
        boolean unloadRequested = false;
        switch (dispatchedState) {
            case 0 -> advanceState0(linkEntityX, linkEntityY, randomByteSupplier, slot);
            case 1 -> advanceState1(frame, linkEntityX, linkEntityY,
                backgroundCollision, slot, entity);
            case 2 -> advanceState2(frame, ignoreHitsCountdown, slot);
            case 3 -> advanceState3(frame, ignoreHitsCountdown,
                backgroundCollision, slot, entity);
            case 4 -> nextIgnoreHitsCountdown = advanceState4(slot);
            case 5 -> nextIgnoreHitsCountdown = advanceState5(entity, ignoreHitsCountdown, slot);
            case 6 -> advanceState6(slot);
            case 7 -> advanceState7(slot);
            case 8 -> advanceState8(entity, effects, slot);
            case 9 -> advanceState9(backgroundCollision, slot, entity);
            case 0x0A -> advanceStateA(backgroundCollision, slot, entity);
            case 0x0B -> advanceStateB(slot);
            case 0x0C -> advanceStateC(entity, effects, entities, slot);
            case 0x0D -> unloadRequested = advanceStateD(entity, effects, slot);
            default -> throw new IllegalStateException("Invalid Color Shell state: "
                + state[slot]);
        }

        RoomEntity updated = new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            positionX[slot], positionY[slot], entity.status(), entity.spriteDefinition(),
            spriteVariant[slot], entity.entityFlipAttribute(), entity.spriteTileOffset(),
            positionZ[slot]);
        return new Update(updated, nextIgnoreHitsCountdown, unloadRequested);
    }

    void clear(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        spriteVariant[slot] = 0;
        physicsFlags[slot] = 0;
        positionX[slot] = 0;
        positionY[slot] = 0;
        positionZ[slot] = 0;
        initialized[slot] = false;
    }

    int state(int slot) {
        return state[slot];
    }

    int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    int direction(int slot) {
        return direction[slot];
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

    int spriteVariant(int slot) {
        return spriteVariant[slot];
    }

    int physicsFlags(int slot) {
        return physicsFlags[slot];
    }

    /** Test/fixture hook shaped like writes to the bank-$36 WRAM tables. */
    void setStateForTest(int slot, int state, int transitionCountdown, int direction,
                         int speedX, int speedY) {
        if (state < 0 || state > 0x0D) {
            throw new IllegalArgumentException("Color Shell state out of range: " + state);
        }
        initialize(slot);
        this.state[slot] = state;
        this.transitionCountdown[slot] = transitionCountdown & 0xFF;
        this.direction[slot] = direction & 0x03;
        this.speedX[slot] = speedX & 0xFF;
        this.speedY[slot] = speedY & 0xFF;
    }

    /** Enters the Color Dungeon's state-$06 branch without discarding WRAM position. */
    void enterState6(RoomEntity entity) {
        int slot = entity.slot();
        initialize(slot);
        positionX[slot] = entity.x();
        positionY[slot] = entity.y();
        positionZ[slot] = entity.z();
        state[slot] = 0x06;
    }

    private void advanceState0(int linkEntityX, int linkEntityY,
                               IntSupplier randomByteSupplier, int slot) {
        if (transitionCountdown[slot] == 0) {
            direction[slot] = ((randomByteSupplier.getAsInt() & 0xFF) & 0x06) >>> 1;
            transitionCountdown[slot] = 0x40;
            state[slot] = 1;
        }
        if (unsignedDistance(linkEntityX, positionX[slot]) < 0x30
            && unsignedDistance(linkEntityY, positionY[slot]) < 0x30) {
            state[slot] = 1;
        }
    }

    private void advanceState1(int frame, int linkEntityX, int linkEntityY,
                               RoomEntityBackgroundCollision backgroundCollision,
                               int slot, RoomEntity entity) {
        int currentDirection = direction[slot] & 0x03;
        speedX[slot] = SPEED_X_BY_DIRECTION[currentDirection];
        speedY[slot] = SPEED_Y_BY_DIRECTION[currentDirection];
        moveWithBackground(entity, backgroundCollision, true, slot);

        if (transitionCountdown[slot] == 0) {
            transitionCountdown[slot] = 0x10;
            state[slot] = 0;
            if (unsignedDistance(linkEntityX, positionX[slot]) < 0x20
                && unsignedDistance(linkEntityY, positionY[slot]) < 0x20) {
                Vector vector = vectorTowardsLink(positionX[slot], positionY[slot],
                    linkEntityX, linkEntityY, 0x0E);
                speedX[slot] = vector.x();
                speedY[slot] = vector.y();
                transitionCountdown[slot] = 0x20;
                state[slot] = 2;
            }
        }
        if ((frame & 0x07) == 0) {
            spriteVariant[slot] = (spriteVariant[slot] + 1) & 0x01;
        }
    }

    private void advanceState2(int frame, int ignoreHitsCountdown, int slot) {
        if (transitionCountdown[slot] == 0) {
            transitionCountdown[slot] = 0x18;
            state[slot] = 3;
        }
        animateJumpVariant(frame, slot);
        if (ignoreHitsCountdown != 0) {
            physicsFlags[slot] |= 0x80;
            state[slot] = 4;
        }
    }

    private void advanceState3(int frame, int ignoreHitsCountdown,
                               RoomEntityBackgroundCollision backgroundCollision,
                               int slot, RoomEntity entity) {
        if (transitionCountdown[slot] == 0) {
            state[slot] = 1;
            speedX[slot] = 0;
            speedY[slot] = 0;
        }
        moveWithBackground(entity, backgroundCollision, true, slot);
        animateJumpVariant(frame, slot);
        if (ignoreHitsCountdown != 0) {
            physicsFlags[slot] |= 0x80;
            state[slot] = 4;
        }
    }

    private int advanceState4(int slot) {
        int nextState = (state[slot] + 1) & 0xFF;
        transitionCountdown[slot] = 0;
        state[slot] = nextState;
        return 0;
    }

    private int advanceState5(RoomEntity entity, int ignoreHitsCountdown, int slot) {
        if (ignoreHitsCountdown != 0) {
            physicsFlags[slot] |= 0x80;
            state[slot] = 4;
            return ignoreHitsCountdown & 0xFF;
        }
        physicsFlags[slot] |= 0x80;
        if (entity.status().value() < EntityStatus.STUNNED.value()) {
            physicsFlags[slot] &= 0x7F;
            state[slot] = 1;
        }
        return 0;
    }

    private void advanceState6(int slot) {
        positionX[slot] = positionX[slot] < 0x50 ? 0x28 : 0x78;
        positionY[slot] = positionY[slot] < 0x48 ? 0x30 : 0x60;
        speedZ[slot] = 0x10;
        state[slot]++;
    }

    private void advanceState7(int slot) {
        speedZ[slot] = (speedZ[slot] - 1) & 0xFF;
        positionZ[slot] = addSpeedToPosition(positionZ[slot], speedZ[slot],
            speedZAccumulator, slot);
        if ((positionZ[slot] & 0x80) != 0) {
            positionZ[slot] = 0;
            speedZ[slot] = 0;
            state[slot]++;
        }
    }

    private void advanceState8(RoomEntity entity, ColorShellWorld world, int slot) {
        int color = entity.type() - ENTITY_COLOR_SHELL_RED;
        if (world.objectAt(entity, -1) == PUZZLE_EXPECTED_OBJECTS[color]) {
            state[slot] = 0x0C;
            physicsFlags[slot] |= 0xF0;
            world.writeObject(entity, 0x67 + color);
            world.playNoise(0x04);
            return;
        }

        world.playJingle(0x1D);
        speedY[slot] = 0;
        speedX[slot] = positionX[slot] < 0x50 ? 0x10 : 0xF0;
        speedZ[slot] = 0x10;
        transitionCountdown[slot] = 0x18;
        state[slot] = 9;
    }

    private void advanceState9(RoomEntityBackgroundCollision backgroundCollision,
                               int slot, RoomEntity entity) {
        if (transitionCountdown[slot] != 0) {
            return;
        }
        if (speedX[slot] != 0) {
            moveWithBackground(entity, backgroundCollision, false, slot);
        }
        speedZ[slot] = (speedZ[slot] - 1) & 0xFF;
        positionZ[slot] = addSpeedToPosition(positionZ[slot], speedZ[slot],
            speedZAccumulator, slot);
        if ((positionZ[slot] & 0x80) != 0) {
            positionZ[slot] = 0;
            speedZ[slot] = 0x08;
            speedX[slot] = arithmeticShiftRight(speedX[slot]);
            state[slot] = 0x0A;
        }
    }

    private void advanceStateA(RoomEntityBackgroundCollision backgroundCollision,
                               int slot, RoomEntity entity) {
        moveWithBackground(entity, backgroundCollision, false, slot);
        speedZ[slot] = (speedZ[slot] - 1) & 0xFF;
        positionZ[slot] = addSpeedToPosition(positionZ[slot], speedZ[slot],
            speedZAccumulator, slot);
        if ((positionZ[slot] & 0x80) != 0) {
            transitionCountdown[slot] = 0x20;
            state[slot]++;
        }
    }

    private void advanceStateB(int slot) {
        if (transitionCountdown[slot] != 0) {
            return;
        }
        transitionCountdown[slot] = 0;
        speedZ[slot] = 0;
        speedX[slot] = 0;
        state[slot] = 1;
        physicsFlags[slot] &= 0x7F;
    }

    private void advanceStateC(RoomEntity entity, ColorShellWorld world,
                               List<RoomEntity> allEntities, int slot) {
        for (RoomEntity other : allEntities) {
            if (other == null || !isColorShellType(other.type()) || !other.loaded()
                || other.status().value() == 0 || state[other.slot()] >= 0x0C) {
                continue;
            }
            return;
        }
        int color = entity.type() - ENTITY_COLOR_SHELL_RED;
        transitionCountdown[slot] = 0x18;
        state[slot] = 0x0D;
        world.writeObject(entity, 0x67 + color);
    }

    private boolean advanceStateD(RoomEntity entity, ColorShellWorld world, int slot) {
        if (transitionCountdown[slot] != 0) {
            return false;
        }
        int color = entity.type() - ENTITY_COLOR_SHELL_RED;
        world.writeObject(entity, PUZZLE_FINAL_OBJECTS[color]);
        world.spawnPoof(positionX[slot], (positionY[slot] - positionZ[slot]) & 0xFF);
        return true;
    }

    private void animateJumpVariant(int frame, int slot) {
        if ((frame & 0x01) != 0) {
            return;
        }
        spriteVariant[slot] = (spriteVariant[slot] + 1) & 0x01;
        if (spriteVariant[slot] == 0) {
            direction[slot] = CHARGE_DIRECTION_BY_DIRECTION[direction[slot] & 0x03];
        }
    }

    private void moveWithBackground(RoomEntity entity,
                                    RoomEntityBackgroundCollision backgroundCollision,
                                    boolean clampPosition, int slot) {
        int x = addSpeedToPosition(positionX[slot], speedX[slot], speedXAccumulator, slot);
        if (x != positionX[slot] && backgroundCollision != null
            && backgroundCollision.blocks(entity, directionForX(speedX[slot]), x, positionY[slot])) {
            x = positionX[slot];
        }
        int y = addSpeedToPosition(positionY[slot], speedY[slot], speedYAccumulator, slot);
        if (y != positionY[slot] && backgroundCollision != null
            && backgroundCollision.blocks(entity, directionForY(speedY[slot]), x, y)) {
            y = positionY[slot];
        }
        positionX[slot] = clampPosition ? clamp(x, 0x16, 0x89) : x & 0xFF;
        positionY[slot] = clampPosition ? clamp(y, 0x1E, 0x72) : y & 0xFF;
    }

    static boolean isColorShellType(int type) {
        return type >= ENTITY_COLOR_SHELL_RED && type <= ENTITY_COLOR_SHELL_BLUE;
    }

    private void decrementTransitionCountdown(int slot) {
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }
    }

    private static int directionForX(int speed) {
        return signedByte(speed) < 0 ? 1 : 0;
    }

    private static int directionForY(int speed) {
        return signedByte(speed) < 0 ? 2 : 3;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value & 0xFF));
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

    private static int arithmeticShiftRight(int value) {
        return (signedByte(value) >> 1) & 0xFF;
    }

    private static Vector vectorTowardsLink(int entityX, int entityY,
                                            int linkEntityX, int linkEntityY, int length) {
        int distanceX = signedByte((linkEntityX - entityX) & 0xFF);
        int distanceY = signedByte((linkEntityY - entityY) & 0xFF);
        boolean yIsLargerAxis = Math.abs(distanceY) > Math.abs(distanceX);
        int smallerDistance = Math.min(Math.abs(distanceX), Math.abs(distanceY));
        int largerDistance = Math.max(Math.abs(distanceX), Math.abs(distanceY));
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (largerDistance != 0 && sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum & 0xFF;
        }
        int x = yIsLargerAxis ? result : length;
        int y = yIsLargerAxis ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new Vector(x & 0xFF, y & 0xFF);
    }

    private static int unsignedDistance(int first, int second) {
        int difference = (first - second) & 0xFF;
        return difference < 0x80 ? difference : 0x100 - difference;
    }

    private static int signedByte(int value) {
        int byteValue = value & 0xFF;
        return byteValue < 0x80 ? byteValue : byteValue - 0x100;
    }

    record Update(RoomEntity entity, int nextIgnoreHitsCountdown, boolean unloadRequested) {
        Update(RoomEntity entity) {
            this(entity, 0, false);
        }
    }

    private record Vector(int x, int y) {
    }
}
