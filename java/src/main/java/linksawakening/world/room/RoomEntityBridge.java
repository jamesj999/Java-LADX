package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import java.util.List;
import linksawakening.gpu.GPU;
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
    final RoomEntityFrameSetup frameSetup;
    final RoomEntityAfterTick afterTick;
    private final Dependencies dependencies;
    private final RoomEntityTickDefaults roomEntityTickDefaults;
    private final RoomEntitySimulation roomEntitySimulation;

    RoomEntityBridge(Dependencies dependencies) {
        this.dependencies = dependencies;
        afterTick = dependencies.entityAfterTick().get();
        frameSetup = dependencies.entityFrameSetup().get();
        combat = dependencies.entityCombatBridge().get();
        roomEntityTickDefaults = new RoomEntityTickDefaults(
                new RoomEntityTickDefaults.Dependencies(dependencies.entityBridge(), dependencies.followers()));
        roomEntitySimulation = new RoomEntitySimulation(new RoomEntitySimulation.Dependencies(
                dependencies.entityBridge(),
                dependencies.entityFrame(),
                dependencies.entityRandomByteSource(),
                dependencies.followers(),
                dependencies.items(),
                dependencies.location(),
                dependencies.roomCollisionQueries(),
                () -> this));
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
        roomEntityTickDefaults.tickEntities(frameCounter);
    }

    void tickEntities(int frameCounter, int linkEntityX, int linkEntityY) {
        roomEntityTickDefaults.tickEntities(frameCounter, linkEntityX, linkEntityY);
    }

    void tickEntities(int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkDirection) {
        roomEntityTickDefaults.tickEntities(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkDirection);
    }

    void tickEntities(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            boolean linkAirborne,
            int linkDirection) {
        roomEntityTickDefaults.tickEntities(
                frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkAirborne, linkDirection);
    }

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int linkMotionState,
            int linkDirection,
            boolean usingShield) {
        return roomEntityTickDefaults.tickEntitiesWithProjectileEvents(
                frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkMotionState, linkDirection, usingShield);
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
        return roomEntityTickDefaults.tickEntitiesWithProjectileEvents(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkEntityZ,
                linkAirborne,
                linkMotionState,
                linkDirection,
                usingShield);
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

    List<EntityProjectileEvent> tickEntities(EntityTickInputs inputs) {
        return roomEntitySimulation.tickEntities(inputs);
    }
}
