package linksawakening.world;

/** Collision result and the ROM ledge-timer byte to retain for the entity. */
record EntityBackgroundCollisionResolution(EntityBackgroundCollisionResult result,
                                           int nextLedgeTimer) {
    EntityBackgroundCollisionResolution {
        if (result == null) {
            throw new IllegalArgumentException("Collision result cannot be null");
        }
        nextLedgeTimer &= 0xFF;
    }
}
