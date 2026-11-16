package linksawakening.world.entity;

import java.util.function.IntSupplier;

/** Frame input fixtures for runtime tests. */
public final class EntityTestFrames {
    public static EntityFrameContext frame(RoomEntityRuntime runtime, int frameCounter) {
        return runtime.pipeline().frameContext(frameCounter);
    }

    public static EntityFrameContext frame(
            RoomEntityRuntime runtime,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier) {
        return EntityFrameContext.builder()
                .frameCounter(frameCounter)
                .linkEntityX(linkEntityX)
                .linkEntityY(linkEntityY)
                .collisionType(0)
                .randomByteSupplier(randomByteSupplier)
                .backgroundCollision(linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN)
                .objectCollision(linksawakening.world.entity.RoomEntityObjectCollision.NONE)
                .linkPositionHistory(null)
                .linkZ(0)
                .linkAirborne(false)
                .linkDirection(0)
                .entityYOffset(0)
                .projectileLinkState(EnemyProjectileCollision.LinkState.nonInteractive())
                .swordCollisionActive(false)
                .swordX(0)
                .swordWidth(0)
                .swordY(0)
                .swordHeight(0)
                .linkSpeedX(0)
                .linkSpeedY(0)
                .creditsGameplay(false)
                .handlerLinkCollisionEnabled(true)
                .build();
    }

    public static EntityFrameContext frame(
            RoomEntityRuntime runtime,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int collisionType,
            IntSupplier randomByteSupplier) {
        return EntityFrameContext.builder()
                .frameCounter(frameCounter)
                .linkEntityX(linkEntityX)
                .linkEntityY(linkEntityY)
                .collisionType(collisionType)
                .randomByteSupplier(randomByteSupplier)
                .backgroundCollision(linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN)
                .objectCollision(linksawakening.world.entity.RoomEntityObjectCollision.NONE)
                .linkPositionHistory(null)
                .linkZ(0)
                .linkAirborne(false)
                .linkDirection(0)
                .entityYOffset(0)
                .projectileLinkState(EnemyProjectileCollision.LinkState.nonInteractive())
                .swordCollisionActive(false)
                .swordX(0)
                .swordWidth(0)
                .swordY(0)
                .swordHeight(0)
                .linkSpeedX(0)
                .linkSpeedY(0)
                .creditsGameplay(false)
                .handlerLinkCollisionEnabled(true)
                .build();
    }

    public static EntityFrameContext frame(
            RoomEntityRuntime runtime,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        return EntityFrameContext.builder()
                .frameCounter(frameCounter)
                .linkEntityX(linkEntityX)
                .linkEntityY(linkEntityY)
                .collisionType(0)
                .randomByteSupplier(randomByteSupplier)
                .backgroundCollision(backgroundCollision)
                .objectCollision(linksawakening.world.entity.RoomEntityObjectCollision.NONE)
                .linkPositionHistory(null)
                .linkZ(0)
                .linkAirborne(false)
                .linkDirection(0)
                .entityYOffset(0)
                .projectileLinkState(EnemyProjectileCollision.LinkState.nonInteractive())
                .swordCollisionActive(false)
                .swordX(0)
                .swordWidth(0)
                .swordY(0)
                .swordHeight(0)
                .linkSpeedX(0)
                .linkSpeedY(0)
                .creditsGameplay(false)
                .handlerLinkCollisionEnabled(true)
                .build();
    }

    public static EntityFrameContext frame(
            RoomEntityRuntime runtime,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int collisionType,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        return EntityFrameContext.builder()
                .frameCounter(frameCounter)
                .linkEntityX(linkEntityX)
                .linkEntityY(linkEntityY)
                .collisionType(collisionType)
                .randomByteSupplier(randomByteSupplier)
                .backgroundCollision(backgroundCollision)
                .objectCollision(linksawakening.world.entity.RoomEntityObjectCollision.NONE)
                .linkPositionHistory(null)
                .linkZ(0)
                .linkAirborne(false)
                .linkDirection(0)
                .entityYOffset(0)
                .projectileLinkState(EnemyProjectileCollision.LinkState.nonInteractive())
                .swordCollisionActive(false)
                .swordX(0)
                .swordWidth(0)
                .swordY(0)
                .swordHeight(0)
                .linkSpeedX(0)
                .linkSpeedY(0)
                .creditsGameplay(false)
                .handlerLinkCollisionEnabled(true)
                .build();
    }

