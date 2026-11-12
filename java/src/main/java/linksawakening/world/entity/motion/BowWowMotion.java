package linksawakening.world.entity.motion;

import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.BowWowChain;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/**
 * The home, kidnapped, and following motion states of bank-$05's Bow-Wow handler.
 *
 * <p>The original keeps Bow-Wow's setup state separate from the active
 * handler state. This class keeps those WRAM-like fields per entity slot so
 * the host renderer can consume the same position and sprite-variant result
 * without running a general Game Boy CPU.</p>
 */
public final class BowWowMotion {
    private int bowWowState;

    public int readBowWowState() {
        return bowWowState;
    }

    public int writeBowWowState(int value) {
        return bowWowState = value;
    }

    static final int[] SPEED_X_BY_RANDOM = {0x04, 0x08, 0x0C, 0x08, 0xFC, 0xF8, 0xF4, 0xF8};
    static final int[] SPEED_Y_BY_RANDOM = {0xF4, 0xF8, 0x04, 0x08, 0x0C, 0x08, 0xFC, 0xF8};

    private final BowWowChain[] chains = new BowWowChain[EntityRoomLoader.MAX_ENTITIES];

    final int[] activeState = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] selectedVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown2 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] targetX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] targetY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] targetSlot = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] updatedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public void initialize(int slot) {
        clear(slot);
        initialized[slot] = true;
    }

    RoomEntity advance(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        return advanceWithTargetScan(new BowWowTargetFrame(
                        entity,
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkEntityZ,
                        randomByteSupplier,
                        backgroundCollision,
                        List.of(entity),
                        type -> false))
                .entity();
    }

    Update advanceWithTargetScan(BowWowTargetFrame inputs) {
        return BowWowBehavior.advanceWithTargetScan(this, inputs);
    }

    public Update advanceAfterTimers(BowWowFrame inputs) {
        return BowWowBehavior.advanceAfterTimers(this, inputs);
    }

    public Update advanceAfterTimers(BowWowCollisionFrame inputs) {
        return BowWowBehavior.advanceAfterTimers(this, inputs);
    }

    public void clear(int slot) {
        chains[slot] = new BowWowChain();
        activeState[slot] = 0;
        selectedVariant[slot] = 0;
        privateState4[slot] = 0;
        transitionCountdown[slot] = 0;
        privateCountdown1[slot] = 0;
        privateCountdown2[slot] = 0;
        targetX[slot] = 0;
        targetY[slot] = 0;
        targetSlot[slot] = -1;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        updatedZ[slot] = 0;
        initialized[slot] = false;
    }

    public RoomEntity advanceChain(RoomEntity entity, int questState, boolean linkMoving) {
        int slot = entity.slot();
        if (chains[slot] == null) chains[slot] = new BowWowChain();
        return chains[slot].advance(entity, targetX[slot], targetY[slot], questState, linkMoving);
    }

    public List<BowWowChain.Entry> chainEntries(int slot, int questState, int frame) {
        return chains[slot] == null ? List.of() : chains[slot].entries(slot, questState, frame);
    }

    int activeState(int slot) {
        return activeState[slot];
    }

    public int selectedVariant(int slot) {
        return selectedVariant[slot];
    }

    int privateState4(int slot) {
        return privateState4[slot];
    }

    public int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    public int privateCountdown2(int slot) {
        return privateCountdown2[slot];
    }

    int targetX(int slot) {
        return targetX[slot];
    }

    int targetY(int slot) {
        return targetY[slot];
    }

    public int targetSlot(int slot) {
        return targetSlot[slot];
    }

    int speedZ(int slot) {
        return speedZ[slot] & 0xFF;
    }

    int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    public void setTransitionCountdown(int slot, int value) {
        if (slot < 0 || slot >= transitionCountdown.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("Transition countdown must be an unsigned byte: " + value);
        }
        transitionCountdown[slot] = value;
    }

    public void setPrivateCountdown2(int slot, int value) {
        if (slot < 0 || slot >= privateCountdown2.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("Private countdown 2 must be an unsigned byte: " + value);
        }
        privateCountdown2[slot] = value;
    }

    RoomEntity setup(RoomEntity entity) {
        int slot = entity.slot();
        int x = byteValue(entity.x() + 0x04);
        int y = byteValue(entity.y() + 0x08);
        targetX[slot] = x;
        targetY[slot] = y;
        chains[slot].initialize(x, y - entity.z());
        privateState4[slot] = 1;
        return withPositionAndVariant(entity, x, y, entity.z(), entity.spriteVariant());
    }

    public void tickTimers(int slot) {
        if (!initialized[slot]) {
            initialize(slot);
        }
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }
        if (privateCountdown1[slot] > 0) {
            privateCountdown1[slot]--;
        }
        if (privateCountdown2[slot] > 0) {
            privateCountdown2[slot]--;
        }
    }

    /** Returns whether the source's gravity step crossed below signed zero. */
    boolean updateZ(int positionZ, int slot) {
        int nextZ = addSpeedToPosition(positionZ, speedZ[slot], speedZAccumulator, slot);
        speedZ[slot] = byteValue(speedZ[slot] - 0x02);
        if ((nextZ & 0x80) == 0) {
            updatedZ[slot] = nextZ;
            return false;
        }
        updatedZ[slot] = 0;
        speedZ[slot] = 0;
        return true;
    }

    int[] move(RoomEntity entity, int x, int y, int slot, RoomEntityBackgroundCollision backgroundCollision) {
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        // ApplyEntityInteractionWithBackground returns before the generic wall
        // collision path when hActiveEntityType is BowWow. Its earlier ground
        // status/splash work remains outside this bounded motion port.
        return new int[] {nextX, nextY};
    }

    int correctTargetX(int nextX, int beforeX, int slot) {
        return inSignedWindow(signedByte(targetX[slot] - nextX), 0x20) ? nextX : beforeX;
    }

    int correctTargetY(int nextY, int beforeY, int slot) {
        return inSignedWindow(signedByte(targetY[slot] - nextY), 0x20) ? nextY : beforeY;
    }

    int movementVariant(int frameCounter, int slot) {
        int horizontal = Math.abs(signedByte(speedX[slot]));
        int vertical = Math.abs(signedByte(speedY[slot]));
        int base;
        if (vertical >= horizontal) {
            base = signedByte(speedY[slot]) < 0 ? 6 : 0;
        } else {
            base = signedByte(speedX[slot]) < 0 ? 2 : 4;
        }
        return base == 6 ? 6 : base + ((frameCounter >>> 3) & 0x01);
    }

    static int findTarget(
            RoomEntity source,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            List<RoomEntity> entities,
            IntPredicate canEatEntity) {
        return BowWowBehavior.findTarget(source, linkEntityX, linkEntityY, randomByteSupplier, entities, canEatEntity);
    }

    static Vector vectorTowardsTarget(RoomEntity source, RoomEntity target, int sourceZ, int length) {
        int distanceX = signedByte(target.x() - source.x());
        // The shared vector helper reads Z after this handler's gravity update.
        int distanceY = signedByte(target.y() - source.y() + sourceZ);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean swapped = absoluteX < absoluteY;
        int smaller = Math.min(absoluteX, absoluteY);
        int larger = Math.max(absoluteX, absoluteY);
        int result = divideComponent(length, smaller, larger);
        int vectorX = swapped ? result : length;
        int vectorY = swapped ? length : result;
        if (distanceX < 0) {
            vectorX = -vectorX;
        }
        if (distanceY < 0) {
            vectorY = -vectorY;
        }
        return new Vector(vectorX & 0xFF, vectorY & 0xFF);
    }

    private static int divideComponent(int length, int smaller, int larger) {
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smaller;
            if (sum > 0xFF || sum >= larger) {
                sum -= larger;
                result++;
            }
            remainder = sum & 0xFF;
        }
        return result;
    }

    static boolean inUnsignedWindow(int difference, int offset, int limit) {
        return ((difference + offset) & 0xFF) < limit;
    }

    /** Mirrors label_005_4335's final X/visual-Y contact test. */
    static TargetContact findTargetContact(
            int sourceX, int sourceY, int sourceZ, int targetSlot, List<RoomEntity> entities) {
        if (targetSlot < 0 || targetSlot >= entities.size()) {
            return null;
        }
        RoomEntity target = entities.get(targetSlot);
        if (!target.loaded()
                || !inUnsignedWindow(sourceX - target.x(), 0x0E, 0x1A)
                || !inUnsignedWindow((sourceY - sourceZ) - target.y(), 0x10, 0x20)) {
            return null;
        }
        return new TargetContact(targetSlot, target.type());
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
        return byteValue(position + delta);
    }

    static boolean inSignedWindow(int distance, int halfOpen) {
        return distance >= -halfOpen && distance < halfOpen;
    }

    static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    static int byteValue(int value) {
        return value & 0xFF;
    }

    static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y, int z, int variant) {
        return EntitySnapshots.withMotion(entity, x, y, variant, entity.entityFlipAttribute(), z, -1, false);
    }

    public record Update(RoomEntity entity, int targetSlot, TargetContact targetContact) {
        boolean targetAcquired() {
            return targetSlot >= 0;
        }
    }

    public record TargetContact(int slot, int type) {}

    record Vector(int x, int y) {}
}
