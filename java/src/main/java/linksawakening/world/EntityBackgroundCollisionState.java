package linksawakening.world;

/** ROM state read by ApplyEntityCollisionWithObject's ledge branch. */
record EntityBackgroundCollisionState(int frameCounter, boolean indoorRoom,
                                      int thrownDirection, int ledgeTimer) {
    EntityBackgroundCollisionState {
        frameCounter &= 0xFF;
        thrownDirection &= 0xFF;
        ledgeTimer &= 0xFF;
    }
}
