package linksawakening.world.room;

import java.util.ArrayList;
import java.util.List;
import linksawakening.gpu.GPU;
import linksawakening.rom.Rom;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.object.SwitchBlockAnimation;

/** Owns room-level entity output queues and their consumption boundary. */
final class RoomEntityBridge {
    private final LoadedRoomHandle rooms;

    boolean gelClingingToLink() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded != null && loaded.entities.gelClingingToLink();
    }

    void resetGuardianAcornCounterOnHurt() {
        RoomRuntime loaded = rooms.currentOrNull();
        boolean roomPresent = loaded != null;
        var counters = roomPresent
                ? loaded.entities.inspection().enemyDropCounters()
                : dependencies.entityFrame().enemyDropCounters;
        dependencies.entityFrame().enemyDropCounters =
                linksawakening.world.entity.EnemyDropResolver.resetGuardianAcornCounter(counters);
        if (loaded != null)
            loaded.entities.configuration().setEnemyDropCounters(dependencies.entityFrame().enemyDropCounters);
    }

    int entityHealth(int slot) {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded.entities.enemies().enemyHealth(slot);
    }

    List<DialogRequest> consumeEntityDialogRequests() {
        RoomRuntime loaded = rooms.currentOrNull();
        if (dependencies.entityFrame().outbox.pendingRoomDialogRequests.isEmpty())
            return loaded == null ? List.of() : loaded.entities.consumePendingDialogRequests();
        List<DialogRequest> requests = new ArrayList<>(dependencies.entityFrame().outbox.pendingRoomDialogRequests);
        dependencies.entityFrame().outbox.pendingRoomDialogRequests.clear();
        if (loaded != null) {
            requests.addAll(loaded.entities.consumePendingDialogRequests());
        }
        return List.copyOf(requests);
    }

    boolean swordPickupSequenceActive() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded != null && loaded.entities.inspection().swordPickupSequenceActive();
    }

    boolean powerUpPickupSequenceActive() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded != null && loaded.entities.inspection().powerUpPickupSequenceActive();
    }

    boolean gotItemPresentationActive() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded != null && loaded.entities.inspection().witchGotItemPresentationActive();
    }

    public List<LinkAttackClearRequest> consumeLinkAttackClearRequests() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? List.of() : loaded.entities.consumePendingLinkAttackClearRequests();
    }

    void updateSetEnemyDropPlayerState(int maxHearts, int health, boolean power) {
        setEnemyDropPlayerState(maxHearts, health, power);
        configureEnemyDropRuntime();
    }

    void configureEnemyDropRuntime() {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded != null) dependencies.roomLoadCoordinator().configureEnemyDropRuntime(loaded);
    }

    int clearEntity(int slot) {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? 0 : dependencies.entityCombatBridge().clearEntity(loaded, slot);
    }

    boolean deferContactRecoil(int slot, int x, int y) {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded != null && dependencies.entityCombatBridge().deferContactRecoil(loaded, slot, x, y);
    }

    final RoomEntityOutbox outbox;
    final RoomEntityCombatBridge combat;
    private final Dependencies dependencies;

    private final RoomEntitySimulation roomEntitySimulation;

    RoomEntityBridge(LoadedRoomHandle rooms, Dependencies dependencies) {
        this.rooms = java.util.Objects.requireNonNull(rooms);
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
                        dependencies.location()),
                dependencies.entityConfiguration(),
                dependencies.entityEffects());
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

    List<EntityProjectileEvent> tickEntities(RoomRuntime loaded, EntityTickValues inputs) {
        return roomEntitySimulation.tickEntities(loaded, inputs);
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
            RoomEntityCombatBridge entityCombatBridge,
            RoomEntitySimulation.SetupDependencies entityConfiguration,
            RomRandomByteSource entityRandomByteSource,
            GPU gpu,
            Rom romData,
            RoomLoadCoordinator roomLoadCoordinator) {
        Dependencies {
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(items, "items");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(terrain, "terrain");
            java.util.Objects.requireNonNull(entityEffects, "entityEffects");
            java.util.Objects.requireNonNull(entityCombatBridge, "entityCombatBridge");
            java.util.Objects.requireNonNull(entityConfiguration, "entityConfiguration");
            java.util.Objects.requireNonNull(entityRandomByteSource, "entityRandomByteSource");
            java.util.Objects.requireNonNull(gpu, "gpu");
            java.util.Objects.requireNonNull(romData, "romData");
            java.util.Objects.requireNonNull(roomLoadCoordinator, "roomLoadCoordinator");
        }
    }
}
