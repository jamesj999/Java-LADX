package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$07 WingedOctorokEntityHandler state and WRAM fields. */
public final class WingedOctorokMotion {
    static final int ENTITY_WINGED_OCTOROK = 0xAE;
    private static final int ENTITY_SWORD = 0x01;
    static final int[] SPEED_X_BY_DIRECTION = {0x08, 0xF8, 0x00, 0x00};
    static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xF8, 0x08};
    private static final int[] BASE_VARIANT_BY_DIRECTION = {6, 4, 2, 0};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState2 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateCountdown2 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] zPosition = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] presentationVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] zInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record RockSpawn(int direction) {
        public RockSpawn {
            if (direction < 0 || direction > 3) {
                throw new IllegalArgumentException("Winged Octorok rock direction out of range: " + direction);
            }
        }
    }

    public record Update(
            RoomEntity entity,
            int state,
            int transitionCountdown,
            RockSpawn rockSpawn,
            boolean jumped,
            boolean skippedDefaultCollision) {}

    public void initialize(int slot) {
        clear(slot);
        initialized[slot] = true;
    }

    public Update advance(WingedOctorokFrame inputs) {
        RoomEntity entity = inputs.entity();
        int frameCounter = inputs.frameCounter();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        java.util.function.IntSupplier randomByteSupplier = inputs.randomByteSupplier();
        RoomEntityBackgroundCollision backgroundCollision = inputs.backgroundCollision();
        int transitionCountdown = inputs.transitionCountdown();
        int ignoreHitsCountdown = inputs.ignoreHitsCountdown();
        boolean actionButtonsHeld = inputs.actionButtonsHeld();
        int linkItemA = inputs.linkItemA();
        int linkItemB = inputs.linkItemB();
        return advance(new WingedOctorokInteractionFrame(
                entity,
                frameCounter,
                linkEntityX,
                linkEntityY,
                randomByteSupplier,
                backgroundCollision,
                transitionCountdown,
                ignoreHitsCountdown,
                actionButtonsHeld,
                actionButtonsHeld,
                linkItemA,
                linkItemB));
    }

    public Update advance(WingedOctorokInteractionFrame inputs) {
        return wingedOctorokStep.run(this, inputs);
    }

    public void decrementPrivateCountdown2(int slot) {
        if (privateCountdown2[slot] > 0) {
            privateCountdown2[slot]--;
        }
    }

    public int state(int slot) {
        return state[slot];
    }

    int direction(int slot) {
        return direction[slot];
    }

    public int privateState2(int slot) {
        return privateState2[slot];
    }

    public int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    public int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    public int speedZ(int slot) {
        return speedZ[slot] & 0xFF;
    }

    public void clear(int slot) {
        state[slot] = 0;
        direction[slot] = 0;
        privateState1[slot] = 0;
        privateState2[slot] = 0;
        privateCountdown2[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        zPosition[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        inertia[slot] = 0;
        presentationVariant[slot] = 0;
        initialized[slot] = false;
        zInitialized[slot] = false;
    }

    void setVariantForDirection(int slot) {
        inertia[slot] = (inertia[slot] + 1) & 0xFF;
        presentationVariant[slot] = BASE_VARIANT_BY_DIRECTION[direction[slot] & 0x03] | ((inertia[slot] >>> 4) & 0x01);
    }

    void clearHorizontalSpeed(int slot) {
        speedX[slot] = 0;
        speedY[slot] = 0;
    }

    static boolean swordAttackHeld(boolean actionButtonAHeld, boolean actionButtonBHeld, int linkItemA, int linkItemB) {
        if ((linkItemB & 0xFF) == ENTITY_SWORD) {
            return actionButtonBHeld;
        }
        return (linkItemA & 0xFF) == ENTITY_SWORD && actionButtonAHeld;
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

    static Vector vectorTowardsLink(int entityX, int entityY, int entityZ, int linkX, int linkY, int length) {
        int distanceY = signedByte(linkY - entityY + entityZ);
        int distanceX = signedByte(linkX - entityX);
        int absoluteY = Math.abs(distanceY);
        int absoluteX = Math.abs(distanceX);
        int smaller = Math.min(absoluteX, absoluteY);
        int larger = Math.max(absoluteX, absoluteY);
        int quotient = larger == 0 ? length : (length * smaller) / larger;
        int y = absoluteY < absoluteX ? quotient : length;
        int x = absoluteY < absoluteX ? length : quotient;
        if (distanceY < 0) {
            y = -y;
        }
        if (distanceX < 0) {
            x = -x;
        }
        return new Vector(y & 0xFF, x & 0xFF);
    }

    static boolean withinSignedWindow(int difference) {
        int signedDifference = signedByte(difference);
        return signedDifference >= -0x20 && signedDifference < 0x20;
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

    private static int horizontalDirection(int speed) {
        return signedByte(speed) < 0 ? 1 : 0;
    }

    private static int verticalDirection(int speed) {
        return signedByte(speed) < 0 ? 2 : 3;
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    record Position(int x, int y, boolean collided) {}

    record Vector(int y, int x) {}

    final WingedOctorokStep wingedOctorokStep = new WingedOctorokStep();
}
