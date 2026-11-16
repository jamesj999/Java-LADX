package linksawakening.world.room;

import java.util.ArrayList;
import java.util.List;
import linksawakening.world.entity.EnemyCombatInputs;
import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.EntityPickupFrameInputs;
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

/** Public room API delegating to room-scoped collaborators. */
public final class RoomSession implements RoomEntityOutputSource {

    /** Applies the live frame inputs in source order before ticking slots. */
    public List<EntityProjectileEvent> tickEntities(EntityFrameValues inputs) {
        return frameInputs.tick(inputs);
    }

    public EntityFrameResults entityFrameResults() {
        return frameResults;
    }

    public RoomDoors doors() {
        return doors;
    }

    public RoomTransitions transitions() {
        return transitions;
    }

    public RoomPlayerItems playerItems() {
        return playerItems;
    }

    public RoomStory story() {
        return story;
    }

    // The sole optional current generation; room-only code receives a non-null value.
    private RoomRuntime loadedRoom;
    private final LoadedRoomHandle loadedRooms = new LoadedRoomHandle();

    RoomRuntime loadedRoomOrNull() {
        return loadedRoom;
    }

    void installRoom(RoomRuntime room) {
        java.util.Objects.requireNonNull(room, "room");
        loadedRoom = room;
        loadedRooms.install(room);
        dungeonView.setRoomMap(room.room.mapId());
    }

    public boolean hasRoom() {
        return loadedRoom != null;
    }

    final RoomSessionComponents components;
    private final RoomRoleViews roles;
    private final RoomFrameInputApplier frameInputs;
    private final EntityFrameResults frameResults;
    private final RoomDoors doors;
    private final RoomTransitions transitions;
    private final RoomPlayerItems playerItems;
    private final RoomStory story;
    private final RoomLoadListener roomLoadListener;
    private final RoomDungeonLiveView dungeonView;

    static long nextRoomSessionId;

    public RoomSession(RoomSessionConfig config) {
        roomLoadListener = config.presentation().roomLoadListener() == null
                ? room -> {}
                : config.presentation().roomLoadListener();
        components = RoomSessionContext.create(
                config, loadedRooms, this::installRoom, this::roomLoaded, this::resetRoomTransients);

        dungeonView = components.roomState().view();
        roles = RoomRoleViews.create(components, this, loadedRooms);
        doors = components.roomDoorInteractions();
        transitions = components.roomTransitionController();
        playerItems = components.roomPlayerItemActions();
        story = components.roomStoryState();
        frameInputs = new RoomFrameInputApplier(
                components.entityInputs(),
                components.entityBridge(),
                components.entityFrame(),
                roles.entityState(),
                story,
                components.roomLoadCoordinator(),
                this);
        frameResults = new EntityFrameResults(
                new LinkOutputs(components.linkOutputsDependencies(), this),
                new RewardOutputs(components.rewardOutputsDependencies(), this),
                new PresentationOutputs(this));
    }

    public List<EntityProjectileEvent> tickEntities(EntityTickValues inputs) {

        int linkEntityX = inputs.position().x();
        int linkEntityY = inputs.position().y();
        int linkEntityZ = inputs.position().z();

        int linkMotionState = inputs.motion().rawState();
        int linkDirection = inputs.position().rawDirection();

        components.followers().followingLinkX = linkEntityX & 0xFF;
        components.followers().followingLinkY = linkEntityY & 0xFF;
        components.followers().followingLinkZ = linkEntityZ & 0xFF;
        components.followers().followingLinkDirection = linkDirection & 0xFF;
        components.entityFrame().currentLinkMotionState = linkMotionState & 0xFF;
        components.location().shouldGetLostInMysteriousWoods = false;

        return hasRoom() ? components.entityBridge().tickEntities(loadedRoom, inputs) : List.of();
    }

    public RoomEntities entities() {
        return roles.entities();
    }

    public RoomEntityState entityState() {
        return roles.entityState();
    }

    public RoomRooms rooms() {
        return roles.rooms();
    }

    public RoomDungeon dungeon() {
        return roles.dungeon();
    }

    public RoomProgress progress() {
        return roles.progress();
    }

    public RoomTerrain terrain() {
        return roles.terrain();
    }

    boolean inBossBattle() {
        return hasRoom() && loadedRoom.entities.inspection().inBossBattle();
    }

    java.util.List<linksawakening.world.entity.event.EntityCombatEvent> resolveEntityCombat(EnemyCombatInputs inputs) {
        if (inputs instanceof EntityCombatInputs owned) java.util.Objects.requireNonNull(owned.sword(), "sword");
        else inputs.swordCollisionActive();
        return !hasRoom()
                ? java.util.List.of()
                : components.entityCombatBridge().resolveEntityCombat(loadedRoom, inputs);
    }

