package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Bank-$06 {@code HinoxEntityHandler} state machine for entity {@code $89}. */
public final class HinoxMotion {
    public static final int ENTITY_TYPE = 0x89;
    static final int ENTITY_BOMB = 0x02;

    static final int STATE_INITIAL = 0;
    static final int STATE_WANDER = 1;
    static final int STATE_CHARGE_WINDUP = 2;
    static final int STATE_CHARGE = 3;
    public static final int STATE_GRAB = 4;
    static final int STATE_BOMB = 5;

    static final int[] SPEED_X_BY_DIRECTION = {0x08, 0xF8, 0x00, 0x00};
    static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xF8, 0x08};
    static final int[] GRAB_LINK_X_OFFSETS = {0xEF, 0xEF, 0xEF, 0xEF, 0xEF, 0xEF, 0xEF, 0xEF, 0xF3, 0xF7, 0xFB, 0x00};
    static final int[] GRAB_LINK_Z = {0x15, 0x15, 0x15, 0x15, 0x15, 0x14, 0x14, 0x14, 0x10, 0x08, 0x04, 0x00};
    static final int[] GRAB_BODY_VARIANT = {0, 0, 0, 0, 1, 1, 1, 1, 0, 0};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState3 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] entityCollisionFlags = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] bombOriginX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] bombOriginY = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] bombOriginWritten = new boolean[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record BombSpawn(
            int type, int x, int y, int z, int speedX, int speedY, int speedZ, int transitionCountdown) {}

    public record Update(
            RoomEntity entity,
            int state,
            int transitionCountdown,
            int spriteVariant,
            boolean linkMotionBlocked,
            int heldLinkX,
            int heldLinkY,
            int heldLinkZ,
            int linkSpeedX,
            int linkSpeedY,
            int linkVelocityZ,
            int linkAirborneState,
            int linkDamage,
            int jingleId,
            int waveId,
            boolean dustRequested,
            boolean heldLinkPositionWritten,
            boolean applyHeldLinkPose,
            BombSpawn bombSpawn) {}

    public void initialize(int slot) {
        validateSlot(slot);
        state[slot] = STATE_INITIAL;
        privateState1[slot] = 0;
        privateState3[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        inertia[slot] = 0;
        privateCountdown1[slot] = 0;
        entityCollisionFlags[slot] = 0;
        bombOriginX[slot] = 0;
        bombOriginY[slot] = 0;
        bombOriginWritten[slot] = false;
        initialized[slot] = true;
    }

    public Update advance(HinoxFrame inputs) {
        return HinoxBehavior.advance(this, inputs);
    }

    Update update(HinoxResult inputs) {
        return HinoxResults.update(this, inputs);
    }

    Position move(RoomEntity entity, int x, int y, RoomEntityBackgroundInteraction background, int slot) {
        return HinoxBehavior.move(this, entity, x, y, background, slot);
    }

    int tickInertia(int slot) {
        inertia[slot] = (inertia[slot] + 1) & 0xFF;
        return (inertia[slot] >>> 4) & 0x01;
    }

    void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    public int state(int slot) {
        validateSlot(slot);
        return state[slot];
    }

    public int speedX(int slot) {
        validateSlot(slot);
        return speedX[slot] & 0xFF;
    }

    public int speedY(int slot) {
        validateSlot(slot);
        return speedY[slot] & 0xFF;
    }

    int inertia(int slot) {
        validateSlot(slot);
        return inertia[slot] & 0xFF;
    }

    int privateCountdown1(int slot) {
        validateSlot(slot);
        return privateCountdown1[slot] & 0xFF;
    }

    public void decrementPrivateCountdown1(int slot) {
        validateSlot(slot);
        if (privateCountdown1[slot] > 0) {
            privateCountdown1[slot]--;
        }
    }

    public int collisionFlags(int slot) {
        validateSlot(slot);
        return entityCollisionFlags[slot] & 0xFF;
    }

    static Vector vectorTowardsLink(int x, int y, int entityZ, int linkX, int linkY, int length) {
        int dx = signedByte(linkX - x);
        int dy = signedByte(linkY - y + entityZ);
        int ax = Math.abs(dx);
        int ay = Math.abs(dy);
        int major = Math.max(ax, ay);
        int minor = Math.min(ax, ay);
        int small = major == 0 ? length : divide(minor, major, length);
        int vx = ax >= ay ? length : small;
        int vy = ay > ax ? length : small;
        if (dx < 0) vx = -vx;
        if (dy < 0) vy = -vy;
        return new Vector(vx & 0xFF, vy & 0xFF);
    }

    private static int divide(int minor, int major, int length) {
        int result = 0;
        int remainder = 0;
        for (int i = 0; i < length; i++) {
            int sum = remainder + minor;
            if (sum >= major) {
                sum -= major;
                result++;
            }
            remainder = sum;
        }
        return result;
    }

    static boolean withinDistance(int value, int target, int distance) {
        int delta = (value - target) & 0xFF;
        int absolute = delta < 0x80 ? delta : 0x100 - delta;
        return absolute < distance;
    }

    static int directionToLink(int x, int y, int linkX, int linkY) {
        int dx = signedByte(linkX - x);
        int dy = signedByte(linkY - y);
        if (Math.abs(dy) >= Math.abs(dx)) {
            return dy < 0 ? 2 : 3;
        }
        return dx < 0 ? 1 : 0;
    }

    static int addSpeed(int position, int speed, int[] accumulator, int slot) {
        speed &= 0xFF;
        if (speed == 0) return position & 0xFF;
        int sum = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = sum & 0xFF;
        int delta = signedByte(speed) >> 4;
        if (sum > 0xFF) delta++;
        return (position + delta) & 0xFF;
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    static int horizontalDirection(int speed) {
        return (speed & 0x80) != 0 ? EntityBackgroundCollisionResult.LEFT : EntityBackgroundCollisionResult.RIGHT;
    }

    static int verticalDirection(int speed) {
        return (speed & 0x80) != 0 ? EntityBackgroundCollisionResult.UP : EntityBackgroundCollisionResult.DOWN;
    }

    static void validateSlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Hinox slot out of range: " + slot);
        }
    }

    record Vector(int x, int y) {}

    record Position(int x, int y) {}
}
