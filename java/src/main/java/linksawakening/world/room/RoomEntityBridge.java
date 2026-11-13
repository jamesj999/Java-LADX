package linksawakening.world.room;

import java.util.List;
import linksawakening.gpu.GPU;
import linksawakening.rom.Rom;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.object.SwitchBlockAnimation;

/** Owns room-level entity output queues and their consumption boundary. */
final class RoomEntityBridge {

    final RoomEntityOutbox outbox;
    final RoomEntityCombatBridge combat;
    private final Dependencies dependencies;

    private final RoomEntitySimulation roomEntitySimulation;

    RoomEntityBridge(Dependencies dependencies) {
        this.dependencies = dependencies;
        outbox = dependencies.entityFrame().outbox;
        combat = dependencies.entityCombatBridge();

        roomEntitySimulation = new RoomEntitySimulation(
                new RoomEntitySimulation.Dependencies(
                        this,
                        dependencies.entityFrame(),
                        dependencies.entityRandomByteSource(),
                        dependencies.followers(),
                        dependencies.items(),
                        dependencies.location(),
                        dependencies.roomCollisionQueries()),
                dependencies.entityConfiguration(),
                dependencies.entityEffects());
    }

    RoomEntityRuntime runtime() {
        return dependencies.entityFrame().runtime;
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
        dependencies.roomLoadCoordinator().configureEnemyDropRuntime();
    }

    void advanceBoomerangSoundClock(boolean worldGameplay) {
        if (worldGameplay) {
            dependencies.entityFrame().boomerangSoundCounter = (dependencies.entityFrame().boomerangSoundCounter + 1)
                    % linksawakening.world.entity.EntityRomConstants.BOOMERANG_SFX_COUNTER_PERIOD;
        }
        // Main may run several world frames without an entity pass during a
        // transition. The next pass must use that clock without ticking twice.
        dependencies.entityFrame().boomerangSoundClockProvided = true;
    }

    List<EntityProjectileEvent> tickEntities(RoomRuntime loaded, EntityTickInputs inputs) {
        return roomEntitySimulation.tickEntities(loaded, inputs);
    }

    List<WarpLinkStateRequest> consumeWarpLinkStateRequests() {
        List<WarpLinkStateRequest> requests = dependencies.entityRuntimeState().runtime == null
                ? List.of()
                : dependencies.entityRuntimeState().runtime.consumePendingWarpLinkStateRequests();
        if (!requests.isEmpty()) {
            dependencies.items().ocarinaPlaybackCountdown = 0;
            dependencies.items().ocarinaAnimationCounter = 0;
            dependencies.items().ocarinaAnimationPhase = 0;
            dependencies.entityInputs().entityDialogActive = false;
            dependencies.entityInputs().entityDialogCooldown = 0;
            dependencies.entityInputs().entityInventoryAppearing = false;
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
            dependencies.roomLoadCoordinator().synchronizeSwitchBlockCollisionState();
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
            RoomEntitySessionState entityFrame,
            RoomFollowerState followers,
            RoomItemUseState items,
            RoomLocationState location,
            RoomTerrainState terrain,
            RoomEntitySimulation.EffectsDependencies entityEffects,
            RoomEntitySessionState entityRuntimeState,
            RoomEntityCombatBridge entityCombatBridge,
            RoomEntitySimulation.SetupDependencies entityConfiguration,
            RoomEntityInputs entityInputs,
            RomRandomByteSource entityRandomByteSource,
            GPU gpu,
            Rom romData,
            RoomCollisionQueries roomCollisionQueries,
            RoomLoadCoordinator roomLoadCoordinator) {
        Dependencies {
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(items, "items");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(terrain, "terrain");
            java.util.Objects.requireNonNull(entityEffects, "entityEffects");
            java.util.Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
            java.util.Objects.requireNonNull(entityCombatBridge, "entityCombatBridge");
            java.util.Objects.requireNonNull(entityConfiguration, "entityConfiguration");
            java.util.Objects.requireNonNull(entityInputs, "entityInputs");
            java.util.Objects.requireNonNull(entityRandomByteSource, "entityRandomByteSource");
            java.util.Objects.requireNonNull(gpu, "gpu");
            java.util.Objects.requireNonNull(romData, "romData");
            java.util.Objects.requireNonNull(roomCollisionQueries, "roomCollisionQueries");
            java.util.Objects.requireNonNull(roomLoadCoordinator, "roomLoadCoordinator");
        }
    }
}
