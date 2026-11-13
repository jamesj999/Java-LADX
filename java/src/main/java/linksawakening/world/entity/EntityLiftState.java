package linksawakening.world.entity;

/** Shared liftThrow slot state, independent of behavior dispatch and construction order. */
final class EntityLiftState {
    private final int[] liftedPhase = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] liftedSourceDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] liftedStateInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] liftableRockSmashCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] liftableRockSmashSourceVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] liftableRockSmashActive = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] thrownDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] ledgeTransitionTimer = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] thrownMotionInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private int liftedEntitySlot = -1;
    private int liftedCarryState;
    private int liftedEffectiveDirection;
    private int liftedLinkC13B;

    int readLiftedPhase(int slot) {
        return liftedPhase[slot];
    }

    int writeLiftedPhase(int slot, int value) {
        return liftedPhase[slot] = value;
    }

    int readLiftedSourceDirection(int slot) {
        return liftedSourceDirection[slot];
    }

    int writeLiftedSourceDirection(int slot, int value) {
        return liftedSourceDirection[slot] = value;
    }

    boolean readLiftedStateInitialized(int slot) {
        return liftedStateInitialized[slot];
    }

    boolean writeLiftedStateInitialized(int slot, boolean value) {
        return liftedStateInitialized[slot] = value;
    }

    int readLiftableRockSmashCountdown(int slot) {
        return liftableRockSmashCountdown[slot];
    }

    int writeLiftableRockSmashCountdown(int slot, int value) {
        return liftableRockSmashCountdown[slot] = value;
    }

    int postLiftableRockSmashCountdown(int slot, int delta) {
        int previous = liftableRockSmashCountdown[slot];
        liftableRockSmashCountdown[slot] += delta;
        return previous;
    }

    int readLiftableRockSmashSourceVariant(int slot) {
        return liftableRockSmashSourceVariant[slot];
    }

    int writeLiftableRockSmashSourceVariant(int slot, int value) {
        return liftableRockSmashSourceVariant[slot] = value;
    }

    boolean readLiftableRockSmashActive(int slot) {
        return liftableRockSmashActive[slot];
    }

    boolean writeLiftableRockSmashActive(int slot, boolean value) {
        return liftableRockSmashActive[slot] = value;
    }

    int readThrownDirection(int slot) {
        return thrownDirection[slot];
    }

    int writeThrownDirection(int slot, int value) {
        return thrownDirection[slot] = value;
    }

    int[] storageThrownDirection() {
        return thrownDirection;
    }

    int readLedgeTransitionTimer(int slot) {
        return ledgeTransitionTimer[slot];
    }

    int writeLedgeTransitionTimer(int slot, int value) {
        return ledgeTransitionTimer[slot] = value;
    }

    boolean readThrownMotionInitialized(int slot) {
        return thrownMotionInitialized[slot];
    }

    boolean writeThrownMotionInitialized(int slot, boolean value) {
        return thrownMotionInitialized[slot] = value;
    }

    int readLiftedEntitySlot() {
        return liftedEntitySlot;
    }

    int writeLiftedEntitySlot(int value) {
        return liftedEntitySlot = value;
    }

    int readLiftedCarryState() {
        return liftedCarryState;
    }

    int writeLiftedCarryState(int value) {
        return liftedCarryState = value;
    }

    int readLiftedEffectiveDirection() {
        return liftedEffectiveDirection;
    }

    int writeLiftedEffectiveDirection(int value) {
        return liftedEffectiveDirection = value;
    }

    int readLiftedLinkC13B() {
        return liftedLinkC13B;
    }

    int writeLiftedLinkC13B(int value) {
        return liftedLinkC13B = value;
    }
}
