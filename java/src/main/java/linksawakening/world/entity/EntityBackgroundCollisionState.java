package linksawakening.world.entity;

/** ROM state read by ApplyEntityCollisionWithObject's ledge branch. */
public record EntityBackgroundCollisionState(
        int frameCounter,
        boolean indoorRoom,
        int thrownDirection,
        int ledgeTimer,
        int switchBlocksState,
        boolean linkStandingOnSwitchBlock) {
    public EntityBackgroundCollisionState {
        frameCounter &= 0xFF;
        thrownDirection &= 0xFF;
        ledgeTimer &= 0xFF;
        switchBlocksState &= 0xFF;
    }

    public EntityBackgroundCollisionState(int frameCounter, boolean indoorRoom, int thrownDirection, int ledgeTimer) {
        this(frameCounter, indoorRoom, thrownDirection, ledgeTimer, 0, false);
    }

    public EntityBackgroundCollisionState(
            int frameCounter, boolean indoorRoom, int thrownDirection, int ledgeTimer, int switchBlocksState) {
        this(frameCounter, indoorRoom, thrownDirection, ledgeTimer, switchBlocksState, false);
    }
}
