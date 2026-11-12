package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;

/** Bank-$06 BooBuddyEntityHandler's drift, trigger, and flee states. */
public final class BooBuddyMotion {
    private int booBuddyTriggerCount;

    public int readBooBuddyTriggerCount() {
        return booBuddyTriggerCount;
    }

    public int writeBooBuddyTriggerCount(int value) {
        return booBuddyTriggerCount = value;
    }

    public static final int ENTITY_TYPE = 0x50;
    public static final int INITIAL_PHYSICS_FLAGS = 0x02 | 0x10;
    public static final int OPTIONS1 = 0x20 | 0x08;

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] selectedVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Entity $50 uses the ordinary no-op initialization path. */
    public void initialize(int slot) {
        state[slot] = 0;
        selectedVariant[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        privateState1[slot] = 0;
        privateCountdown1[slot] = 0;
        initialized[slot] = true;
    }

    /**
     * Advances one handler pass. The shared entity timer has already reduced
     * the transition countdown; the private countdown is reduced here to
     * mirror UpdateEntityTimers' private-counter write.
     */
    Update advance(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int transitionCountdown,
            int roomTriggerCount,
            boolean swordMiddle,
            IntSupplier randomByteSupplier) {
        tickTimers(entity.slot());
        return advanceAfterTimers(
                entity,
                frameCounter,
                linkEntityX,
                linkEntityY,
                transitionCountdown,
                roomTriggerCount,
                swordMiddle,
                randomByteSupplier);
    }

    public void tickTimers(int slot) {
        ensureInitialized(slot);
        if (privateCountdown1[slot] > 0) {
            privateCountdown1[slot]--;
        }
    }

    public RoomEntity advanceRecoil(RoomEntity entity, int recoilX, int recoilY) {
        int slot = entity.slot();
        ensureInitialized(slot);
        // ApplyRecoilIfNeeded_06 temporarily replaces speeds, so recoil
        // shares the ordinary position fractions and leaves drift speeds intact.
        int x = addSpeedToPosition(entity.x(), recoilX, speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), recoilY, speedYAccumulator, slot);
        return withPosition(entity, x, y, selectedVariant[slot]);
    }

    public Update advanceAfterTimers(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int transitionCountdown,
            int roomTriggerCount,
            boolean swordMiddle,
            IntSupplier randomByteSupplier) {
        return advanceAfterTimers(new BooBuddyFrame(
                entity,
                frameCounter,
                linkEntityX,
                linkEntityY,
                transitionCountdown,
                roomTriggerCount,
                swordMiddle,
                randomByteSupplier));
    }

    public linksawakening.world.entity.motion.BooBuddyMotion.Update advanceAfterTimers(BooBuddyFrame inputs) {
        return booBuddyFrameStep.run(this, inputs);
    }

    public void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    public int state(int slot) {
        return state[slot];
    }

    public int selectedVariant(int slot) {
        ensureInitialized(slot);
        return selectedVariant[slot];
    }

    public int speedX(int slot) {
        return speedX[slot];
    }

    public int speedY(int slot) {
        return speedY[slot];
    }

    int privateCountdown1(int slot) {
        return privateCountdown1[slot];
    }

    public boolean allowsEnemyCollision(int slot, int roomTriggerCount, int transitionCountdown, boolean swordMiddle) {
        if (roomTriggerCount != 0) {
            // State 0 only increments into state 1 on this pass; neither
            // handler path reaches a collision helper on that frame.
            return state[slot] == 1;
        }
        // State 0 calls label_3B44 only on its ordinary drift path. The
        // transition and private-countdown branches, plus the sword-middle
        // branch, clear movement and return without a Link collision check.
        return state[slot] == 0 && (transitionCountdown & 0xFF) == 0 && privateCountdown1[slot] == 0 && !swordMiddle;
    }

    public boolean allowsSwordCollision(int slot, int roomTriggerCount) {
        // Only BooBuddyState1Handler calls DefaultEnemyDamageCollisionHandler.
        return roomTriggerCount != 0 && state[slot] == 1;
    }

    void ensureInitialized(int slot) {
        if (!initialized[slot]) {
            initialize(slot);
        }
    }

    int animateTransition(int frameCounter, int x, int linkEntityX, int slot, int countdown) {
        int variant;
        if (((frameCounter >>> 2) & 0x01) == 0) {
            variant = -1;
        } else {
            int direction = signedByte((linkEntityX - x) & 0xFF) < 0 ? 1 : 0;
            variant = direction == 0 ? 0x06 : 0x07;
        }
        privateState1[slot] = countdown == 1 ? 1 : 0;
        return variant;
    }

    static boolean withinSwordTriggerWindow(int x, int y, int linkX, int linkY) {
        int distanceX = signedByte((linkX - x) & 0xFF);
        int distanceY = signedByte((linkY - y) & 0xFF);
        return distanceX >= -0x24 && distanceX < 0x24 && distanceY >= -0x24 && distanceY < 0x24;
    }

    static int directionVariant(int entityX, int linkX, int frameCounter) {
        int direction = signedByte((linkX - entityX) & 0xFF) < 0 ? 2 : 0;
        return direction + ((frameCounter >>> 4) & 0x01);
    }

    static RoomEntity withPosition(RoomEntity entity, int x, int y, int variant) {
        return EntitySnapshots.withMotion(
                entity,
                x & 0xFF,
                y & 0xFF,
                variant,
                entity.entityFlipAttribute(),
                entity.z(),
                entity.deathSpriteVariant(),
                entity.powerRecoilDeath());
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

    static int approachByFour(int current, int target) {
        int difference = (target - current) & 0xFF;
        if (difference == 0) {
            return current & 0xFF;
        }
        return (current + ((difference & 0x80) == 0 ? 4 : -4)) & 0xFF;
    }

    static Vector vectorTowardsLink(int entityX, int entityY, int linkX, int linkY, int length) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
        boolean xNegative = distanceX < 0;
        boolean yNegative = distanceY < 0;
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        int result = divideComponent(Math.min(absoluteX, absoluteY), Math.max(absoluteX, absoluteY), length);
        int x = absoluteX >= absoluteY ? length : result;
        int y = absoluteY > absoluteX ? length : result;
        if (xNegative) {
            x = -x;
        }
        if (yNegative) {
            y = -y;
        }
        return new Vector(x & 0xFF, y & 0xFF);
    }

    private static int divideComponent(int smallerDistance, int largerDistance, int length) {
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
        return result;
    }

    static int negate(int value) {
        return (-signedByte(value)) & 0xFF;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    record Vector(int x, int y) {}

    public record Update(RoomEntity entity, int transitionCountdown, boolean unloaded, int healthOverride) {}

    final BooBuddyFrameStep booBuddyFrameStep = new BooBuddyFrameStep();
}
