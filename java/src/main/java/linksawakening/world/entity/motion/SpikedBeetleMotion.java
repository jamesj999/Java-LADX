package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$07 SpikedBeetleEntityHandler's rest, walk, dash, and flip states. */
public final class SpikedBeetleMotion {
    public static final int ENTITY_TYPE = 0x2C;
    static final int NORMAL_OPTIONS1 = 0x48;
    static final int FLIPPED_OPTIONS1 = 0x08;
    static final int NORMAL_HITBOX_FLAGS = 0x80;
    static final int FLIPPED_HITBOX_FLAGS = 0x00;

    private static final int[] WALKING_X_SPEEDS = {0x06, 0xFA, 0x00, 0x00};
    private static final int[] WALKING_Y_SPEEDS = {0x00, 0x00, 0xFA, 0x06};
    static final int[] DASHING_X_SPEEDS = {0x18, 0xE8, 0x00, 0x00};
    static final int[] DASHING_Y_SPEEDS = {0x00, 0x00, 0xE8, 0x18};
    private static final int[] FLIP_X_SPEEDS = {0x10, 0xF0, 0x00, 0x00};
    private static final int[] FLIP_Y_SPEEDS = {0x00, 0x00, 0xF0, 0x10};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] animationVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public void initialize(int slot) {
        state[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
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
            int transitionCountdown,
            int ignoreHitsCountdown) {
        return advance(new SpikedBeetleFrame(
                entity,
                frameCounter,
                linkEntityX,
                linkEntityY,
                randomByteSupplier,
                backgroundCollision,
                transitionCountdown,
                ignoreHitsCountdown));
    }

    public linksawakening.world.entity.motion.SpikedBeetleMotion.Update advance(SpikedBeetleFrame inputs) {
        return new SpikedBeetleFrameStep(this, inputs).run();
    }

    /** Mirrors the shield-contact branch at $03:6F2A. */
    public void flipFromShield(int slot, int romLinkDirection) {
        if (romLinkDirection < 0 || romLinkDirection > 3) {
            throw new IllegalArgumentException("ROM Link direction must be 0..3: " + romLinkDirection);
        }
        if (!initialized[slot]) {
            initialize(slot);
        }
        if (state[slot] == 3) {
            return;
        }
        state[slot] = 3;
        speedZ[slot] = 0x20;
        speedX[slot] = FLIP_X_SPEEDS[romLinkDirection];
        speedY[slot] = FLIP_Y_SPEEDS[romLinkDirection];
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
    }

    public void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    public int state(int slot) {
        return state[slot];
    }

    public int speedX(int slot) {
        return speedX[slot];
    }

    public int speedY(int slot) {
        return speedY[slot];
    }

    public int speedZ(int slot) {
        return speedZ[slot];
    }

    public boolean allowsCombat(int slot) {
        // SpikedBeetleEntityHandler calls DefaultEnemyDamageCollisionHandler
        // before dispatching every state, including flipped state $03.
        return initialized[slot];
    }

    void startDash(int slot, int linkEntityX, int linkEntityY, int entityX, int entityY) {
        state[slot]++;
        direction[slot] = directionToLink(entityX, entityY, linkEntityX, linkEntityY);
    }

    final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];

    void setWalkingSpeed(int slot, int newDirection) {
        direction[slot] = newDirection & 0x03;
        speedX[slot] = WALKING_X_SPEEDS[direction[slot]];
        speedY[slot] = WALKING_Y_SPEEDS[direction[slot]];
    }

    static boolean nearLinkOnOneAxis(int entityX, int entityY, int linkEntityX, int linkEntityY) {
        return withinSourceWindow((linkEntityX - entityX) & 0xFF) || withinSourceWindow((linkEntityY - entityY) & 0xFF);
    }

    private static boolean withinSourceWindow(int distance) {
        return (((distance & 0xFF) + 0x06) & 0xFF) < 0x0A;
    }

    static int directionToLink(int entityX, int entityY, int linkEntityX, int linkEntityY) {
        int distanceX = signedByte((linkEntityX - entityX) & 0xFF);
        int distanceY = signedByte((linkEntityY - entityY) & 0xFF);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
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

    void clearSpeed(int slot) {
        speedX[slot] = 0;
        speedY[slot] = 0;
    }

    static int approach(int current, int target) {
        int difference = (target - current) & 0xFF;
        if (difference == 0) {
            return current & 0xFF;
        }
        return (difference & 0x80) != 0 ? (current - 1) & 0xFF : (current + 1) & 0xFF;
    }

    static int halveSignedSpeed(int speed) {
        int signed = signedByte(speed);
        return (signed >> 1) & 0xFF;
    }

    static int complementHalfSpeed(int speed) {
        return (~(signedByte(speed) >> 1)) & 0xFF;
    }

    private static int directionForX(int speed) {
        return (speed & 0x80) != 0 ? 1 : 0;
    }

    private static int directionForY(int speed) {
        return (speed & 0x80) != 0 ? 2 : 3;
    }

    static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
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
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    record Position(int x, int y, boolean collided) {}

    public record Update(RoomEntity entity, int transitionCountdown, int options1, int hitboxFlags) {}
}
