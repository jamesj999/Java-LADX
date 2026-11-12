package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$07 BushCrawlerEntityHandler movement and lift trigger. */
public final class BushCrawlerMotion {
    static final int ENTITY_BUSH_CRAWLER = 0xBB;
    static final int ENTITY_SWORD = 0x01;
    static final int ENTITY_POWER_BRACELET = 0x03;

    static final int[] SPEED_X_BY_DIRECTION = {0x10, 0xF0, 0x00, 0x00};
    static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xF0, 0x10};
    // Data_007_41E6 is immediately followed by Data_007_41E8 in bank $07.
    // The handler intentionally indexes across that label boundary for Y.
    static final int[] SPECIAL_SPEED_X = {0x00, 0x0C, 0x10, 0x0C, 0x00, 0xF4, 0xF0, 0xF4};
    static final int[] SPECIAL_SPEED_Y = {0xF0, 0xF4, 0x00, 0x0C, 0x10, 0x0C, 0x00, 0xF4};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState2 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] presentationVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record Update(
            RoomEntity entity,
            int state,
            int transitionCountdown,
            int privateState1,
            int privateState4,
            int visualYOffset,
            boolean liftRequested,
            boolean specialState,
            boolean crawlOverlayRendered) {}

    public void initialize(int slot) {
        clear(slot);
        initialized[slot] = true;
    }

    public void initializeSpecial(int slot) {
        initialize(slot);
        privateState1[slot] = 2;
    }

    public Update advance(BushCrawlerFrame inputs) {
        return new BushCrawlerStep(this, inputs).run();
    }

    public void clear(int slot) {
        state[slot] = 0;
        privateState1[slot] = 0;
        privateState2[slot] = 0;
        privateState4[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        presentationVariant[slot] = 0;
        initialized[slot] = false;
    }

    public int state(int slot) {
        return state[slot];
    }

    public int privateState1(int slot) {
        return privateState1[slot];
    }

    public int privateState4(int slot) {
        return privateState4[slot];
    }

    public int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    public int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    static boolean startsCrawl(int x, int y, int linkX, int linkY) {
        int distanceY = signedByte(linkY - y);
        if (!inSignedWindow(distanceY, 8)) {
            return false;
        }
        int distanceX = signedByte(linkX - x);
        if (!inSignedWindow(distanceX, 8)) {
            return false;
        }
        return directionToLink(x, y, linkX, linkY) != 2;
    }

    static boolean powerBraceletHeld(
            boolean actionButtonAHeld, boolean actionButtonBHeld, int linkItemA, int linkItemB) {
        if ((linkItemB & 0xFF) == ENTITY_POWER_BRACELET) {
            return actionButtonBHeld;
        }
        return (linkItemA & 0xFF) == ENTITY_POWER_BRACELET && actionButtonAHeld;
    }

    Position move(RoomEntity entity, int x, int y, RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        boolean collided = false;
        if (backgroundCollision != null
                && nextX != x
                && backgroundCollision.blocks(entity, horizontalDirection(speedX[slot]), nextX, y)) {
            nextX = x;
            collided = true;
        }
        if (backgroundCollision != null
                && nextY != y
                && backgroundCollision.blocks(entity, verticalDirection(speedY[slot]), nextX, nextY)) {
            nextY = y;
            collided = true;
        }
        return new Position(nextX, nextY, collided);
    }

    static int directionToLink(int entityX, int entityY, int linkX, int linkY) {
        int distanceX = signedByte(linkX - entityX);
        int distanceY = signedByte(linkY - entityY);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    private static boolean inSignedWindow(int distance, int halfOpen) {
        return distance >= -halfOpen && distance < halfOpen;
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
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

    private static int horizontalDirection(int speed) {
        return signedByte(speed) < 0 ? 1 : 0;
    }

    private static int verticalDirection(int speed) {
        return signedByte(speed) < 0 ? 2 : 3;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    record Position(int x, int y, boolean collided) {}
}
