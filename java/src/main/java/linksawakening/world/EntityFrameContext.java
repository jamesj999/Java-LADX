package linksawakening.world;

import java.util.function.IntSupplier;

/** Immutable frame inputs; referenced collision and RNG services retain their identity. */
record EntityFrameContext(
    int frameCounter,
    int linkEntityX,
    int linkEntityY,
    int collisionType,
    IntSupplier randomByteSupplier,
    RoomEntityBackgroundCollision backgroundCollision,
    RoomEntityObjectCollision objectCollision,
    LinkPositionHistory linkPositionHistory,
    int linkZ,
    boolean linkAirborne,
    int linkDirection,
    int entityYOffset,
    EnemyProjectileCollision.LinkState projectileLinkState,
    boolean swordCollisionActive,
    int swordX,
    int swordWidth,
    int swordY,
    int swordHeight,
    int linkSpeedX,
    int linkSpeedY) {
}