    public static EntityFrameContext frame(
            RoomEntityRuntime runtime,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            boolean creditsGameplay) {
        return EntityFrameContext.builder()
                .frameCounter(frameCounter)
                .linkEntityX(linkEntityX)
                .linkEntityY(linkEntityY)
                .collisionType(0)
                .randomByteSupplier(randomByteSupplier)
                .backgroundCollision(linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN)
                .objectCollision(linksawakening.world.entity.RoomEntityObjectCollision.NONE)
                .linkPositionHistory(null)
                .linkZ(0)
                .linkAirborne(false)
                .linkDirection(0)
                .entityYOffset(0)
                .projectileLinkState(EnemyProjectileCollision.LinkState.nonInteractive())
                .swordCollisionActive(false)
                .swordX(0)
                .swordWidth(0)
                .swordY(0)
                .swordHeight(0)
                .linkSpeedX(0)
                .linkSpeedY(0)
                .creditsGameplay(creditsGameplay)
                .handlerLinkCollisionEnabled(true)
                .build();
    }

    public static EntityFrameContext frame(
            RoomEntityRuntime runtime,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            LinkPositionHistory linkPositionHistory,
            int linkZ,
            int linkDirection,
            int entityYOffset) {
        return EntityFrameContext.builder()
                .frameCounter(frameCounter)
                .linkEntityX(linkEntityX)
                .linkEntityY(linkEntityY)
                .collisionType(0)
                .randomByteSupplier(randomByteSupplier)
                .backgroundCollision(backgroundCollision)
                .objectCollision(linksawakening.world.entity.RoomEntityObjectCollision.NONE)
                .linkPositionHistory(linkPositionHistory)
                .linkZ(linkZ)
                .linkAirborne(false)
                .linkDirection(linkDirection)
                .entityYOffset(entityYOffset)
                .projectileLinkState(EnemyProjectileCollision.LinkState.nonInteractive())
                .swordCollisionActive(false)
                .swordX(0)
                .swordWidth(0)
                .swordY(0)
                .swordHeight(0)
                .linkSpeedX(0)
                .linkSpeedY(0)
                .creditsGameplay(false)
                .handlerLinkCollisionEnabled(true)
                .build();
    }

    public static EntityFrameContext frame(
            RoomEntityRuntime runtime,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int collisionType,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            LinkPositionHistory linkPositionHistory,
            int linkZ,
            int linkDirection,
            int entityYOffset) {
        return EntityFrameContext.builder()
                .frameCounter(frameCounter)
                .linkEntityX(linkEntityX)
                .linkEntityY(linkEntityY)
                .collisionType(collisionType)
                .randomByteSupplier(randomByteSupplier)
                .backgroundCollision(backgroundCollision)
                .objectCollision(linksawakening.world.entity.RoomEntityObjectCollision.NONE)
                .linkPositionHistory(linkPositionHistory)
                .linkZ(linkZ)
                .linkAirborne(false)
                .linkDirection(linkDirection)
                .entityYOffset(entityYOffset)
                .projectileLinkState(EnemyProjectileCollision.LinkState.nonInteractive())
                .swordCollisionActive(false)
                .swordX(0)
                .swordWidth(0)
                .swordY(0)
                .swordHeight(0)
                .linkSpeedX(0)
                .linkSpeedY(0)
                .creditsGameplay(false)
                .handlerLinkCollisionEnabled(true)
                .build();
    }

    public static EntityFrameContext projectileFrame(
            RoomEntityRuntime runtime,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            EnemyProjectileCollision.LinkState projectileLinkState) {
        return EntityFrameContext.builder()
                .frameCounter(frameCounter)
                .linkEntityX(linkEntityX)
                .linkEntityY(linkEntityY)
                .collisionType(0)
                .randomByteSupplier(randomByteSupplier)
                .backgroundCollision(backgroundCollision)
                .objectCollision(linksawakening.world.entity.RoomEntityObjectCollision.NONE)
                .linkPositionHistory(null)
                .linkZ(projectileLinkState.z())
                .linkAirborne(false)
                .linkDirection(projectileLinkState.direction())
                .entityYOffset(0)
                .projectileLinkState(projectileLinkState)
                .swordCollisionActive(false)
                .swordX(0)
                .swordWidth(0)
                .swordY(0)
                .swordHeight(0)
                .linkSpeedX(runtime.inspection().linkState.readCurrentLinkSpeedX())
                .linkSpeedY(runtime.inspection().linkState.readCurrentLinkSpeedY())
                .creditsGameplay(false)
                .handlerLinkCollisionEnabled(
                        projectileLinkState.motionState() < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE)
                .build();
    }

    public static EntityFrameContext projectileFrame(
            RoomEntityRuntime runtime,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            EnemyProjectileCollision.LinkState projectileLinkState,
            int linkSpeedX,
            int linkSpeedY) {
        return projectileFrame(
                        runtime,
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        randomByteSupplier,
                        backgroundCollision,
                        projectileLinkState)
                .withLinkSpeeds(linkSpeedX, linkSpeedY);
    }

