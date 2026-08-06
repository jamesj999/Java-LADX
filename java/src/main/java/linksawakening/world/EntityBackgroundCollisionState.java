package linksawakening.world;

/** ROM state read by ApplyEntityCollisionWithObject's ledge branch. */
record EntityBackgroundCollisionState(int frameCounter, boolean indoorRoom,
                                      int thrownDirection, int ledgeTimer,
                                      int switchBlocksState,
                                      boolean linkStandingOnSwitchBlock) {
    EntityBackgroundCollisionState {
        frameCounter &= 0xFF;
        thrownDirection &= 0xFF;
        ledgeTimer &= 0xFF;
        switchBlocksState &= 0xFF;
    }

    EntityBackgroundCollisionState(int frameCounter, boolean indoorRoom,
                                   int thrownDirection, int ledgeTimer) {
        this(frameCounter, indoorRoom, thrownDirection, ledgeTimer, 0, false);
    }

    EntityBackgroundCollisionState(int frameCounter, boolean indoorRoom,
                                   int thrownDirection, int ledgeTimer,
                                   int switchBlocksState) {
        this(frameCounter, indoorRoom, thrownDirection, ledgeTimer,
            switchBlocksState, false);
    }
}
