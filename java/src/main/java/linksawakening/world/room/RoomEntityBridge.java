package linksawakening.world.room;

import java.util.ArrayList;
import java.util.List;
import linksawakening.gpu.GPU;
import linksawakening.rom.Rom;
import linksawakening.world.entity.EnemyCombatInputs;
import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.EntityPickupFrameInputs;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.HeartPiecePickupUpdate;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkAttackStepRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.LinkSwordFinalPoseRequest;
import linksawakening.world.entity.event.LinkSwordSpinPoseRequest;
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.object.SwitchBlockAnimation;

/** Owns room-level entity output queues and their consumption boundary. */
final class RoomEntityBridge implements RoomEntities, RoomEntityOutputSource {
    private final LoadedRoomHandle rooms;

    boolean gelClingingToLink() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded != null && loaded.entities.enemies().gelClingingToLink();
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
            return loaded == null ? List.of() : loaded.entities.outbox().consumePendingDialogRequests();
        List<DialogRequest> requests = new ArrayList<>(dependencies.entityFrame().outbox.pendingRoomDialogRequests);
        dependencies.entityFrame().outbox.pendingRoomDialogRequests.clear();
        if (loaded != null) {
            requests.addAll(loaded.entities.outbox().consumePendingDialogRequests());
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

    @Override
    public List<LinkAttackClearRequest> consumeLinkAttackClearRequests() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? List.of() : loaded.entities.outbox().consumePendingLinkAttackClearRequests();
    }

    void updateEnemyDropPlayerState(int maxHearts, int health, boolean power) {
        setEnemyDropPlayerState(maxHearts, health, power);
        configureEnemyDropRuntime();
    }

