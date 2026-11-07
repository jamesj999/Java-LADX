package linksawakening.world.entity;

import linksawakening.world.entity.LinkPositionHistory;

import java.util.function.IntSupplier;

/** Immutable frame inputs; referenced collision and RNG services retain their identity. */
public record EntityFrameContext(
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
    int linkSpeedY,
    boolean creditsGameplay,
    boolean handlerLinkCollisionEnabled) {
  public EntityFrameContext(
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
    this(
        frameCounter,
        linkEntityX,
        linkEntityY,
        collisionType,
        randomByteSupplier,
        backgroundCollision,
        objectCollision,
        linkPositionHistory,
        linkZ,
        linkAirborne,
        linkDirection,
        entityYOffset,
        projectileLinkState,
        swordCollisionActive,
        swordX,
        swordWidth,
        swordY,
        swordHeight,
        linkSpeedX,
        linkSpeedY,
        false,
        projectileLinkState.motionState() < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE);
  }

  EntityFrameContext withRuntimeInputs(
      int frame, int linkX, int linkY, RoomEntityBackgroundCollision collision) {
    return new EntityFrameContext(
        frame,
        linkX,
        linkY,
        collisionType,
        randomByteSupplier,
        collision,
        objectCollision,
        linkPositionHistory,
        linkZ,
        linkAirborne,
        linkDirection,
        entityYOffset,
        projectileLinkState,
        swordCollisionActive,
        swordX,
        swordWidth,
        swordY,
        swordHeight,
        linkSpeedX,
        linkSpeedY,
        creditsGameplay,
        handlerLinkCollisionEnabled);
  }
  EntityFrameContext withLinkSpeeds(int speedX, int speedY) {
    return new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, collisionType,
        randomByteSupplier, backgroundCollision, objectCollision, linkPositionHistory,
        linkZ, linkAirborne, linkDirection, entityYOffset, projectileLinkState,
        swordCollisionActive, swordX, swordWidth, swordY, swordHeight, speedX, speedY,
        creditsGameplay, handlerLinkCollisionEnabled);
  }
}
