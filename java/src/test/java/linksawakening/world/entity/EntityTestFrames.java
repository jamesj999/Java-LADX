package linksawakening.world.entity;



import linksawakening.world.entity.RoomEntityBackgroundCollision;
import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.RoomEntityObjectCollision;
import linksawakening.world.entity.EntityFrameContext;

import java.util.function.IntSupplier;

/** Frame input fixtures for runtime tests. */
public final class EntityTestFrames {
    public static EntityFrameContext frame(RoomEntityRuntime runtime, int frameCounter) {
        return runtime.frameContext(frameCounter);
    }

    public static EntityFrameContext frame(RoomEntityRuntime runtime, int frameCounter, int linkEntityX, int linkEntityY,
                     IntSupplier randomByteSupplier) {
        return new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier, null, null, null, 0, false, 0, 0, EnemyProjectileCollision.LinkState.nonInteractive(), false, 0, 0, 0, 0, 0, 0, false, true);
    }

    public static EntityFrameContext frame(RoomEntityRuntime runtime, int frameCounter, int linkEntityX, int linkEntityY,
                     int collisionType, IntSupplier randomByteSupplier) {
        return new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, collisionType, randomByteSupplier, null, null, null, 0, false, 0, 0, EnemyProjectileCollision.LinkState.nonInteractive(), false, 0, 0, 0, 0, 0, 0, false, true);
    }

    public static EntityFrameContext frame(RoomEntityRuntime runtime, int frameCounter, int linkEntityX, int linkEntityY,
                     IntSupplier randomByteSupplier,
                     RoomEntityBackgroundCollision backgroundCollision) {
        return new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier, backgroundCollision, null, null, 0, false, 0, 0, EnemyProjectileCollision.LinkState.nonInteractive(), false, 0, 0, 0, 0, 0, 0, false, true);
    }

    public static EntityFrameContext frame(RoomEntityRuntime runtime, int frameCounter, int linkEntityX, int linkEntityY,
                     int collisionType, IntSupplier randomByteSupplier,
                     RoomEntityBackgroundCollision backgroundCollision) {
        return new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, collisionType, randomByteSupplier, backgroundCollision, null, null, 0, false, 0, 0, EnemyProjectileCollision.LinkState.nonInteractive(), false, 0, 0, 0, 0, 0, 0, false, true);
    }

    public static EntityFrameContext frame(RoomEntityRuntime runtime, int frameCounter, int linkEntityX, int linkEntityY,
                     IntSupplier randomByteSupplier, boolean creditsGameplay) {
        return new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier, null, null, null, 0, false, 0, 0, EnemyProjectileCollision.LinkState.nonInteractive(), false, 0, 0, 0, 0, 0, 0, creditsGameplay, true);
    }

    public static EntityFrameContext frame(RoomEntityRuntime runtime, int frameCounter, int linkEntityX, int linkEntityY,
              IntSupplier randomByteSupplier,
              RoomEntityBackgroundCollision backgroundCollision,
              LinkPositionHistory linkPositionHistory,
              int linkZ, int linkDirection, int entityYOffset) {
        return new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier, backgroundCollision, null, linkPositionHistory, linkZ, false, linkDirection, entityYOffset, EnemyProjectileCollision.LinkState.nonInteractive(), false, 0, 0, 0, 0, 0, 0, false, true);
    }

    public static EntityFrameContext frame(RoomEntityRuntime runtime, int frameCounter, int linkEntityX, int linkEntityY, int collisionType,
              IntSupplier randomByteSupplier,
              RoomEntityBackgroundCollision backgroundCollision,
              LinkPositionHistory linkPositionHistory,
              int linkZ, int linkDirection, int entityYOffset) {
        return new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, collisionType, randomByteSupplier, backgroundCollision, null, linkPositionHistory, linkZ, false, linkDirection, entityYOffset, EnemyProjectileCollision.LinkState.nonInteractive(), false, 0, 0, 0, 0, 0, 0, false, true);
    }

    public static EntityFrameContext projectileFrame(RoomEntityRuntime runtime, int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        EnemyProjectileCollision.LinkState projectileLinkState) {
        return new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier, backgroundCollision, null, null, projectileLinkState.z(), false, projectileLinkState.direction(), 0, projectileLinkState, false, 0, 0, 0, 0, runtime.currentLinkSpeedX, runtime.currentLinkSpeedY, false, projectileLinkState.motionState() < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE);
    }

    public static EntityFrameContext projectileFrame(RoomEntityRuntime runtime, int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        EnemyProjectileCollision.LinkState projectileLinkState,
        int linkSpeedX, int linkSpeedY) {
        return projectileFrame(runtime, frameCounter, linkEntityX, linkEntityY, randomByteSupplier, backgroundCollision, projectileLinkState).withLinkSpeeds(linkSpeedX, linkSpeedY);
    }

    public static EntityFrameContext projectileFrame(RoomEntityRuntime runtime, int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        EnemyProjectileCollision.LinkState projectileLinkState,
        boolean swordCollisionActive, int swordX, int swordWidth,
        int swordY, int swordHeight) {
        return new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier, backgroundCollision, null, null, projectileLinkState.z(), false, projectileLinkState.direction(), 0, projectileLinkState, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, runtime.currentLinkSpeedX, runtime.currentLinkSpeedY, false, projectileLinkState.motionState()
                < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE);
    }

    public static EntityFrameContext projectileFrame(RoomEntityRuntime runtime, int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        RoomEntityObjectCollision objectCollision,
        EnemyProjectileCollision.LinkState projectileLinkState,
        boolean swordCollisionActive, int swordX, int swordWidth,
        int swordY, int swordHeight) {
        return new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier, backgroundCollision, objectCollision, null, projectileLinkState.z(), false, projectileLinkState.direction(), 0, projectileLinkState, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, runtime.currentLinkSpeedX, runtime.currentLinkSpeedY, false, projectileLinkState.motionState()
                < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE);
    }

    public static EntityFrameContext projectileFrame(RoomEntityRuntime runtime, int frameCounter, int linkEntityX, int linkEntityY, int collisionType,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        RoomEntityObjectCollision objectCollision,
        LinkPositionHistory linkPositionHistory,
        int linkZ, int linkDirection, int entityYOffset,
        EnemyProjectileCollision.LinkState projectileLinkState,
        boolean swordCollisionActive, int swordX, int swordWidth,
        int swordY, int swordHeight) {
        return new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, collisionType, randomByteSupplier, backgroundCollision, objectCollision, linkPositionHistory, linkZ, false, linkDirection, entityYOffset, projectileLinkState, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, runtime.currentLinkSpeedX, runtime.currentLinkSpeedY, false, projectileLinkState.motionState()
                < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE);
    }

    public static EntityFrameContext projectileFrame(RoomEntityRuntime runtime, int frameCounter, int linkEntityX, int linkEntityY, int collisionType,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        RoomEntityObjectCollision objectCollision,
        LinkPositionHistory linkPositionHistory,
        int linkZ, int linkDirection, int entityYOffset,
        EnemyProjectileCollision.LinkState projectileLinkState,
        boolean swordCollisionActive, int swordX, int swordWidth,
        int swordY, int swordHeight, int linkSpeedX, int linkSpeedY) {
        return new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, collisionType, randomByteSupplier, backgroundCollision, objectCollision, linkPositionHistory, linkZ, false, linkDirection, entityYOffset, projectileLinkState, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, linkSpeedX, linkSpeedY);
    }

    public static EntityFrameContext projectileFrame(RoomEntityRuntime runtime, int frameCounter, int linkEntityX, int linkEntityY, int collisionType,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        RoomEntityObjectCollision objectCollision,
        LinkPositionHistory linkPositionHistory,
        int linkZ, boolean linkAirborne, int linkDirection, int entityYOffset,
        EnemyProjectileCollision.LinkState projectileLinkState,
        boolean swordCollisionActive, int swordX, int swordWidth,
        int swordY, int swordHeight, int linkSpeedX, int linkSpeedY) {
        return new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, collisionType, randomByteSupplier, backgroundCollision, objectCollision, linkPositionHistory, linkZ, linkAirborne, linkDirection, entityYOffset, projectileLinkState, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, linkSpeedX, linkSpeedY);
    }

    public static EntityFrameContext projectileFrame(RoomEntityRuntime runtime, int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        LinkPositionHistory linkPositionHistory,
        int linkZ, int linkDirection, int entityYOffset,
        EnemyProjectileCollision.LinkState projectileLinkState) {
        return new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier, backgroundCollision, null, linkPositionHistory, linkZ, false, linkDirection, entityYOffset, projectileLinkState, false, 0, 0, 0, 0, runtime.currentLinkSpeedX, runtime.currentLinkSpeedY, false, projectileLinkState.motionState()
                < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE);
    }

    public static EntityFrameContext projectileFrame(RoomEntityRuntime runtime, int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        LinkPositionHistory linkPositionHistory,
        int linkZ, int linkDirection, int entityYOffset,
        EnemyProjectileCollision.LinkState projectileLinkState,
        boolean swordCollisionActive, int swordX, int swordWidth,
        int swordY, int swordHeight) {
        return new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier, backgroundCollision, null, linkPositionHistory, linkZ, false, linkDirection, entityYOffset, projectileLinkState, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, runtime.currentLinkSpeedX, runtime.currentLinkSpeedY, false, projectileLinkState.motionState()
                < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE);
    }

    public static EntityFrameContext projectileFrame(RoomEntityRuntime runtime, int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        RoomEntityObjectCollision objectCollision,
        LinkPositionHistory linkPositionHistory,
        int linkZ, int linkDirection, int entityYOffset,
        EnemyProjectileCollision.LinkState projectileLinkState,
        boolean swordCollisionActive, int swordX, int swordWidth,
        int swordY, int swordHeight) {
        return new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier, backgroundCollision, objectCollision, linkPositionHistory, linkZ, false, linkDirection, entityYOffset, projectileLinkState, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, runtime.currentLinkSpeedX, runtime.currentLinkSpeedY, false, projectileLinkState.motionState()
                < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE);
    }
}
