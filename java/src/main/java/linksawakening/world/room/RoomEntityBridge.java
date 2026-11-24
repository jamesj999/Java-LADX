package linksawakening.world.room;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
        return rooms.test(room -> room.entities().enemies().gelClingingToLink());
    }

    void resetGuardianAcornCounterOnHurt() {
        var counters = rooms.readOr(
                dependencies.entityFrame().enemyDropCounters(),
                room -> room.entities().inspection().enemyDropCounters());
        dependencies
                .entityFrame()
                .enemyDropCounters(linksawakening.world.entity.EnemyDropResolver.resetGuardianAcornCounter(counters));
        rooms.update(dependencies, (room, state) -> room.entities()
                .configuration()
                .setEnemyDropCounters(state.entityFrame().enemyDropCounters()));
    }

    int entityHealth(int slot) {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded.entities().enemies().enemyHealth(slot);
    }

    List<DialogRequest> consumeEntityDialogRequests() {
        if (dependencies.entityFrame().outbox().pendingRoomDialogRequests().isEmpty())
            return rooms.drain(room -> room.entities().outbox().consumePendingDialogRequests());
        List<DialogRequest> requests =
                new ArrayList<>(dependencies.entityFrame().outbox().pendingRoomDialogRequests());
        dependencies.entityFrame().outbox().pendingRoomDialogRequests().clear();
        requests.addAll(rooms.drain(room -> room.entities().outbox().consumePendingDialogRequests()));
        return List.copyOf(requests);
    }

    boolean swordPickupSequenceActive() {
        return rooms.test(room -> room.entities().inspection().swordPickupSequenceActive());
    }

    boolean powerUpPickupSequenceActive() {
        return rooms.test(room -> room.entities().inspection().powerUpPickupSequenceActive());
    }

    boolean gotItemPresentationActive() {
        return rooms.test(room -> room.entities().inspection().witchGotItemPresentationActive());
    }

    @Override
    public List<LinkAttackClearRequest> consumeLinkAttackClearRequests() {
        return rooms.drain(room -> room.entities().outbox().consumePendingLinkAttackClearRequests());
    }

    void updateEnemyDropPlayerState(int maxHearts, int health, boolean power) {
        setEnemyDropPlayerState(maxHearts, health, power);
        configureEnemyDropRuntime();
    }

    void configureEnemyDropRuntime() {
        rooms.update(dependencies, (room, state) -> state.roomLoadCoordinator().configureEnemyDropRuntime(room));
    }

    int clearEntity(int slot) {
        return rooms.intOr(slot, 0, (room, index) -> room.interactions().clearEntity(index));
    }

    boolean deferContactRecoil(int slot, int x, int y) {
        return rooms.testPosition(slot, x, y, (room, index, linkX, linkY) -> room.entities()
                .combat()
                .deferContactRecoil(index, linkX, linkY));
    }

    @Override
    public List<EntityProjectileEvent> tickEntities(EntityTickValues inputs) {
        if (!inputs.incomingScrollOnly()) {
            int x = inputs.position().x();
            int y = inputs.position().y();
            int z = inputs.position().z();
            int motion = inputs.motion().rawState();
            int direction = inputs.position().rawDirection();
            dependencies.followers().observeLink(x, y, z, direction);
            dependencies.entityFrame().observeMotionState(motion);
            dependencies.location().beginEntityTick();
        }
        return rooms.readOr(this, inputs, List.of(), (room, bridge, frame) -> bridge.tickEntities(room, frame));
    }

    @Override
    public boolean inBossBattle() {
        return rooms.test(room -> room.entities().inspection().inBossBattle());
    }

    @Override
    public List<linksawakening.world.entity.event.EntityCombatEvent> resolveEntityCombat(EnemyCombatInputs inputs) {
        if (inputs instanceof EntityCombatInputs owned) Objects.requireNonNull(owned.sword(), "sword");
        else inputs.swordCollisionActive();
        return rooms.readOr(
                inputs, List.of(), (room, frame) -> room.interactions().resolveEntityCombat(frame));
    }

    @Override
    public linksawakening.world.entity.EntityPickupEvent collectEntityIfNeeded(EntityPickupFrameInputs inputs) {
        // Evaluate both required groups before the no-room default, preserving boundary validation.
        if (inputs instanceof EntityPickupInputs owned) {
            Objects.requireNonNull(owned.link(), "link");
            Objects.requireNonNull(owned.sword(), "sword");
        } else {
            inputs.linkDirection();
            inputs.swordActive();
        }
        return rooms.readOr(inputs, null, (room, frame) -> room.interactions().collectEntityIfNeeded(frame));
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
                dependencies.followers().followingLinkDirection(),
                dependencies.followers().followingLinkZ());
    }

    @Override
    public linksawakening.world.entity.EntityPickupEvent collectEntityIfNeeded(
            int frame, int x, int y, boolean airborne, boolean interactive, int direction) {
        return collectEntityIfNeeded(
                frame,
                x,
                y,
                airborne,
                interactive,
                direction,
                dependencies.followers().followingLinkZ());
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
                dependencies.followers().followingLinkZ(),
                dependencies.followers().followingLinkDirection());
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
        var events = rooms.drain(room -> room.entities().outbox().consumePendingEntityEvents());
        dependencies
                .entityFrame()
                .outbox()
                .combatDrain()
                .drain(events, dependencies.entityFrame().outbox().pendingRoomEntityEvents(), target);
    }

    @Override
    public DialogRequest consumeFirstEntityDialogRequest() {
        DialogRequest first =
                dependencies.entityFrame().outbox().pendingRoomDialogRequests().isEmpty()
                        ? null
                        : dependencies
                                .entityFrame()
                                .outbox()
                                .pendingRoomDialogRequests()
                                .getFirst();
        dependencies.entityFrame().outbox().pendingRoomDialogRequests().clear();
        var events = rooms.drain(room -> room.entities().outbox().consumePendingDialogRequests());
        return first != null || events.isEmpty() ? first : events.getFirst();
    }

    @Override
    public List<EntityCombatEvent> consumeEntityEvents() {
        var entityEvents = rooms.drain(room -> room.entities().outbox().consumePendingEntityEvents());
        if (dependencies.entityFrame().outbox().pendingRoomEntityEvents().isEmpty()) return entityEvents;
        List<EntityCombatEvent> events = new ArrayList<>(entityEvents.size()
                + dependencies.entityFrame().outbox().pendingRoomEntityEvents().size());
        events.addAll(entityEvents);
        events.addAll(dependencies.entityFrame().outbox().pendingRoomEntityEvents());
        dependencies.entityFrame().outbox().pendingRoomEntityEvents().clear();
        return List.copyOf(events);
    }

    @Override
    public List<LinkFinalPositionRequest> consumeLinkFinalPositionRequests() {
        return rooms.drain(room -> room.entities().outbox().consumePendingLinkFinalPositionRequests());
    }

    @Override
    public List<RoosterLinkStateRequest> consumeRoosterLinkStateRequests() {
        return rooms.drain(room -> room.entities().outbox().consumePendingRoosterLinkStateRequests());
    }

    @Override
    public List<LinkMotionBlockRequest> consumeLinkMotionBlockRequests() {
        return rooms.drain(room -> room.entities().outbox().consumePendingLinkMotionBlockRequests());
    }

    @Override
    public List<SideViewPlatformLinkRequest> consumeSideViewPlatformLinkRequests() {
        return rooms.drain(room -> room.entities().outbox().consumePendingSideViewPlatformLinkRequests());
    }

    @Override
    public List<SideViewPotLinkRequest> consumeSideViewPotLinkRequests() {
        return rooms.drain(room -> room.entities().outbox().consumePendingSideViewPotLinkRequests());
    }

    @Override
    public List<HinoxLinkEffectRequest> consumeHinoxLinkEffectRequests() {
        return rooms.drain(room -> room.entities().outbox().consumePendingHinoxLinkEffectRequests());
    }

    @Override
    public List<LinkFallenPoseRequest> consumeLinkFallenPoseRequests() {
        return rooms.drain(room -> room.entities().outbox().consumePendingLinkFallenPoseRequests());
    }

    @Override
    public List<LinkFacingRequest> consumeLinkFacingRequests() {
        return rooms.drain(room -> room.entities().outbox().consumePendingLinkFacingRequests());
    }

    @Override
    public List<LinkAttackStepRequest> consumeLinkAttackStepRequests() {
        return rooms.drain(room -> room.entities().outbox().consumePendingLinkAttackStepRequests());
    }

    @Override
    public List<LinkHeldItemPoseRequest> consumeLinkHeldItemPoseRequests() {
        return rooms.drain(room -> room.entities().outbox().consumePendingLinkHeldItemPoseRequests());
    }

    @Override
    public List<LinkSwordSpinPoseRequest> consumeLinkSwordSpinPoseRequests() {
        return rooms.drain(room -> room.entities().outbox().consumePendingLinkSwordSpinPoseRequests());
    }

    @Override
    public List<LinkSwordFinalPoseRequest> consumeLinkSwordFinalPoseRequests() {
        return rooms.drain(room -> room.entities().outbox().consumePendingLinkSwordFinalPoseRequests());
    }

    @Override
    public List<ScreenShakeRequest> consumeScreenShakeRequests() {
        return rooms.drain(room -> room.entities().outbox().consumePendingScreenShakeRequests());
    }

    @Override
    public int consumePendingMusicTrack() {
        if (dependencies.entityFrame().outbox().pendingRoomMusicTrack() >= 0) {
            int pending = dependencies.entityFrame().outbox().pendingRoomMusicTrack();
            dependencies.entityFrame().outbox().pendingRoomMusicTrack(-1);
            return pending;
        }
        return rooms.intOr(-1, room -> room.entities().outbox().consumePendingMusicTrack());
    }

    @Override
    public int consumePendingMusicFadeOutCountdown() {
        return rooms.intOr(0, room -> room.entities().outbox().consumePendingMusicFadeOutCountdown());
    }

    @Override
    public List<LikeLikeEvent> consumeLikeLikeEvents() {
        return rooms.drain(room -> room.entities().outbox().consumePendingLikeLikeEvents());
    }

    @Override
    public List<HeartPiecePickupUpdate> consumeHeartPieceUpdates() {
        return rooms.readOr(this, List.of(), (room, bridge) -> {
            var updates = room.entities().outbox().consumeHeartPieceUpdates();
            for (int i = 0; i < updates.size(); i++) {
                if (updates.get(i).complete()) {
                    bridge.dependencies.roomState().markCompleted(room.room());
                    break;
                }
            }
            return updates;
        });
    }

    @Override
    public List<WarpLinkStateRequest> consumeWarpLinkStateRequests() {
        List<WarpLinkStateRequest> requests =
                rooms.drain(room -> room.entities().outbox().consumePendingWarpLinkStateRequests());
        if (!requests.isEmpty()) {
            dependencies.items().finishOcarinaPlayback();
            dependencies.entityInputs().clearDialogAfterWarp();
        }
        return requests;
    }

    private final RoomEntityOutbox outbox;
    private final Dependencies dependencies;

    private final RoomEntitySimulation roomEntitySimulation;

    RoomEntityBridge(LoadedRoomHandle rooms, Dependencies dependencies, RoomEntitySimulation roomEntitySimulation) {
        this.rooms = Objects.requireNonNull(rooms);
        this.dependencies = Objects.requireNonNull(dependencies, "dependencies");
        outbox = dependencies.entityFrame().outbox();

        this.roomEntitySimulation = Objects.requireNonNull(roomEntitySimulation, "roomEntitySimulation");
    }

    void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp) {
        if (maxHearts < 0 || maxHearts > 0xFF) {
            throw new IllegalArgumentException("Maximum hearts must be an unsigned byte: " + maxHearts);
        }
        if (health < 0 || health > 0xFF) {
            throw new IllegalArgumentException("Health must be an unsigned byte: " + health);
        }
        dependencies.entityFrame().enemyDropMaxHearts(maxHearts);
        dependencies.entityFrame().enemyDropHealth(health);
        dependencies.entityFrame().enemyDropLowHealth(null);
        dependencies.entityFrame().enemyDropActivePowerUp(activePowerUp);
    }

    void advanceBoomerangSoundClock(boolean worldGameplay) {
        if (worldGameplay) {
            dependencies
                    .entityFrame()
                    .boomerangSoundCounter((dependencies.entityFrame().boomerangSoundCounter() + 1)
                            % linksawakening.world.entity.EntityRomConstants.BOOMERANG_SFX_COUNTER_PERIOD);
        }
        // Main may run several world frames without an entity pass during a
        // transition. The next pass must use that clock without ticking twice.
        dependencies.entityFrame().boomerangSoundClockProvided(true);
    }

    List<EntityProjectileEvent> tickEntities(RoomRuntime loaded, EntityTickValues inputs) {
        return roomEntitySimulation.tickEntities(loaded, inputs);
    }

    boolean consumeWorldLinkMotionBlockRequest() {
        boolean pending = dependencies.entityFrame().worldLinkMotionBlockPending();
        dependencies.entityFrame().worldLinkMotionBlockPending(false);
        return pending;
    }

    boolean tickGameplayVBlank() {
        if (SwitchBlockAnimation.isAnimating(dependencies.terrain().switchableObjectAnimationStage())) {
            SwitchBlockAnimation.Step step = SwitchBlockAnimation.advance(
                    dependencies.terrain().switchableObjectAnimationStage(),
                    dependencies.terrain().switchBlocksState());
            dependencies.terrain().switchBlocksState(step.switchBlocksState());
            dependencies.terrain().switchableObjectAnimationStage(step.nextStage());
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
            return false;
        }
        return dependencies.gpu().tickAnimatedTiles(dependencies.romData());
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
            Objects.requireNonNull(entityFrame, "entityFrame");
            Objects.requireNonNull(roomState, "roomState");
            Objects.requireNonNull(entityInputs, "entityInputs");
            Objects.requireNonNull(followers, "followers");
            Objects.requireNonNull(items, "items");
            Objects.requireNonNull(location, "location");
            Objects.requireNonNull(terrain, "terrain");
            Objects.requireNonNull(entityEffects, "entityEffects");
            Objects.requireNonNull(entityConfiguration, "entityConfiguration");
            Objects.requireNonNull(entityRandomByteSource, "entityRandomByteSource");
            Objects.requireNonNull(gpu, "gpu");
            Objects.requireNonNull(romData, "romData");
            Objects.requireNonNull(roomLoadCoordinator, "roomLoadCoordinator");
        }
    }

    RoomEntityOutbox outbox() {
        return this.outbox;
    }
}