    void configureEnemyDropRuntime() {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded != null) dependencies.roomLoadCoordinator().configureEnemyDropRuntime(loaded);
    }

    int clearEntity(int slot) {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? 0 : loaded.interactions.clearEntity(slot);
    }

    boolean deferContactRecoil(int slot, int x, int y) {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded != null && loaded.entities.combat().deferContactRecoil(slot, x, y);
    }

    @Override
    public List<EntityProjectileEvent> tickEntities(EntityTickValues inputs) {
        int x = inputs.position().x();
        int y = inputs.position().y();
        int z = inputs.position().z();
        int motion = inputs.motion().rawState();
        int direction = inputs.position().rawDirection();
        dependencies.followers().observeLink(x, y, z, direction);
        dependencies.entityFrame().observeMotionState(motion);
        dependencies.location().beginEntityTick();
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? List.of() : tickEntities(loaded, inputs);
    }

    @Override
    public boolean inBossBattle() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded != null && loaded.entities.inspection().inBossBattle();
    }

    @Override
    public java.util.List<linksawakening.world.entity.event.EntityCombatEvent> resolveEntityCombat(
            EnemyCombatInputs inputs) {
        if (inputs instanceof EntityCombatInputs owned) java.util.Objects.requireNonNull(owned.sword(), "sword");
        else inputs.swordCollisionActive();
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? java.util.List.of() : loaded.interactions.resolveEntityCombat(inputs);
    }

    @Override
    public linksawakening.world.entity.EntityPickupEvent collectEntityIfNeeded(EntityPickupFrameInputs inputs) {
        // Evaluate both required groups before the no-room default, preserving boundary validation.
        if (inputs instanceof EntityPickupInputs owned) {
            java.util.Objects.requireNonNull(owned.link(), "link");
            java.util.Objects.requireNonNull(owned.sword(), "sword");
        } else {
            inputs.linkDirection();
            inputs.swordActive();
        }
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? null : loaded.interactions.collectEntityIfNeeded(inputs);
    }

    @Override
    public linksawakening.world.entity.EntityPickupEvent collectEntityIfNeeded(
            int frame, int x, int y, boolean airborne, boolean interactive) {
        return collectEntityIfNeeded(
                frame,
                x,
                y,
                airborne,
                interactive,
                dependencies.followers().followingLinkDirection,
                dependencies.followers().followingLinkZ);
    }

    @Override
    public linksawakening.world.entity.EntityPickupEvent collectEntityIfNeeded(
            int frame, int x, int y, boolean airborne, boolean interactive, int direction) {
        return collectEntityIfNeeded(
                frame, x, y, airborne, interactive, direction, dependencies.followers().followingLinkZ);
    }

    @Override
    public linksawakening.world.entity.EntityPickupEvent collectEntityIfNeeded(
            int frame, int x, int y, boolean airborne, boolean interactive, int direction, int z) {
        return collectEntityIfNeeded(new EntityPickupInputs(
                frame,
                new EntityPickupInputs.Link(x, y, airborne, interactive, direction, z),
                new SwordBox(false, 0, 0, 0, 0)));
    }

    @Override
    public void tickEntities(int frameCounter) {
        this.tickEntities(frameCounter, 0, 0);
    }

    @Override
    public void tickEntities(int frameCounter, int linkEntityX, int linkEntityY) {
        this.tickEntities(
                frameCounter,
                linkEntityX,
                linkEntityY,
                dependencies.followers().followingLinkZ,
                dependencies.followers().followingLinkDirection);
    }

    @Override
    public void tickEntities(int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkDirection) {
        this.tickEntitiesWithProjectileEvents(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkEntityZ,
                EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE,
                linkDirection,
                false);
    }

    @Override
    public void tickEntities(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            boolean linkAirborne,
            int linkDirection) {
        this.tickEntitiesWithProjectileEvents(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkEntityZ,
                linkAirborne,
                EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE,
                linkDirection,
                false);
    }

    @Override
    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int linkMotionState,
            int linkDirection,
            boolean usingShield) {
        return this.tickEntities(new EntityTickInputs(
                frameCounter,
                new LinkPosition(linkEntityX, linkEntityY, linkEntityZ, linkDirection, 0),
                new LinkMotion(false, linkMotionState, usingShield, 0, 0, 0),
                1,
                new SwordBox(false, 0, 0, 0, 0)));
    }

    @Override
    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            boolean linkAirborne,
            int linkMotionState,
            int linkDirection,
            boolean usingShield) {
        return this.tickEntities(new EntityTickInputs(
                frameCounter,
                new LinkPosition(linkEntityX, linkEntityY, linkEntityZ, linkDirection, 0),
                new LinkMotion(linkAirborne, linkMotionState, usingShield, 0, 0, 0),
                1,
                new SwordBox(false, 0, 0, 0, 0)));
    }

    @Override
    public void drainEntityEvents(RoomFrameDrain<EntityCombatEvent> target) {
        RoomRuntime loaded = rooms.currentOrNull();
        var events = loaded == null
                ? List.<EntityCombatEvent>of()
                : loaded.entities.outbox().consumePendingEntityEvents();
        dependencies
                .entityFrame()
                .outbox
                .combatDrain
                .drain(events, dependencies.entityFrame().outbox.pendingRoomEntityEvents, target);
    }

    @Override
    public DialogRequest consumeFirstEntityDialogRequest() {
        RoomRuntime loaded = rooms.currentOrNull();
        DialogRequest first = dependencies
                        .entityFrame()
                        .outbox
                        .pendingRoomDialogRequests
                        .isEmpty()
                ? null
                : dependencies.entityFrame().outbox.pendingRoomDialogRequests.getFirst();
        dependencies.entityFrame().outbox.pendingRoomDialogRequests.clear();
        var events = loaded == null
                ? List.<DialogRequest>of()
                : loaded.entities.outbox().consumePendingDialogRequests();
        return first != null || events.isEmpty() ? first : events.getFirst();
    }

    @Override
    public List<EntityCombatEvent> consumeEntityEvents() {
        RoomRuntime loaded = rooms.currentOrNull();
        var entityEvents = loaded == null
                ? List.<EntityCombatEvent>of()
                : loaded.entities.outbox().consumePendingEntityEvents();
        if (dependencies.entityFrame().outbox.pendingRoomEntityEvents.isEmpty()) return entityEvents;
        List<EntityCombatEvent> events = new ArrayList<>(entityEvents.size()
                + dependencies.entityFrame().outbox.pendingRoomEntityEvents.size());
        events.addAll(entityEvents);
        events.addAll(dependencies.entityFrame().outbox.pendingRoomEntityEvents);
        dependencies.entityFrame().outbox.pendingRoomEntityEvents.clear();
        return List.copyOf(events);
    }

    @Override
    public List<LinkFinalPositionRequest> consumeLinkFinalPositionRequests() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? List.of() : loaded.entities.outbox().consumePendingLinkFinalPositionRequests();
    }

    @Override
    public List<RoosterLinkStateRequest> consumeRoosterLinkStateRequests() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? List.of() : loaded.entities.outbox().consumePendingRoosterLinkStateRequests();
    }

    @Override
    public List<LinkMotionBlockRequest> consumeLinkMotionBlockRequests() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? List.of() : loaded.entities.outbox().consumePendingLinkMotionBlockRequests();
    }

    @Override
    public List<SideViewPlatformLinkRequest> consumeSideViewPlatformLinkRequests() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? List.of() : loaded.entities.outbox().consumePendingSideViewPlatformLinkRequests();
    }

    @Override
    public List<SideViewPotLinkRequest> consumeSideViewPotLinkRequests() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? List.of() : loaded.entities.outbox().consumePendingSideViewPotLinkRequests();
    }

    @Override
    public List<HinoxLinkEffectRequest> consumeHinoxLinkEffectRequests() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? List.of() : loaded.entities.outbox().consumePendingHinoxLinkEffectRequests();
    }

    @Override
    public List<LinkFallenPoseRequest> consumeLinkFallenPoseRequests() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? List.of() : loaded.entities.outbox().consumePendingLinkFallenPoseRequests();
    }

    @Override
    public List<LinkFacingRequest> consumeLinkFacingRequests() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? List.of() : loaded.entities.outbox().consumePendingLinkFacingRequests();
    }

    @Override
    public List<LinkAttackStepRequest> consumeLinkAttackStepRequests() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? List.of() : loaded.entities.outbox().consumePendingLinkAttackStepRequests();
    }

    @Override
    public List<LinkHeldItemPoseRequest> consumeLinkHeldItemPoseRequests() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? List.of() : loaded.entities.outbox().consumePendingLinkHeldItemPoseRequests();
    }

    @Override
    public List<LinkSwordSpinPoseRequest> consumeLinkSwordSpinPoseRequests() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? List.of() : loaded.entities.outbox().consumePendingLinkSwordSpinPoseRequests();
    }

    @Override
    public List<LinkSwordFinalPoseRequest> consumeLinkSwordFinalPoseRequests() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? List.of() : loaded.entities.outbox().consumePendingLinkSwordFinalPoseRequests();
    }

    @Override
    public List<ScreenShakeRequest> consumeScreenShakeRequests() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? List.of() : loaded.entities.outbox().consumePendingScreenShakeRequests();
    }

    @Override
    public int consumePendingMusicTrack() {
        RoomRuntime loaded = rooms.currentOrNull();
        if (dependencies.entityFrame().outbox.pendingRoomMusicTrack >= 0) {
            int pending = dependencies.entityFrame().outbox.pendingRoomMusicTrack;
            dependencies.entityFrame().outbox.pendingRoomMusicTrack = -1;
            return pending;
        }
        return loaded == null ? -1 : loaded.entities.outbox().consumePendingMusicTrack();
    }

    @Override
    public int consumePendingMusicFadeOutCountdown() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? 0 : loaded.entities.outbox().consumePendingMusicFadeOutCountdown();
    }

    @Override
    public List<LikeLikeEvent> consumeLikeLikeEvents() {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded == null) {
            return List.of();
        }
        return loaded.entities.outbox().consumePendingLikeLikeEvents();
    }

    @Override
    public List<HeartPiecePickupUpdate> consumeHeartPieceUpdates() {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded == null) return List.of();
        var updates = loaded.entities.outbox().consumeHeartPieceUpdates();
        for (int i = 0; i < updates.size(); i++) {
            if (updates.get(i).complete()) {
                dependencies.roomState().markCompleted(loaded.room);
                break;
            }
        }
        return updates;
    }

    @Override
    public List<WarpLinkStateRequest> consumeWarpLinkStateRequests() {
        RoomRuntime loaded = rooms.currentOrNull();
        List<WarpLinkStateRequest> requests =
                loaded == null ? List.of() : loaded.entities.outbox().consumePendingWarpLinkStateRequests();
        if (!requests.isEmpty()) {
            dependencies.items().finishOcarinaPlayback();
            dependencies.entityInputs().clearDialogAfterWarp();
        }
        return requests;
    }

    final RoomEntityOutbox outbox;
    private final Dependencies dependencies;

    private final RoomEntitySimulation roomEntitySimulation;

    RoomEntityBridge(LoadedRoomHandle rooms, Dependencies dependencies) {
        this.rooms = java.util.Objects.requireNonNull(rooms);
        this.dependencies = dependencies;
        outbox = dependencies.entityFrame().outbox;

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
            RoomWorldState roomState,
            RoomEntityInputs entityInputs,
            RoomFollowerState followers,
            RoomItemUseState items,
            RoomLocationState location,
            RoomTerrainState terrain,
            RoomEntitySimulation.EffectsDependencies entityEffects,
            RoomEntitySimulation.SetupDependencies entityConfiguration,
            RomRandomByteSource entityRandomByteSource,
            GPU gpu,
            Rom romData,
            RoomLoadCoordinator roomLoadCoordinator) {
        Dependencies {
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(roomState, "roomState");
            java.util.Objects.requireNonNull(entityInputs, "entityInputs");
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(items, "items");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(terrain, "terrain");
            java.util.Objects.requireNonNull(entityEffects, "entityEffects");
            java.util.Objects.requireNonNull(entityConfiguration, "entityConfiguration");
            java.util.Objects.requireNonNull(entityRandomByteSource, "entityRandomByteSource");
            java.util.Objects.requireNonNull(gpu, "gpu");
            java.util.Objects.requireNonNull(romData, "romData");
            java.util.Objects.requireNonNull(roomLoadCoordinator, "roomLoadCoordinator");
        }
    }
}
