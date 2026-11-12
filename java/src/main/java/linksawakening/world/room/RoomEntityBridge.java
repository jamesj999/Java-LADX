package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import java.util.List;
import linksawakening.gpu.GPU;
import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.EntityFrameContext;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.object.SwitchBlockAnimation;

/** Owns room-level entity output queues and their consumption boundary. */
final class RoomEntityBridge {
    RoomEntityRuntime entityRuntime;
    final RoomEntityOutbox outbox = new RoomEntityOutbox(() -> entityRuntime);
    final RoomEntityCombatBridge combat;
    private final RoomEntityFrameSetup frameSetup;
    private final RoomEntityAfterTick afterTick;
    private final Dependencies dependencies;

    RoomEntityBridge(Dependencies dependencies) {
        this.dependencies = dependencies;
        afterTick = dependencies.entityAfterTick().get();
        frameSetup = dependencies.entityFrameSetup().get();
        combat = dependencies.entityCombatBridge().get();
    }

    void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp) {
        if (maxHearts < 0 || maxHearts > 0xFF) {
            throw new IllegalArgumentException("Maximum hearts must be an unsigned byte: " + maxHearts);
        }
        if (health < 0 || health > 0xFF) {
            throw new IllegalArgumentException("Health must be an unsigned byte: " + health);
        }
        dependencies.entityFrame().enemyDropMaxHearts = maxHearts;
        dependencies.entityFrame().enemyDropHealth = health;
        dependencies.entityFrame().enemyDropLowHealth = null;
        dependencies.entityFrame().enemyDropActivePowerUp = activePowerUp;
        dependencies.roomLoadCoordinator().get().configureEnemyDropRuntime();
    }

    void advanceBoomerangSoundClock(boolean worldGameplay) {
        if (worldGameplay) {
            dependencies.entityFrame().boomerangSoundCounter = (dependencies.entityFrame().boomerangSoundCounter + 1)
                    % RoomEntityRuntime.BOOMERANG_SFX_COUNTER_PERIOD;
        }
        // Main may run several world frames without an entity pass during a
        // transition. The next pass must use that clock without ticking twice.
        dependencies.entityFrame().boomerangSoundClockProvided = true;
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
        frameSetup.configure();
        // rLY is not a meaningful value in the host renderer. Keep the
        // non-emulator policy explicit while preserving the ROM seed update.
        dependencies.entityRandomByteSource().beginFrame(frameCounter & 0xFF, 0);
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
        afterTick.complete(linkEntityX, linkEntityY);
        return events;
    }

    List<WarpLinkStateRequest> consumeWarpLinkStateRequests() {
        List<WarpLinkStateRequest> requests = dependencies.entityBridge().get().entityRuntime == null
                ? List.of()
                : dependencies.entityBridge().get().entityRuntime.consumePendingWarpLinkStateRequests();
        if (!requests.isEmpty()) {
            dependencies.items().ocarinaPlaybackCountdown = 0;
            dependencies.items().ocarinaAnimationCounter = 0;
            dependencies.items().ocarinaAnimationPhase = 0;
            dependencies.entityInputs().get().entityDialogActive = false;
            dependencies.entityInputs().get().entityDialogCooldown = 0;
            dependencies.entityInputs().get().entityInventoryAppearing = false;
        }
        return requests;
    }

    boolean consumeWorldLinkMotionBlockRequest() {
        boolean pending = dependencies.entityFrame().worldLinkMotionBlockPending;
        dependencies.entityFrame().worldLinkMotionBlockPending = false;
        return pending;
    }

    void tickGameplayVBlank() {
        if (SwitchBlockAnimation.isAnimating(dependencies.terrain().switchableObjectAnimationStage)) {
            SwitchBlockAnimation.Step step = SwitchBlockAnimation.advance(
                    dependencies.terrain().switchableObjectAnimationStage, dependencies.terrain().switchBlocksState);
            dependencies.terrain().switchBlocksState = step.switchBlocksState();
            dependencies.terrain().switchableObjectAnimationStage = step.nextStage();
            dependencies.roomLoadCoordinator().get().synchronizeSwitchBlockCollisionState();
            if (step.tileCopy() != null) {
                dependencies
                        .gpu()
                        .copySwitchBlockTiles(
                                dependencies.romData(),
                                step.tileCopy().sourceOffset(),
                                step.tileCopy().destinationTile());
            }
            // AnimateTiles returns after UpdateSwitchBlockTiles while the
            // switch animation is active, so ordinary animated BG tiles wait
            // until the next idle VBlank.
            return;
        }
        dependencies.gpu().tickAnimatedTiles(dependencies.romData());
    }

    record Dependencies(
            RoomEntityFrameState entityFrame,
            RoomFollowerState followers,
            RoomItemUseState items,
            RoomLocationState location,
            RoomTerrainState terrain,
            java.util.function.Supplier<RoomEntityAfterTick> entityAfterTick,
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            java.util.function.Supplier<RoomEntityCombatBridge> entityCombatBridge,
            java.util.function.Supplier<RoomEntityFrameSetup> entityFrameSetup,
            java.util.function.Supplier<RoomEntityInputs> entityInputs,
            RomRandomByteSource entityRandomByteSource,
            GPU gpu,
            byte[] romData,
            java.util.function.Supplier<RoomCollisionQueries> roomCollisionQueries,
            java.util.function.Supplier<RoomLoadCoordinator> roomLoadCoordinator) {}
}
