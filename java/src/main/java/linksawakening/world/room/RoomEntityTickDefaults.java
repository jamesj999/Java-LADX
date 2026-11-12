package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import java.util.List;
import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.event.EntityProjectileEvent;

final class RoomEntityTickDefaults {
    private final Dependencies dependencies;

    RoomEntityTickDefaults(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    void tickEntities(int frameCounter) {
        dependencies.entityBridge().get().tickEntities(frameCounter, 0, 0);
    }

    void tickEntities(int frameCounter, int linkEntityX, int linkEntityY) {
        dependencies
                .entityBridge()
                .get()
                .tickEntities(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        dependencies.followers().followingLinkZ,
                        dependencies.followers().followingLinkDirection);
    }

    void tickEntities(int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkDirection) {
        dependencies
                .entityBridge()
                .get()
                .tickEntitiesWithProjectileEvents(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkEntityZ,
                        EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE,
                        linkDirection,
                        false);
    }

    void tickEntities(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            boolean linkAirborne,
            int linkDirection) {
        dependencies
                .entityBridge()
                .get()
                .tickEntitiesWithProjectileEvents(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkEntityZ,
                        linkAirborne,
                        EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE,
                        linkDirection,
                        false);
    }

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int linkMotionState,
            int linkDirection,
            boolean usingShield) {
        return dependencies
                .entityBridge()
                .get()
                .tickEntitiesWithProjectileEvents(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkEntityZ,
                        linkMotionState,
                        linkDirection,
                        usingShield,
                        1,
                        0);
    }

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            boolean linkAirborne,
            int linkMotionState,
            int linkDirection,
            boolean usingShield) {
        return dependencies
                .entityBridge()
                .get()
                .tickEntitiesWithProjectileEvents(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkEntityZ,
                        linkAirborne,
                        linkMotionState,
                        linkDirection,
                        0,
                        usingShield,
                        1,
                        0,
                        false,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0);
    }

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int linkMotionState,
            int linkDirection,
            boolean usingShield,
            int shieldLevel,
            int invincibilityCounter) {
        return dependencies
                .entityBridge()
                .get()
                .tickEntitiesWithProjectileEvents(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkEntityZ,
                        linkMotionState,
                        linkDirection,
                        usingShield,
                        shieldLevel,
                        invincibilityCounter,
                        false,
                        0,
                        0,
                        0,
                        0);
    }

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int linkMotionState,
            int linkDirection,
            boolean usingShield,
            int shieldLevel,
            int invincibilityCounter,
            boolean swordCollisionActive,
            int swordX,
            int swordWidth,
            int swordY,
            int swordHeight) {
        return dependencies
                .entityBridge()
                .get()
                .tickEntitiesWithProjectileEvents(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkEntityZ,
                        linkMotionState,
                        linkDirection,
                        0,
                        usingShield,
                        shieldLevel,
                        invincibilityCounter,
                        swordCollisionActive,
                        swordX,
                        swordWidth,
                        swordY,
                        swordHeight);
    }

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int linkMotionState,
            int linkDirection,
            int collisionType,
            boolean usingShield,
            int shieldLevel,
            int invincibilityCounter,
            boolean swordCollisionActive,
            int swordX,
            int swordWidth,
            int swordY,
            int swordHeight) {
        return dependencies
                .entityBridge()
                .get()
                .tickEntitiesWithProjectileEvents(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkEntityZ,
                        linkMotionState,
                        linkDirection,
                        collisionType,
                        usingShield,
                        shieldLevel,
                        invincibilityCounter,
                        swordCollisionActive,
                        swordX,
                        swordWidth,
                        swordY,
                        swordHeight,
                        0,
                        0);
    }

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int linkMotionState,
            int linkDirection,
            int collisionType,
            boolean usingShield,
            int shieldLevel,
            int invincibilityCounter,
            boolean swordCollisionActive,
            int swordX,
            int swordWidth,
            int swordY,
            int swordHeight,
            int linkSpeedX,
            int linkSpeedY) {
        return dependencies
                .entityBridge()
                .get()
                .tickEntitiesWithProjectileEvents(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkEntityZ,
                        false,
                        linkMotionState,
                        linkDirection,
                        collisionType,
                        usingShield,
                        shieldLevel,
                        invincibilityCounter,
                        swordCollisionActive,
                        swordX,
                        swordWidth,
                        swordY,
                        swordHeight,
                        linkSpeedX,
                        linkSpeedY);
    }

    record Dependencies(java.util.function.Supplier<RoomEntityBridge> entityBridge, RoomFollowerState followers) {}
}
