package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Bank-$07 Hiding Zol reveal, bounce, and hide state machine. */
public final class HidingZolMotion {
    static final int ENTITY_HIDING_ZOL = 0x9B;

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] swordCollisionEnabled = new boolean[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] linkCollisionEnabled = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public void initialize(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        privateState1[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        swordCollisionEnabled[slot] = false;
        linkCollisionEnabled[slot] = false;
        initialized[slot] = true;
    }

    public Update advance(
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        RoomEntityBackgroundInteraction backgroundInteraction =
                backgroundCollision == null ? null : RoomEntityBackgroundInteraction.fromBoolean(backgroundCollision);
        return advance(entity, linkEntityX, linkEntityY, randomByteSupplier, backgroundInteraction, 0);
    }

    public Update advance(
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundInteraction backgroundInteraction,
            int frameCounter) {
        return new HidingZolStep(
                        this, entity, linkEntityX, linkEntityY, randomByteSupplier, backgroundInteraction, frameCounter)
                .run();
    }

    public boolean allowsSwordCollision(int slot) {
        return swordCollisionEnabled[slot];
    }

    public boolean allowsLinkCollision(int slot) {
        return linkCollisionEnabled[slot];
    }

    public void clear(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        privateState1[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        swordCollisionEnabled[slot] = false;
        linkCollisionEnabled[slot] = false;
        initialized[slot] = false;
    }

    public int state(int slot) {
        return state[slot];
    }

    public int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    public int privateState1(int slot) {
        return privateState1[slot];
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

    void decrementTransitionCountdown(int slot) {
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }
    }

    static boolean isWithinRevealWindow(int entityX, int entityY, int linkX, int linkY) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
        return distanceX >= -0x20 && distanceX < 0x20 && distanceY >= -0x20 && distanceY < 0x20;
    }

    int[] move(
            RoomEntity entity,
            int x,
            int y,
            RoomEntityBackgroundInteraction backgroundInteraction,
            int frameCounter,
            int slot) {
        int movedX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        if (movedX != x && backgroundInteraction != null) {
            EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                    entity, signedByte(speedX[slot]) < 0 ? 1 : 0, movedX, y, 0x03, frameCounter);
            if (result.blocked()) {
                movedX = x;
            }
        }
        int movedY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        if (movedY != y && backgroundInteraction != null) {
            EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                    entity, signedByte(speedY[slot]) < 0 ? 2 : 3, movedX, movedY, 0x03, frameCounter);
            if (result.blocked()) {
                movedY = y;
            }
        }
        return new int[] {movedX, movedY};
    }

    static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
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

    static Vector vectorTowardsLink(int entityX, int entityY, int entityZ, int linkX, int linkY, int length) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY + entityZ) & 0xFF);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean yIsLargerAxis = absoluteY > absoluteX;
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum;
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

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    public record Update(
            RoomEntity entity,
            boolean hitGround,
            boolean clearsIgnoreHitsCountdown,
            boolean jumpJingle,
            int physicsFlags) {}

    record Vector(int x, int y) {}
}