    public static EntityFrameContext projectileFrame(
            RoomEntityRuntime runtime,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            EnemyProjectileCollision.LinkState projectileLinkState,
            boolean swordCollisionActive,
            int swordX,
            int swordWidth,
            int swordY,
            int swordHeight) {
        return EntityFrameContext.builder()
                .frameCounter(frameCounter)
                .linkEntityX(linkEntityX)
                .linkEntityY(linkEntityY)
                .collisionType(0)
                .randomByteSupplier(randomByteSupplier)
                .backgroundCollision(backgroundCollision)
                .objectCollision(linksawakening.world.entity.RoomEntityObjectCollision.NONE)
                .linkPositionHistory(null)
                .linkZ(projectileLinkState.z())
                .linkAirborne(false)
                .linkDirection(projectileLinkState.direction())
                .entityYOffset(0)
                .projectileLinkState(projectileLinkState)
                .swordCollisionActive(swordCollisionActive)
                .swordX(swordX)
                .swordWidth(swordWidth)
                .swordY(swordY)
                .swordHeight(swordHeight)
                .linkSpeedX(runtime.inspection().linkState.readCurrentLinkSpeedX())
                .linkSpeedY(runtime.inspection().linkState.readCurrentLinkSpeedY())
                .creditsGameplay(false)
                .handlerLinkCollisionEnabled(
                        projectileLinkState.motionState() < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE)
                .build();
    }

    public static EntityFrameContext projectileFrame(
            RoomEntityRuntime runtime,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            RoomEntityObjectCollision objectCollision,
            EnemyProjectileCollision.LinkState projectileLinkState,
            boolean swordCollisionActive,
            int swordX,
            int swordWidth,
            int swordY,
            int swordHeight) {
        return EntityFrameContext.builder()
                .frameCounter(frameCounter)
                .linkEntityX(linkEntityX)
                .linkEntityY(linkEntityY)
                .collisionType(0)
                .randomByteSupplier(randomByteSupplier)
                .backgroundCollision(backgroundCollision)
                .objectCollision(objectCollision)
                .linkPositionHistory(null)
                .linkZ(projectileLinkState.z())
                .linkAirborne(false)
                .linkDirection(projectileLinkState.direction())
                .entityYOffset(0)
                .projectileLinkState(projectileLinkState)
                .swordCollisionActive(swordCollisionActive)
                .swordX(swordX)
                .swordWidth(swordWidth)
                .swordY(swordY)
                .swordHeight(swordHeight)
                .linkSpeedX(runtime.inspection().linkState.readCurrentLinkSpeedX())
                .linkSpeedY(runtime.inspection().linkState.readCurrentLinkSpeedY())
                .creditsGameplay(false)
                .handlerLinkCollisionEnabled(
                        projectileLinkState.motionState() < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE)
                .build();
    }

    public static EntityFrameContext projectileFrame(
            RoomEntityRuntime runtime,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int collisionType,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            RoomEntityObjectCollision objectCollision,
            LinkPositionHistory linkPositionHistory,
            int linkZ,
            int linkDirection,
            int entityYOffset,
            EnemyProjectileCollision.LinkState projectileLinkState,
            boolean swordCollisionActive,
            int swordX,
            int swordWidth,
            int swordY,
            int swordHeight) {
        return EntityFrameContext.builder()
                .frameCounter(frameCounter)
                .linkEntityX(linkEntityX)
                .linkEntityY(linkEntityY)
                .collisionType(collisionType)
                .randomByteSupplier(randomByteSupplier)
                .backgroundCollision(backgroundCollision)
                .objectCollision(objectCollision)
                .linkPositionHistory(linkPositionHistory)
                .linkZ(linkZ)
                .linkAirborne(false)
                .linkDirection(linkDirection)
                .entityYOffset(entityYOffset)
                .projectileLinkState(projectileLinkState)
                .swordCollisionActive(swordCollisionActive)
                .swordX(swordX)
                .swordWidth(swordWidth)
                .swordY(swordY)
                .swordHeight(swordHeight)
                .linkSpeedX(runtime.inspection().linkState.readCurrentLinkSpeedX())
                .linkSpeedY(runtime.inspection().linkState.readCurrentLinkSpeedY())
                .creditsGameplay(false)
                .handlerLinkCollisionEnabled(
                        projectileLinkState.motionState() < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE)
                .build();
    }

    public static EntityFrameContext projectileFrame(
            RoomEntityRuntime runtime,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int collisionType,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            RoomEntityObjectCollision objectCollision,
            LinkPositionHistory linkPositionHistory,
            int linkZ,
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
        return EntityFrameContext.builder()
                .frameCounter(frameCounter)
                .linkEntityX(linkEntityX)
                .linkEntityY(linkEntityY)
                .collisionType(collisionType)
                .randomByteSupplier(randomByteSupplier)
                .backgroundCollision(backgroundCollision)
                .objectCollision(objectCollision)
                .linkPositionHistory(linkPositionHistory)
                .linkZ(linkZ)
                .linkAirborne(false)
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
                .build();
    }

