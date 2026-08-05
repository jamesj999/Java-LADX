package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$36 Color Shell states 0-$03 and their fixed-point movement helpers. */
final class ColorShellMotion {
    static final int ENTITY_COLOR_SHELL_RED = 0xE9;
    static final int ENTITY_COLOR_SHELL_BLUE = 0xEB;

    private static final int[] SPEED_X_BY_DIRECTION = {0x03, 0xFD, 0x00, 0x00};
    private static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xFD, 0x03};
    private static final int[] CHARGE_DIRECTION_BY_DIRECTION = {0x02, 0x03, 0x01, 0x00};

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
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundCollision backgroundCollision) {
        return advance(entity, frameCounter, linkEntityX, linkEntityY,
            randomByteSupplier, backgroundCollision, 0);
    }

    Update advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundCollision backgroundCollision,
                   int ignoreHitsCountdown) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }
        decrementTransitionCountdown(slot);

        int frame = frameCounter & 0xFF;
        int dispatchedState = state[slot];
        switch (dispatchedState) {
            case 0 -> advanceState0(entity, linkEntityX, linkEntityY, randomByteSupplier);
            case 1 -> advanceState1(entity, frame, linkEntityX, linkEntityY,
                backgroundCollision);
            case 2 -> advanceState2(entity, frame, ignoreHitsCountdown,
                linkEntityX, linkEntityY, backgroundCollision);
            case 3 -> advanceState3(entity, frame, ignoreHitsCountdown,
                linkEntityX, linkEntityY, backgroundCollision);
            default -> {
                // Later bank-$36 states are added in the puzzle/effects
                // increment. Keeping the state stable here prevents a
                // partially initialized shell from falling through to a
                // fabricated generic movement path.
            }
        }

        int x = entity.x();
        int y = entity.y();
        int z = entity.z();
        if (dispatchedState == 1 || dispatchedState == 3) {
            x = movedX[slot];
            y = movedY[slot];
        }
        return new Update(new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x, y, entity.status(), entity.spriteDefinition(), spriteVariant[slot],
            entity.entityFlipAttribute(), entity.spriteTileOffset(), z));
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

    private void advanceState0(RoomEntity entity, int linkEntityX, int linkEntityY,
                               IntSupplier randomByteSupplier) {
        int slot = entity.slot();
        if (transitionCountdown[slot] == 0) {
            direction[slot] = ((randomByteSupplier.getAsInt() & 0xFF) & 0x06) >>> 1;
            transitionCountdown[slot] = 0x40;
            state[slot] = 1;
        }

        if (unsignedDistance(linkEntityX, entity.x()) < 0x30
            && unsignedDistance(linkEntityY, entity.y()) < 0x30) {
            state[slot] = 1;
        }
    }

    private void decrementTransitionCountdown(int slot) {
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }
    }

    private void advanceState1(RoomEntity entity, int frame, int linkEntityX, int linkEntityY,
                                RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        int currentDirection = direction[slot] & 0x03;
        speedX[slot] = SPEED_X_BY_DIRECTION[currentDirection];
        speedY[slot] = SPEED_Y_BY_DIRECTION[currentDirection];

        int[] moved = moveAndClamp(entity, backgroundCollision);
        int x = moved[0];
        int y = moved[1];

        if (transitionCountdown[slot] == 0) {
            transitionCountdown[slot] = 0x10;
            state[slot] = 0;
            if (unsignedDistance(linkEntityX, x) < 0x20
                && unsignedDistance(linkEntityY, y) < 0x20) {
                Vector vector = vectorTowardsLink(x, y, linkEntityX, linkEntityY, 0x0E);
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

    private void advanceState2(RoomEntity entity, int frame, int ignoreHitsCountdown,
                                int linkEntityX, int linkEntityY,
                                RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
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

    private void advanceState3(RoomEntity entity, int frame, int ignoreHitsCountdown,
                                int linkEntityX, int linkEntityY,
                                RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (transitionCountdown[slot] == 0) {
            state[slot] = 1;
            speedX[slot] = 0;
            speedY[slot] = 0;
        }
        moveAndClamp(entity, backgroundCollision);
        animateJumpVariant(frame, slot);
        if (ignoreHitsCountdown != 0) {
            physicsFlags[slot] |= 0x80;
            state[slot] = 4;
        }
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

    private int[] moveAndClamp(RoomEntity entity,
                               RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        if (x != entity.x() && backgroundCollision != null
            && backgroundCollision.blocks(entity, directionForX(speedX[slot]), x, entity.y())) {
            x = entity.x();
        }
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        if (y != entity.y() && backgroundCollision != null
            && backgroundCollision.blocks(entity, directionForY(speedY[slot]), x, y)) {
            y = entity.y();
        }

        // The source writes the clamped values to the entity tables. The
        // current helper returns coordinates, so the caller must retain them
        // in the update result.
        movedX[slot] = clamp(x, 0x16, 0x89);
        movedY[slot] = clamp(y, 0x1E, 0x72);
        return new int[] {movedX[slot], movedY[slot]};
    }

    private final int[] movedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] movedY = new int[EntityRoomLoader.MAX_ENTITIES];

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

    record Update(RoomEntity entity) {
    }

    private record Vector(int x, int y) {
    }
}
