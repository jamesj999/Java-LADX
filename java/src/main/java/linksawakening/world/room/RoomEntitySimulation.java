package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import java.util.List;
import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.entity.event.EntityProjectileEvent;

final class RoomEntitySimulation {
    private final Dependencies dependencies;
    private final linksawakening.world.entity.EntityFrameContextBuilder frameInputs =
            linksawakening.world.entity.EntityFrameContext.builder();
    private final linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
    private final linksawakening.world.entity.RoomEntityObjectCollision objectCollision;

    RoomEntitySimulation(Dependencies dependencies) {
        this.dependencies = dependencies;
        backgroundCollision = (entity, direction, x, y) ->
                dependencies.roomCollisionQueries().get().entityBackgroundCollision(entity, direction, x, y);
        objectCollision = entity -> dependencies.roomCollisionQueries().get().pairoddProjectileObjectCollision(entity);
    }

    List<EntityProjectileEvent> tickEntities(EntityTickInputs inputs) {
        int frameCounter = inputs.frameCounter();
        int linkEntityX = inputs.position().x();
        int linkEntityY = inputs.position().y();
        int linkEntityZ = inputs.position().z();
        boolean linkAirborne = inputs.motion().airborne();
        int linkMotionState = inputs.motion().rawState();
        int linkDirection = inputs.position().rawDirection();
        int collisionType = inputs.position().collisionType();
        boolean usingShield = inputs.motion().shield();
        int shieldLevel = inputs.shieldLevel();
        int invincibilityCounter = inputs.motion().invincibility();
        boolean swordCollisionActive = inputs.sword().active();
        int swordX = inputs.sword().x();
        int swordWidth = inputs.sword().width();
        int swordY = inputs.sword().y();
        int swordHeight = inputs.sword().height();
        int linkSpeedX = inputs.motion().speedX();
        int linkSpeedY = inputs.motion().speedY();

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
                .tick(frameInputs
                        .frameCounter(frameCounter)
                        .linkEntityX(linkEntityX)
                        .linkEntityY(linkEntityY)
                        .collisionType(collisionType & 0xFF)
                        .randomByteSupplier(dependencies.entityRandomByteSource())
                        .backgroundCollision(backgroundCollision)
                        .objectCollision(objectCollision)
                        .linkPositionHistory(dependencies.followers().followingLinkPositionHistory)
                        .linkZ(dependencies.followers().followingLinkZ)
                        .linkAirborne(linkAirborne)
                        .linkDirection(dependencies.followers().followingLinkDirection)
                        .entityYOffset(dependencies.followers().followingEntityYOffset)
                        .projectileLinkState(new EnemyProjectileCollision.LinkState(
                                linkEntityX,
                                linkEntityY,
                                linkEntityZ,
                                linkMotionState,
                                RoomInteractionGeometry.romDirectionForProjectileCollision(linkDirection),
                                usingShield,
                                shieldLevel,
                                invincibilityCounter))
                        .swordCollisionActive(swordCollisionActive)
                        .swordX(swordX)
                        .swordWidth(swordWidth)
                        .swordY(swordY)
                        .swordHeight(swordHeight)
                        .linkSpeedX(linkSpeedX)
                        .linkSpeedY(linkSpeedY)
                        .build());
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
