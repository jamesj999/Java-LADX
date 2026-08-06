package linksawakening.world;

/** ROM state read by ApplyEntityCollisionWithObject's ledge branch. */
record EntityBackgroundCollisionState(int frameCounter, boolean indoorRoom,
                                      int thrownDirection, int ledgeTimer,
                                      int switchBlocksState) {
    EntityBackgroundCollisionState {
        frameCounter &= 0xFF;
        thrownDirection &= 0xFF;
        ledgeTimer &= 0xFF;
        switchBlocksState &= 0xFF;
    }

    EntityBackgroundCollisionState(int frameCounter, boolean indoorRoom,
                                   int thrownDirection, int ledgeTimer) {
        this(frameCounter, indoorRoom, thrownDirection, ledgeTimer, 0);
    }
}
