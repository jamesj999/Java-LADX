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
        liftedPhase[slot] = value;
        return liftedPhase[slot];
    }

    int readLiftedSourceDirection(int slot) {
        return liftedSourceDirection[slot];
    }

    int writeLiftedSourceDirection(int slot, int value) {
        liftedSourceDirection[slot] = value;
        return liftedSourceDirection[slot];
    }

    boolean readLiftedStateInitialized(int slot) {
        return liftedStateInitialized[slot];
    }

    boolean writeLiftedStateInitialized(int slot, boolean value) {
        liftedStateInitialized[slot] = value;
        return liftedStateInitialized[slot];
    }

    int readLiftableRockSmashCountdown(int slot) {
        return liftableRockSmashCountdown[slot];
    }

    int writeLiftableRockSmashCountdown(int slot, int value) {
        liftableRockSmashCountdown[slot] = value;
        return liftableRockSmashCountdown[slot];
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
        liftableRockSmashSourceVariant[slot] = value;
        return liftableRockSmashSourceVariant[slot];
    }

    boolean readLiftableRockSmashActive(int slot) {
        return liftableRockSmashActive[slot];
    }

    boolean writeLiftableRockSmashActive(int slot, boolean value) {
        liftableRockSmashActive[slot] = value;
        return liftableRockSmashActive[slot];
    }

    int readThrownDirection(int slot) {
        return thrownDirection[slot];
    }

    int writeThrownDirection(int slot, int value) {
        thrownDirection[slot] = value;
        return thrownDirection[slot];
    }

    int[] storageThrownDirection() {
        return thrownDirection;
    }

    int readLedgeTransitionTimer(int slot) {
        return ledgeTransitionTimer[slot];
    }

    int writeLedgeTransitionTimer(int slot, int value) {
        ledgeTransitionTimer[slot] = value;
        return ledgeTransitionTimer[slot];
    }

    boolean readThrownMotionInitialized(int slot) {
        return thrownMotionInitialized[slot];
    }

    boolean writeThrownMotionInitialized(int slot, boolean value) {
        thrownMotionInitialized[slot] = value;
        return thrownMotionInitialized[slot];
    }

    int readLiftedEntitySlot() {
        return liftedEntitySlot;
    }

    int writeLiftedEntitySlot(int value) {
        liftedEntitySlot = value;
        return liftedEntitySlot;
    }

    int readLiftedCarryState() {
        return liftedCarryState;
    }

    int writeLiftedCarryState(int value) {
        liftedCarryState = value;
        return liftedCarryState;
    }

    int readLiftedEffectiveDirection() {
        return liftedEffectiveDirection;
    }

    int writeLiftedEffectiveDirection(int value) {
        liftedEffectiveDirection = value;
        return liftedEffectiveDirection;
    }

    int readLiftedLinkC13B() {
        return liftedLinkC13B;
    }

    int writeLiftedLinkC13B(int value) {
        liftedLinkC13B = value;
        return liftedLinkC13B;
    }
}
