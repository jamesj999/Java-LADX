package linksawakening.world.entity;

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
    /** Compatibility for world.room.RoomEntityBridge; use builder for new callers. */
    @Deprecated
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

    public static EntityFrameContextBuilder builder() {
        return new EntityFrameContextBuilder();
    }

    public EntityFrameContextBuilder toBuilder() {
        return builder()
                .frameCounter(frameCounter)
                .linkEntityX(linkEntityX)
                .linkEntityY(linkEntityY)
                .collisionType(collisionType)
                .randomByteSupplier(randomByteSupplier)
                .backgroundCollision(backgroundCollision)
                .objectCollision(objectCollision)
                .linkPositionHistory(linkPositionHistory)
                .linkZ(linkZ)
                .linkAirborne(linkAirborne)
                .linkDirection(linkDirection)
                .entityYOffset(entityYOffset)
                .projectileLinkState(projectileLinkState)
                .swordCollisionActive(swordCollisionActive)
                .swordX(swordX)
                .swordWidth(swordWidth)
                .swordY(swordY)
                .swordHeight(swordHeight)
                .linkSpeedX(linkSpeedX)
                .linkSpeedY(linkSpeedY)
                .creditsGameplay(creditsGameplay)
                .handlerLinkCollisionEnabled(handlerLinkCollisionEnabled);
    }

    EntityFrameContext withRuntimeInputs(int frame, int linkX, int linkY, RoomEntityBackgroundCollision collision) {
        return toBuilder()
                .frameCounter(frame)
                .linkEntityX(linkX)
                .linkEntityY(linkY)
                .backgroundCollision(collision)
                .build();
    }

    EntityFrameContext withLinkSpeeds(int speedX, int speedY) {
        return toBuilder().linkSpeedX(speedX).linkSpeedY(speedY).build();
    }
}
