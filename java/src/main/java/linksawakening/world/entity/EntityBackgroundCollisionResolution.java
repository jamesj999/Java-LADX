package linksawakening.world.entity;

/** Collision result and the ROM ledge-timer byte to retain for the entity. */
public record EntityBackgroundCollisionResolution(EntityBackgroundCollisionResult result,
                                           int nextLedgeTimer) {
    public EntityBackgroundCollisionResolution {
        if (result == null) {
            throw new IllegalArgumentException("Collision result cannot be null");
        }
        nextLedgeTimer &= 0xFF;
    }
}
