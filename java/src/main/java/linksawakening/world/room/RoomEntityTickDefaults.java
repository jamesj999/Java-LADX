package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

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
                .tickEntities(new EntityTickInputs(
                        frameCounter,
                        new LinkPosition(linkEntityX, linkEntityY, linkEntityZ, linkDirection, 0),
                        new LinkMotion(false, linkMotionState, usingShield, 0, 0, 0),
                        1,
                        new SwordBox(false, 0, 0, 0, 0)));
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
                .tickEntities(new EntityTickInputs(
                        frameCounter,
                        new LinkPosition(linkEntityX, linkEntityY, linkEntityZ, linkDirection, 0),
                        new LinkMotion(linkAirborne, linkMotionState, usingShield, 0, 0, 0),
                        1,
                        new SwordBox(false, 0, 0, 0, 0)));
    }

    record Dependencies(java.util.function.Supplier<RoomEntityBridge> entityBridge, RoomFollowerState followers) {}

    List<EntityProjectileEvent> tickEntities(EntityTickInputs inputs) {
        return dependencies.entityBridge().get().tickEntities(inputs);
    }
}
