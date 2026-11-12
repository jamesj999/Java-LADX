package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$07 SnakeEntityHandler states 0, 1, and 2. */
public final class SnakeMotion {
    static final int ENTITY_SNAKE = 0xA1;

    private static final int[] SPEED_X_BY_DIRECTION = {0x08, 0xF8, 0x00, 0x00};
    private static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xF8, 0x08};
    private static final int[] SPRITE_DIRECTION_OFFSET = {0x02, 0x00, -1, -1};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] animationVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public void initialize(int slot) {
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

    public Update advance(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            int transitionCountdown) {
        return snakeFrameStep.run(
                this,
                entity,
                frameCounter,
                linkEntityX,
                linkEntityY,
                randomByteSupplier,
                backgroundCollision,
                transitionCountdown);
    }

    public void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    public int state(int slot) {
        return state[slot];
    }

    public int privateCountdown1(int slot) {
        return privateCountdown1[slot];
    }

    public int speedX(int slot) {
        return speedX[slot];
    }

    public int speedY(int slot) {
        return speedY[slot];
    }

    int startDashIfNeeded(int slot, int entityX, int entityY, int linkEntityX, int linkEntityY, int countdown) {
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

    Position move(RoomEntity entity, int x, int y, RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        boolean collided = false;
        if (backgroundCollision != null
                && nextX != x
                && backgroundCollision.blocks(entity, directionForX(speedX[slot]), nextX, y)) {
            nextX = x;
            collided = true;
        }
        if (backgroundCollision != null
                && nextY != y
                && backgroundCollision.blocks(entity, directionForY(speedY[slot]), nextX, nextY)) {
            nextY = y;
            collided = true;
        }
        return new Position(nextX, nextY, collided);
    }

    void setSpeed(int slot, int directionIndex) {
        int index = directionIndex & 0x03;
        speedX[slot] = SPEED_X_BY_DIRECTION[index];
        speedY[slot] = SPEED_Y_BY_DIRECTION[index];
        int spriteDirection = SPRITE_DIRECTION_OFFSET[index];
        if (spriteDirection >= 0) {
            direction[slot] = spriteDirection;
        }
    }

    void clearSpeed(int slot) {
        speedX[slot] = 0;
        speedY[slot] = 0;
    }

    void setAnimationVariant(int slot, int variant) {
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

    static int crawlingAnimation(int frameCounter) {
        return (frameCounter >>> 3) & 0x01;
    }

    static int dashingAnimation(int frameCounter) {
        return (frameCounter >>> 2) & 0x01;
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
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

    record Position(int x, int y, boolean collided) {}

    public record Update(RoomEntity entity, int transitionCountdown) {}

    final SnakeFrameStep snakeFrameStep = new SnakeFrameStep();
}