    linksawakening.world.entity.EntityPickupEvent collectEntityIfNeeded(EntityPickupFrameInputs inputs) {
        // Evaluate both required groups before the no-room default, preserving boundary validation.
        if (inputs instanceof EntityPickupInputs owned) {
            java.util.Objects.requireNonNull(owned.link(), "link");
            java.util.Objects.requireNonNull(owned.sword(), "sword");
        } else {
            inputs.linkDirection();
            inputs.swordActive();
        }
        return !hasRoom() ? null : components.entityCombatBridge().collectEntityIfNeeded(loadedRoom, inputs);
    }

    linksawakening.world.entity.EntityPickupEvent collectEntityIfNeeded(
            int frame, int x, int y, boolean airborne, boolean interactive) {
        return collectEntityIfNeeded(
                frame,
                x,
                y,
                airborne,
                interactive,
                components.followers().followingLinkDirection,
                components.followers().followingLinkZ);
    }

    linksawakening.world.entity.EntityPickupEvent collectEntityIfNeeded(
            int frame, int x, int y, boolean airborne, boolean interactive, int direction) {
        return collectEntityIfNeeded(
                frame, x, y, airborne, interactive, direction, components.followers().followingLinkZ);
    }

    linksawakening.world.entity.EntityPickupEvent collectEntityIfNeeded(
            int frame, int x, int y, boolean airborne, boolean interactive, int direction, int z) {
        return collectEntityIfNeeded(new EntityPickupInputs(
                frame,
                new EntityPickupInputs.Link(x, y, airborne, interactive, direction, z),
                new SwordBox(false, 0, 0, 0, 0)));
    }

    void tickEntities(int frameCounter) {
        this.tickEntities(frameCounter, 0, 0);
    }

    void tickEntities(int frameCounter, int linkEntityX, int linkEntityY) {
        this.tickEntities(
                frameCounter,
                linkEntityX,
                linkEntityY,
                components.followers().followingLinkZ,
                components.followers().followingLinkDirection);
    }