    public static EntityFrameContext projectileFrame(
            RoomEntityRuntime runtime,
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
        return EntityFrameContext.builder()
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
                .build();
    }

    public static EntityFrameContext projectileFrame(
            RoomEntityRuntime runtime,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            LinkPositionHistory linkPositionHistory,
            int linkZ,
            int linkDirection,
            int entityYOffset,
            EnemyProjectileCollision.LinkState projectileLinkState) {
        return EntityFrameContext.builder()
                .frameCounter(frameCounter)
                .linkEntityX(linkEntityX)
                .linkEntityY(linkEntityY)
                .collisionType(0)
                .randomByteSupplier(randomByteSupplier)
                .backgroundCollision(backgroundCollision)
                .objectCollision(linksawakening.world.entity.RoomEntityObjectCollision.NONE)
                .linkPositionHistory(linkPositionHistory)
                .linkZ(linkZ)
                .linkAirborne(false)
                .linkDirection(linkDirection)
                .entityYOffset(entityYOffset)
                .projectileLinkState(projectileLinkState)
                .swordCollisionActive(false)
                .swordX(0)
                .swordWidth(0)
                .swordY(0)
                .swordHeight(0)
                .linkSpeedX(runtime.inspection().linkState.readCurrentLinkSpeedX())
                .linkSpeedY(runtime.inspection().linkState.readCurrentLinkSpeedY())
                .creditsGameplay(false)
                .handlerLinkCollisionEnabled(
                        projectileLinkState.motionState() < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE)
                .build();
    }

    public static EntityFrameContext projectileFrame(
            RoomEntityRuntime runtime,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            LinkPositionHistory linkPositionHistory,
            int linkZ,
            int linkDirection,
            int entityYOffset,
            EnemyProjectileCollision.LinkState projectileLinkState,
            boolean swordCollisionActive,
            int swordX,
            int swordWidth,
            int swordY,
            int swordHeight) {
        return EntityFrameContext.builder()
                .frameCounter(frameCounter)
                .linkEntityX(linkEntityX)
                .linkEntityY(linkEntityY)
                .collisionType(0)
                .randomByteSupplier(randomByteSupplier)
                .backgroundCollision(backgroundCollision)
                .objectCollision(linksawakening.world.entity.RoomEntityObjectCollision.NONE)
                .linkPositionHistory(linkPositionHistory)
                .linkZ(linkZ)
                .linkAirborne(false)
                .linkDirection(linkDirection)
                .entityYOffset(entityYOffset)
                .projectileLinkState(projectileLinkState)
                .swordCollisionActive(swordCollisionActive)
                .swordX(swordX)
                .swordWidth(swordWidth)
                .swordY(swordY)
                .swordHeight(swordHeight)
                .linkSpeedX(runtime.inspection().linkState.readCurrentLinkSpeedX())
                .linkSpeedY(runtime.inspection().linkState.readCurrentLinkSpeedY())
                .creditsGameplay(false)
                .handlerLinkCollisionEnabled(
                        projectileLinkState.motionState() < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE)
                .build();
    }

    public static EntityFrameContext projectileFrame(
            RoomEntityRuntime runtime,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            RoomEntityObjectCollision objectCollision,
            LinkPositionHistory linkPositionHistory,
            int linkZ,
            int linkDirection,
            int entityYOffset,
            EnemyProjectileCollision.LinkState projectileLinkState,
            boolean swordCollisionActive,
            int swordX,
            int swordWidth,
            int swordY,
            int swordHeight) {
        return EntityFrameContext.builder()
                .frameCounter(frameCounter)
                .linkEntityX(linkEntityX)
                .linkEntityY(linkEntityY)
                .collisionType(0)
                .randomByteSupplier(randomByteSupplier)
                .backgroundCollision(backgroundCollision)
                .objectCollision(objectCollision)
                .linkPositionHistory(linkPositionHistory)
                .linkZ(linkZ)
                .linkAirborne(false)
                .linkDirection(linkDirection)
                .entityYOffset(entityYOffset)
                .projectileLinkState(projectileLinkState)
                .swordCollisionActive(swordCollisionActive)
                .swordX(swordX)
                .swordWidth(swordWidth)
                .swordY(swordY)
                .swordHeight(swordHeight)
                .linkSpeedX(runtime.inspection().linkState.readCurrentLinkSpeedX())
                .linkSpeedY(runtime.inspection().linkState.readCurrentLinkSpeedY())
                .creditsGameplay(false)
                .handlerLinkCollisionEnabled(
                        projectileLinkState.motionState() < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE)
                .build();
    }
}
