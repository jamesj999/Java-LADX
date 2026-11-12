package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import java.util.List;
import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.EntityFrameContext;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.entity.event.EntityProjectileEvent;

final class RoomEntitySimulation {
    private final Dependencies dependencies;

    RoomEntitySimulation(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            boolean linkAirborne,
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
        dependencies.followers().followingLinkX = linkEntityX & 0xFF;
        dependencies.followers().followingLinkY = linkEntityY & 0xFF;
        dependencies.followers().followingLinkZ = linkEntityZ & 0xFF;
        dependencies.followers().followingLinkDirection = linkDirection & 0xFF;
        dependencies.entityFrame().currentLinkMotionState = linkMotionState & 0xFF;
        dependencies.location().shouldGetLostInMysteriousWoods = false;
        if (dependencies.location().activeRoom == null
                || dependencies.entityBridge().get().entityRuntime == null) {
            return List.of();
        }
        dependencies.owner().get().frameSetup.configure();
        // rLY is not a meaningful value in the host renderer. Keep the
        // non-emulator policy explicit while preserving the ROM seed update.
        dependencies.entityRandomByteSource().beginFrame(frameCounter & 0xFF, 0);
        spawnPendingShovelDrop();
        List<EntityProjectileEvent> events = dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .tick(new EntityFrameContext(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        collisionType & 0xFF,
                        dependencies.entityRandomByteSource(),
                        dependencies.roomCollisionQueries().get()::entityBackgroundCollision,
                        dependencies.roomCollisionQueries().get()::pairoddProjectileObjectCollision,
                        dependencies.followers().followingLinkPositionHistory,
                        dependencies.followers().followingLinkZ,
                        linkAirborne,
                        dependencies.followers().followingLinkDirection,
                        dependencies.followers().followingEntityYOffset,
                        new EnemyProjectileCollision.LinkState(
                                linkEntityX,
                                linkEntityY,
                                linkEntityZ,
                                linkMotionState,
                                RoomInteractionGeometry.romDirectionForProjectileCollision(linkDirection),
                                usingShield,
                                shieldLevel,
                                invincibilityCounter),
                        swordCollisionActive,
                        swordX,
                        swordWidth,
                        swordY,
                        swordHeight,
                        linkSpeedX,
                        linkSpeedY));
        dependencies.owner().get().afterTick.complete(linkEntityX, linkEntityY);
        return events;
    }

    record Dependencies(
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            RoomEntityFrameState entityFrame,
            RomRandomByteSource entityRandomByteSource,
            RoomFollowerState followers,
            RoomItemUseState items,
            RoomLocationState location,
            java.util.function.Supplier<RoomCollisionQueries> roomCollisionQueries,
            java.util.function.Supplier<RoomEntityBridge> owner) {}

    private void spawnPendingShovelDrop() {
        if (dependencies.items().pendingShovelDrop != null) {
            PendingShovelDrop drop = dependencies.items().pendingShovelDrop;
            dependencies.items().pendingShovelDrop = null;
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .spawnShovelDrop(
                            drop.objectLeft(),
                            drop.objectTop(),
                            drop.linkEntityX(),
                            drop.linkEntityY(),
                            drop.allowDrop());
        }
    }
}