    void tickEntities(int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkDirection) {
        this.tickEntitiesWithProjectileEvents(
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

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
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

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
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
        var events = !hasRoom() ? List.<EntityCombatEvent>of() : loadedRoom.entities.consumePendingEntityEvents();
        components
                .entityFrame()
                .outbox
                .combatDrain
                .drain(events, components.entityFrame().outbox.pendingRoomEntityEvents, target);
    }

    @Override
    public DialogRequest consumeFirstEntityDialogRequest() {
        DialogRequest first = components
                        .entityFrame()
                        .outbox
                        .pendingRoomDialogRequests
                        .isEmpty()
                ? null
                : components.entityFrame().outbox.pendingRoomDialogRequests.getFirst();
        components.entityFrame().outbox.pendingRoomDialogRequests.clear();
        var events = !hasRoom() ? List.<DialogRequest>of() : loadedRoom.entities.consumePendingDialogRequests();
        return first != null || events.isEmpty() ? first : events.getFirst();
    }

    @Override
    public List<EntityCombatEvent> consumeEntityEvents() {
        var entityEvents = !hasRoom() ? List.<EntityCombatEvent>of() : loadedRoom.entities.consumePendingEntityEvents();
        if (components.entityFrame().outbox.pendingRoomEntityEvents.isEmpty()) return entityEvents;
        List<EntityCombatEvent> events = new ArrayList<>(entityEvents.size()
                + components.entityFrame().outbox.pendingRoomEntityEvents.size());
        events.addAll(entityEvents);
        events.addAll(components.entityFrame().outbox.pendingRoomEntityEvents);
        components.entityFrame().outbox.pendingRoomEntityEvents.clear();
        return List.copyOf(events);
    }

    @Override
    public List<LinkFinalPositionRequest> consumeLinkFinalPositionRequests() {
        return !hasRoom() ? List.of() : loadedRoom.entities.consumePendingLinkFinalPositionRequests();
    }

    @Override
    public List<RoosterLinkStateRequest> consumeRoosterLinkStateRequests() {
        return !hasRoom() ? List.of() : loadedRoom.entities.consumePendingRoosterLinkStateRequests();
    }

    @Override
    public List<LinkMotionBlockRequest> consumeLinkMotionBlockRequests() {
        return !hasRoom() ? List.of() : loadedRoom.entities.consumePendingLinkMotionBlockRequests();
    }

    @Override
    public List<SideViewPlatformLinkRequest> consumeSideViewPlatformLinkRequests() {
        return !hasRoom() ? List.of() : loadedRoom.entities.consumePendingSideViewPlatformLinkRequests();
    }

    @Override
    public List<SideViewPotLinkRequest> consumeSideViewPotLinkRequests() {
        return !hasRoom() ? List.of() : loadedRoom.entities.consumePendingSideViewPotLinkRequests();
    }

    @Override
    public List<HinoxLinkEffectRequest> consumeHinoxLinkEffectRequests() {
        return !hasRoom() ? List.of() : loadedRoom.entities.consumePendingHinoxLinkEffectRequests();
    }

    @Override
    public List<LinkFallenPoseRequest> consumeLinkFallenPoseRequests() {
        return !hasRoom() ? List.of() : loadedRoom.entities.consumePendingLinkFallenPoseRequests();
    }

    @Override
    public List<LinkFacingRequest> consumeLinkFacingRequests() {
        return !hasRoom() ? List.of() : loadedRoom.entities.consumePendingLinkFacingRequests();
    }

    @Override
    public List<LinkAttackClearRequest> consumeLinkAttackClearRequests() {
        return components.entityBridge().consumeLinkAttackClearRequests();
    }

    @Override
    public List<LinkAttackStepRequest> consumeLinkAttackStepRequests() {
        return !hasRoom() ? List.of() : loadedRoom.entities.consumePendingLinkAttackStepRequests();
    }

    @Override
    public List<LinkHeldItemPoseRequest> consumeLinkHeldItemPoseRequests() {
        return !hasRoom() ? List.of() : loadedRoom.entities.consumePendingLinkHeldItemPoseRequests();
    }

    @Override
    public List<LinkSwordSpinPoseRequest> consumeLinkSwordSpinPoseRequests() {
        return !hasRoom() ? List.of() : loadedRoom.entities.consumePendingLinkSwordSpinPoseRequests();
    }

    @Override
    public List<LinkSwordFinalPoseRequest> consumeLinkSwordFinalPoseRequests() {
        return !hasRoom() ? List.of() : loadedRoom.entities.consumePendingLinkSwordFinalPoseRequests();
    }

    @Override
    public List<ScreenShakeRequest> consumeScreenShakeRequests() {
        return !hasRoom() ? List.of() : loadedRoom.entities.consumePendingScreenShakeRequests();
    }

    @Override
    public int consumePendingMusicTrack() {
        if (components.entityFrame().outbox.pendingRoomMusicTrack >= 0) {
            int pending = components.entityFrame().outbox.pendingRoomMusicTrack;
            components.entityFrame().outbox.pendingRoomMusicTrack = -1;
            return pending;
        }
        return !hasRoom() ? -1 : loadedRoom.entities.consumePendingMusicTrack();
    }

    @Override
    public int consumePendingMusicFadeOutCountdown() {
        return !hasRoom() ? 0 : loadedRoom.entities.consumePendingMusicFadeOutCountdown();
    }

    @Override
    public List<LikeLikeEvent> consumeLikeLikeEvents() {
        if (!hasRoom()) {
            return List.of();
        }
        return loadedRoom.entities.consumePendingLikeLikeEvents();
    }

    List<DialogRequest> consumeEntityDialogRequests() {
        return components.entityBridge().consumeEntityDialogRequests();
    }

    @Override
    public List<HeartPiecePickupUpdate> consumeHeartPieceUpdates() {
        if (!hasRoom()) return List.of();
        var updates = loadedRoom.entities.consumeHeartPieceUpdates();
        for (int i = 0; i < updates.size(); i++) {
            if (updates.get(i).complete()) {
                components.roomState().markCompleted(loadedRoom.room);
                break;
            }
        }
        return updates;
    }

    /** Explicit lifecycle reset; no room-only collaborator accepts an absent generation. */
    void clearRoom() {
        loadedRoom = null;
        dungeonView.setRoomMap(-1);
    }

    RoomRuntime roomLoaded(RoomRuntime room) {
        roomLoadListener.roomLoaded(room.room);
        // A listener may synchronously replace the room. Continue against that published generation.
        return java.util.Objects.requireNonNull(loadedRoom);
    }

    @Override
    public List<WarpLinkStateRequest> consumeWarpLinkStateRequests() {
        List<WarpLinkStateRequest> requests =
                !hasRoom() ? List.of() : loadedRoom.entities.consumePendingWarpLinkStateRequests();
        if (!requests.isEmpty()) {
            components.items().ocarinaPlaybackCountdown = 0;
            components.items().ocarinaAnimationCounter = 0;
            components.items().ocarinaAnimationPhase = 0;
            components.entityInputs().entityDialogActive = false;
            components.entityInputs().entityDialogCooldown = 0;
            components.entityInputs().entityInventoryAppearing = false;
        }
        return requests;
    }

    /** ResetRoomVariables runs even when subsequent room acquisition fails. */
    void resetRoomTransients() {
        if (hasRoom()) components.roomTransientState().clearTransientRoomState(loadedRoom.contents);
        else components.roomTransientState().clearTransientRoomState();
    }
}
