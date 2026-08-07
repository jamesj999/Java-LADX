package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$07 SnakeEntityHandler states 0, 1, and 2. */
final class SnakeMotion {
    static final int ENTITY_SNAKE = 0xA1;

    private static final int[] SPEED_X_BY_DIRECTION = {0x08, 0xF8, 0x00, 0x00};
    private static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xF8, 0x08};
    private static final int[] SPRITE_DIRECTION_OFFSET = {0x02, 0x00, -1, -1};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] animationVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot) {
        state[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        privateCountdown1[slot] = 0x30;
        animationVariant[slot] = 0;
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                   IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
                   int transitionCountdown) {
        if ((entity.type() & 0xFF) != ENTITY_SNAKE) {
            throw new IllegalArgumentException("Unsupported Snake motion entity type: 0x"
                + Integer.toHexString(entity.type()));
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Snake random-byte supplier cannot be null");
        }

        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }
        if (privateCountdown1[slot] > 0) {
            privateCountdown1[slot]--;
        }

        int countdown = transitionCountdown & 0xFF;
        int x = entity.x() & 0xFF;
        int y = entity.y() & 0xFF;
        // RenderActiveEntitySpritesPair runs before movement and state
        // dispatch. The handler adds the persistent direction offset to the
        // previously selected base animation variant, then writes the next
        // base variant only after the current frame has been drawn.
        int renderedVariant = (animationVariant[slot] + direction[slot]) & 0xFF;

        // SnakeEntityHandler updates its position and applies background
        // interaction before dispatching the state table. A collision then
        // resets the state, transition byte, and private countdown; state 0
        // clears the speed in the same handler pass.
        Position moved = move(entity, x, y, backgroundCollision);
        x = moved.x();
        y = moved.y();
        if (moved.collided()) {
            state[slot] = 0;
            countdown = 0x08;
            privateCountdown1[slot] = 0x20;
        }

        switch (state[slot]) {
            case 0 -> {
                if (countdown == 0) {
                    state[slot] = 1;
                    int random = randomByteSupplier.getAsInt() & 0xFF;
                    countdown = 0x30 + (random & 0x1F);
                    setSpeed(slot, random & 0x03);
                    // The ROM returns immediately from this branch, leaving
                    // the previous base animation variant in place.
                } else {
                    clearSpeed(slot);
                    countdown = startDashIfNeeded(slot, x, y, linkEntityX, linkEntityY,
                        countdown);
                    setAnimationVariant(slot, crawlingAnimation(frameCounter));
                }
            }
            case 1 -> {
                if (countdown == 0) {
                    countdown = 0x18;
                    state[slot] = 2;
                }
                countdown = startDashIfNeeded(slot, x, y, linkEntityX, linkEntityY,
                    countdown);
                setAnimationVariant(slot, crawlingAnimation(frameCounter));
            }
            case 2 -> {
                if (countdown == 0) {
                    countdown = 0x20;
                    state[slot] = 0;
                    privateCountdown1[slot] = 0x40;
                }
                setAnimationVariant(slot, dashingAnimation(frameCounter));
            }
            default -> throw new IllegalStateException("Invalid Snake state: " + state[slot]);
        }

        RoomEntity updated = new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x, y, entity.status(), entity.spriteDefinition(), renderedVariant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
        return new Update(updated, countdown);
    }

    void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    int state(int slot) {
        return state[slot];
    }

    int privateCountdown1(int slot) {
        return privateCountdown1[slot];
    }

    int speedX(int slot) {
        return speedX[slot];
    }

    int speedY(int slot) {
        return speedY[slot];
    }

    private int startDashIfNeeded(int slot, int entityX, int entityY,
                                  int linkEntityX, int linkEntityY, int countdown) {
        if (privateCountdown1[slot] != 0) {
            return countdown;
        }

        int distanceX = signedByte((linkEntityX - entityX) & 0xFF);
        if (withinDashWindow(distanceX)) {
            dash(slot, directionForY(linkEntityY - entityY));
            return 0x30;
        }

        int distanceY = signedByte((linkEntityY - entityY) & 0xFF);
        if (withinDashWindow(distanceY)) {
            dash(slot, directionForX(linkEntityX - entityX));
            return 0x30;
        }
        return countdown;
    }

    private void dash(int slot, int directionIndex) {
        setSpeed(slot, directionIndex);
        speedX[slot] = (speedX[slot] << 1) & 0xFF;
        speedY[slot] = (speedY[slot] << 1) & 0xFF;
        state[slot] = 2;
        // SnakeStartDashIfNeeded overwrites the state-1 $18 countdown with
        // the state-2 dash window.
    }

    private Position move(RoomEntity entity, int x, int y,
                          RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        boolean collided = false;
        if (backgroundCollision != null && nextX != x
            && backgroundCollision.blocks(entity, directionForX(speedX[slot]), nextX, y)) {
            nextX = x;
            collided = true;
        }
        if (backgroundCollision != null && nextY != y
            && backgroundCollision.blocks(entity, directionForY(speedY[slot]), nextX, nextY)) {
            nextY = y;
            collided = true;
        }
        return new Position(nextX, nextY, collided);
    }

    private void setSpeed(int slot, int directionIndex) {
        int index = directionIndex & 0x03;
        speedX[slot] = SPEED_X_BY_DIRECTION[index];
        speedY[slot] = SPEED_Y_BY_DIRECTION[index];
        int spriteDirection = SPRITE_DIRECTION_OFFSET[index];
        if (spriteDirection >= 0) {
            direction[slot] = spriteDirection;
        }
    }

    private void clearSpeed(int slot) {
        speedX[slot] = 0;
        speedY[slot] = 0;
    }

    private void setAnimationVariant(int slot, int variant) {
        animationVariant[slot] = variant & 0x01;
    }

    private static boolean withinDashWindow(int distance) {
        return ((distance + 0x08) & 0xFF) < 0x10;
    }

    private static int directionForX(int distance) {
        return signedByte(distance) < 0 ? 1 : 0;
    }

    private static int directionForY(int distance) {
        return signedByte(distance) < 0 ? 2 : 3;
    }

    private static int crawlingAnimation(int frameCounter) {
        return (frameCounter >>> 3) & 0x01;
    }

    private static int dashingAnimation(int frameCounter) {
        return (frameCounter >>> 2) & 0x01;
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator,
                                          int slot) {
        speed &= 0xFF;
        if (speed == 0) {
            return position & 0xFF;
        }
        int fractionalSum = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = fractionalSum & 0xFF;
        int signedSpeed = speed < 0x80 ? speed : speed - 0x100;
        int delta = signedSpeed >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    private record Position(int x, int y, boolean collided) {
    }

    record Update(RoomEntity entity, int transitionCountdown) {
    }
}
