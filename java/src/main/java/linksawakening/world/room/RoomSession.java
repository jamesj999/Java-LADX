package linksawakening.world.room;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.Link;
import linksawakening.world.entity.EnemyCombatInputs;
import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.EntityPickupFrameInputs;
import linksawakening.world.entity.FollowingNpcState;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.HeartPiecePickupUpdate;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.LiftedEntityState;
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
import linksawakening.world.transition.Warp;

/** Public room API delegating to room-scoped collaborators. */
public final class RoomSession implements RoomEntityOutputSource, RoomBoundaryOperations {

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

        dungeonView = new RoomDungeonLiveView(components.roomState(), -1);
        roles = RoomRoleViews.create(components, this);
        doors = components.roomDoorInteractions();
        transitions = new RoomTransitionsView(components.roomTransitionController(), this);
        playerItems = new RoomPlayerItemsView(this);
        story = new RoomStoryView(components.roomStoryState(), this);
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

    boolean deferContactRecoil(int slot, int x, int y) {
        return hasRoom() && components.entityCombatBridge().deferContactRecoil(loadedRoom, slot, x, y);
    }

    int clearEntity(int slot) {
        return !hasRoom() ? 0 : components.entityCombatBridge().clearEntity(loadedRoom, slot);
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

    void setEntityDialogActive(boolean active) {
        components.entityInputs().setEntityDialogActive(active);
        if (hasRoom()) components.entityInputs().applyDialogActive(loadedRoom, active);
    }

    void setEntityTalkState(boolean inventoryAppearing, int dialogCooldown, int windowY) {
        components.entityInputs().setEntityTalkState(inventoryAppearing, dialogCooldown, windowY);
        if (hasRoom())
            components.entityInputs().applyTalkState(loadedRoom, inventoryAppearing, dialogCooldown, windowY);
    }

    void setEntityDialogAskSelectionIndex(int selectionIndex) {
        components.entityInputs().setEntityDialogAskSelectionIndex(selectionIndex);
        if (hasRoom()) components.entityInputs().applyDialogAskSelectionIndex(loadedRoom, selectionIndex);
    }

    void setEntityMusicActive(boolean active) {
        components.entityInputs().setEntityMusicActive(active);
        if (hasRoom()) components.entityInputs().applyMusicActive(loadedRoom, active);
    }

    void setEntityActionButtonsHeld(boolean actionButtonAHeld, boolean actionButtonBHeld) {
        components.entityInputs().setEntityActionButtonsHeld(actionButtonAHeld, actionButtonBHeld);
        if (hasRoom())
            components.entityInputs().applyActionButtonsHeld(loadedRoom, actionButtonAHeld, actionButtonBHeld);
    }

    void setEntityJoypadHeld(boolean joypadHeld) {
        components.entityInputs().setEntityJoypadHeld(joypadHeld);
        if (hasRoom()) components.entityInputs().applyJoypadHeld(loadedRoom, joypadHeld);
    }

    void setEntityPressedButtonsMask(int pressedButtonsMask) {
        components.entityInputs().setEntityPressedButtonsMask(pressedButtonsMask);
        if (hasRoom()) components.entityInputs().applyPressedButtonsMask(loadedRoom, pressedButtonsMask);
    }

    void setEntityInventorySlots(int itemA, int itemB) {
        components.entityInputs().setEntityInventorySlots(itemA, itemB);
        if (hasRoom()) components.entityInputs().applyInventorySlots(loadedRoom, itemA, itemB);
    }

    void setToadstoolPlayerState(boolean hasToadstool, int magicPowderCount) {
        components.entityInputs().setToadstoolPlayerState(hasToadstool, magicPowderCount);
        if (hasRoom()) components.entityInputs().applyToadstoolPlayerState(loadedRoom, hasToadstool, magicPowderCount);
    }

    void setEntityGoldenLeavesCount(int goldenLeavesCount) {
        components.entityInputs().setEntityGoldenLeavesCount(goldenLeavesCount);
        if (hasRoom()) components.entityInputs().applyGoldenLeavesCount(loadedRoom, goldenLeavesCount);
    }

    void setSecretSeashellPegasusCollisionState(
            boolean screenShakeActive, boolean collisionActive, int collisionX, int collisionY) {
        components
                .entityInputs()
                .setSecretSeashellPegasusCollisionState(screenShakeActive, collisionActive, collisionX, collisionY);
        if (hasRoom())
            components
                    .entityInputs()
                    .applySecretSeashellPegasusCollisionState(
                            loadedRoom, screenShakeActive, collisionActive, collisionX, collisionY);
    }

    void setEntityPowerBraceletButtonHeld(boolean buttonHeld) {
        components.entityInputs().setEntityPowerBraceletButtonHeld(buttonHeld);
        if (hasRoom()) components.entityInputs().applyPowerBraceletButtonHeld(loadedRoom, buttonHeld);
    }

    void setEntityBombButtonHeld(boolean buttonHeld) {
        components.entityInputs().setEntityBombButtonHeld(buttonHeld);
        if (hasRoom()) components.entityInputs().applyBombButtonHeld(loadedRoom, buttonHeld);
    }

    void setEntityAttackStepAnimationCountdown(int countdown) {
        components.entityInputs().setEntityAttackStepAnimationCountdown(countdown);
        if (hasRoom()) components.entityInputs().applyAttackStepAnimationCountdown(loadedRoom, countdown);
    }

    void setEntityFinalPositionX(int positionX) {
        components.entityInputs().setEntityFinalPositionX(positionX);
        if (hasRoom()) components.entityInputs().applyFinalPositionX(loadedRoom, positionX);
    }

    void setEntityActionButtonsHeld(boolean held) {
        setEntityActionButtonsHeld(held, held);
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

    boolean throwLiftedEntity(linksawakening.state.Direction linkDirection) {
        return hasRoom() ? components.roomPlayerItemActions().throwLiftedEntity(loadedRoom, linkDirection) : false;
    }

    boolean fireHookshot(
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int romDirection,
            boolean linkAirborne,
            boolean linkPushing) {
        return hasRoom()
                ? components
                        .roomPlayerItemActions()
                        .fireHookshot(
                                loadedRoom,
                                linkEntityX,
                                linkEntityY,
                                linkEntityZ,
                                romDirection,
                                linkAirborne,
                                linkPushing)
                : false;
    }

    boolean fireBoomerang(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask) {
        return hasRoom()
                ? components
                        .roomPlayerItemActions()
                        .fireBoomerang(
                                loadedRoom, linkEntityX, linkEntityY, linkEntityZ, romDirection, pressedButtonsMask)
                : false;
    }

    boolean fireSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return hasRoom()
                ? components
                        .roomPlayerItemActions()
                        .fireSwordBeam(loadedRoom, linkEntityX, linkEntityY, linkEntityZ, romDirection)
                : false;
    }

    boolean fireMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return hasRoom()
                ? components
                        .roomPlayerItemActions()
                        .fireMagicRodFireball(loadedRoom, linkEntityX, linkEntityY, linkEntityZ, romDirection)
                : false;
    }

    boolean fireMagicRodFireball(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return hasRoom()
                ? components
                        .roomPlayerItemActions()
                        .fireMagicRodFireball(
                                loadedRoom, linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower)
                : false;
    }

    boolean sprinkleMagicPowder(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return hasRoom()
                ? components
                        .roomPlayerItemActions()
                        .sprinkleMagicPowder(loadedRoom, linkEntityX, linkEntityY, linkEntityZ, romDirection)
                : false;
    }

    ShovelStartResult startShovel(int linkEntityX, int linkEntityY, int romDirection, boolean linkAirborne) {
        if (romDirection < 0 || romDirection > 3)
            throw new IllegalArgumentException("ROM shovel direction out of range: " + romDirection);
        return hasRoom()
                ? components
                        .roomPlayerItemActions()
                        .startShovel(loadedRoom, linkEntityX, linkEntityY, romDirection, linkAirborne)
                : new ShovelStartResult(false, true, -1);
    }

    void advanceShovel(int linkEntityX, int linkEntityY, int romDirection, int timer) {
        if (hasRoom())
            components.roomPlayerItemActions().advanceShovel(loadedRoom, linkEntityX, linkEntityY, romDirection, timer);
    }

    int shovelAnimationState(linksawakening.state.Direction javaDirection, int timer) {
        return components.roomPlayerItemActions().shovelAnimationState(javaDirection, timer);
    }

    boolean placeBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return hasRoom()
                ? components
                        .roomPlayerItemActions()
                        .placeBomb(loadedRoom, linkEntityX, linkEntityY, linkEntityZ, romDirection)
                : false;
    }

    boolean lastBombPlacementPlayedBump() {
        return hasRoom() ? components.roomPlayerItemActions().lastBombPlacementPlayedBump(loadedRoom) : false;
    }

    boolean shootArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return hasRoom()
                ? components
                        .roomPlayerItemActions()
                        .shootArrow(loadedRoom, linkEntityX, linkEntityY, linkEntityZ, romDirection)
                : false;
    }

    ArrowShotResult shootArrowResult(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return hasRoom()
                ? components
                        .roomPlayerItemActions()
                        .shootArrowResult(loadedRoom, linkEntityX, linkEntityY, linkEntityZ, romDirection)
                : new ArrowShotResult(false, false);
    }

    ArrowShotResult shootArrowResult(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return hasRoom()
                ? components
                        .roomPlayerItemActions()
                        .shootArrowResult(loadedRoom, linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower)
                : new ArrowShotResult(false, false);
    }

    int activeProjectileCount() {
        return hasRoom() ? components.roomPlayerItemActions().activeProjectileCount(loadedRoom) : 0;
    }

    boolean bombActive() {
        return hasRoom() ? components.roomPlayerItemActions().bombActive(loadedRoom) : false;
    }

    boolean hookshotActive() {
        return hasRoom() ? components.roomPlayerItemActions().hookshotActive(loadedRoom) : false;
    }

    boolean boomerangActive() {
        return hasRoom() ? components.roomPlayerItemActions().boomerangActive(loadedRoom) : false;
    }

    boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) {
        return hasRoom() && components.roomStoryState().applyMarinWakeUpPresentation(loadedRoom, x, y, spriteVariant);
    }

    void updateHouseTarinPresentation(int frameCounter, int linkX, int linkY) {
        if (hasRoom()) components.roomStoryState().updateHouseTarinPresentation(loadedRoom, frameCounter, linkX, linkY);
    }

    void setFollowerSpriteOverrides(java.util.Map<Integer, linksawakening.entity.EntitySpriteDefinition> overrides) {
        if (hasRoom()) components.roomStoryState().setFollowerSpriteOverrides(loadedRoom, overrides);
    }

    void setTailKeyOwned(boolean owned) {
        components.roomStoryState().setTailKeyOwned(owned);
        if (hasRoom()) components.roomStoryState().applyTailKeyOwned(loadedRoom, owned);
    }

    void restoreRoomStatuses(
            byte[] overworldStatus, byte[] indoorAStatus, byte[] indoorBStatus, byte[] colorDungeonStatus) {
        components
                .roomStoryState()
                .restoreRoomStatuses(overworldStatus, indoorAStatus, indoorBStatus, colorDungeonStatus);
        if (hasRoom())
            components
                    .roomStoryState()
                    .applyRestoredRoomStatuses(
                            loadedRoom, overworldStatus, indoorAStatus, indoorBStatus, colorDungeonStatus);
    }

    void setFollowingNpcState(
            FollowingNpcState state, int linkX, int linkY, int linkZ, int entityYOffset, int linkDirection) {
        components.roomStoryState().setFollowingNpcState(state, linkX, linkY, linkZ, entityYOffset, linkDirection);
        if (hasRoom()) components.roomStoryState().synchronizeFollowingNpcEntitiesIfNeeded(loadedRoom);
    }

    void setBowWowState(int state) {
        components.roomStoryState().setBowWowState(state);
        if (hasRoom()) components.roomStoryState().applyBowWowState(loadedRoom, state);
    }

    void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        components.roomStoryState().setChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel);
        if (hasRoom())
            components.roomStoryState().applyChestPlayerLevels(loadedRoom, shieldLevel, swordLevel, powerBraceletLevel);
    }

    void setHouseNpcTradeItem(int tradeItem) {
        components.roomStoryState().setHouseNpcTradeItem(tradeItem);
        if (hasRoom()) components.roomStoryState().configureHouseNpcStoryState(loadedRoom);
    }

    void setEntityDefaultMusicTrack(int trackId) {
        components.roomStoryState().setEntityDefaultMusicTrack(trackId);
        if (hasRoom()) components.roomStoryState().updateWitchEnvironment(loadedRoom);
    }

    void configureEnemyDropRuntime() {
        if (hasRoom()) components.roomLoadCoordinator().configureEnemyDropRuntime(loadedRoom);
    }

    void setEnemyDropPlayerState(int maxHearts, int health, boolean power) {
        components.entityBridge().setEnemyDropPlayerState(maxHearts, health, power);
        configureEnemyDropRuntime();
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

    void setFreeMovementMode(boolean enabled) {
        if (hasRoom()) loadedRoom.entities.setFreeMovementMode(enabled);
    }

    @Override
    public List<LinkFacingRequest> consumeLinkFacingRequests() {
        return !hasRoom() ? List.of() : loadedRoom.entities.consumePendingLinkFacingRequests();
    }

    @Override
    public List<LinkAttackClearRequest> consumeLinkAttackClearRequests() {
        return !hasRoom() ? List.of() : loadedRoom.entities.consumePendingLinkAttackClearRequests();
    }

    @Override
    public List<LinkAttackStepRequest> consumeLinkAttackStepRequests() {
        return !hasRoom() ? List.of() : loadedRoom.entities.consumePendingLinkAttackStepRequests();
    }

    @Override
    public List<LinkHeldItemPoseRequest> consumeLinkHeldItemPoseRequests() {
        return !hasRoom() ? List.of() : loadedRoom.entities.consumePendingLinkHeldItemPoseRequests();
    }

    boolean gotItemPresentationActive() {
        return hasRoom() && loadedRoom.entities.inspection().witchGotItemPresentationActive();
    }

    boolean powerUpPickupSequenceActive() {
        return hasRoom() && loadedRoom.entities.inspection().powerUpPickupSequenceActive();
    }

    boolean swordPickupSequenceActive() {
        return hasRoom() && loadedRoom.entities.inspection().swordPickupSequenceActive();
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
        if (components.entityFrame().outbox.pendingRoomDialogRequests.isEmpty())
            return !hasRoom() ? List.of() : loadedRoom.entities.consumePendingDialogRequests();
        List<DialogRequest> requests = new ArrayList<>(components.entityFrame().outbox.pendingRoomDialogRequests);
        components.entityFrame().outbox.pendingRoomDialogRequests.clear();
        if (hasRoom()) {
            requests.addAll(loadedRoom.entities.consumePendingDialogRequests());
        }
        return List.copyOf(requests);
    }

    int entityHealth(int slot) {
        return loadedRoom.entities.enemies().enemyHealth(slot);
    }

    ActiveRoom activeRoom() {
        return hasRoom() ? loadedRoom.room : null;
    }

    boolean hasActiveRoom() {
        return hasRoom();
    }

    int currentRoomId() {
        return java.util.Objects.requireNonNull(loadedRoom).room.roomId();
    }

    int mapCategory() {
        return java.util.Objects.requireNonNull(loadedRoom).room.mapCategory();
    }

    int mapId() {
        return java.util.Objects.requireNonNull(loadedRoom).room.mapId();
    }

    int indoorRoomPositionForSave() {
        if (!hasRoom() || activeRoom().mapCategory() == Warp.CATEGORY_OVERWORLD) {
            return -1;
        }
        return components.location().indoorMapPosition;
    }

    int[][] palettes() {
        return !hasRoom() ? null : activeRoom().palettes();
    }

    boolean heartPiecePickupActive() {
        return hasRoom() && loadedRoom.entities.inspection().heartPiecePickupActive();
    }

    @Override
    public List<HeartPiecePickupUpdate> consumeHeartPieceUpdates() {
        if (!hasRoom()) return List.of();
        var updates = loadedRoom.entities.consumeHeartPieceUpdates();
        for (int i = 0; i < updates.size(); i++) {
            if (updates.get(i).complete()) {
                components.roomState().markCompleted(activeRoom());
                break;
            }
        }
        return updates;
    }

    String instrumentTrace() {
        return !hasRoom() ? "" : loadedRoom.entities.inspection().instrumentTrace();
    }

    String moblinKingTrace() {
        return !hasRoom() ? "-1" : loadedRoom.entities.inspection().moblinKingTrace();
    }

    RoomRenderSnapshot.View renderView() {
        return hasRoom() ? loadedRoom.renderView : null;
    }

    RoomRenderSnapshot renderSnapshot() {
        return hasRoom() ? loadedRoom.renderSnapshot() : null;
    }

    LiftedEntityState liftedEntityState() {
        return !hasRoom()
                ? LiftedEntityState.none()
                : loadedRoom.entities.inspection().liftedEntityState();
    }

    void setHeartPiecesCount(int count) {
        if (hasRoom()) loadedRoom.entities.setHeartPiecesCount(count);
    }

    RoomDungeon.View dungeonView() {
        return dungeonView;
    }

    byte[] dungeonMinimapRoomStatusSnapshot() {
        return hasRoom() ? components.roomState().dungeonMinimapRoomStatusSnapshot(loadedRoom.room) : new byte[64];
    }

    void resetGuardianAcornCounterOnHurt() {
        boolean roomPresent = hasRoom();
        var counters = roomPresent
                ? loadedRoom.entities.inspection().enemyDropCounters()
                : components.entityFrame().enemyDropCounters;
        components.entityFrame().enemyDropCounters =
                linksawakening.world.entity.EnemyDropResolver.resetGuardianAcornCounter(counters);
        if (hasRoom())
            loadedRoom.entities.configuration().setEnemyDropCounters(components.entityFrame().enemyDropCounters);
    }

    boolean gelClingingToLink() {
        return hasRoom() && loadedRoom.entities.gelClingingToLink();
    }

    void setEntityScreenShakeCountdown(int countdown) {
        if (hasRoom()) loadedRoom.entities.configuration().setScreenShakeCountdown(countdown);
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

    RoomLoadCoordinator.PreviousPalettes previousPalettes() {
        if (!hasRoom()) return RoomLoadCoordinator.PreviousPalettes.NONE;
        return RoomLoadCoordinator.PreviousPalettes.from(loadedRoom);
    }

    void loadInitialOverworld(int room) {
        components.roomLoadCoordinator().loadInitialOverworld(room);
    }

    void loadOverworld(int room) {
        components.roomLoadCoordinator().loadOverworld(room);
    }

    void loadIndoor(int map, int room) {
        components.roomLoadCoordinator().loadIndoor(previousPalettes(), map, room);
    }

    void loadIndoor(int map, int room, int category) {
        components.roomLoadCoordinator().loadIndoor(previousPalettes(), map, room, category);
    }

    void loadIndoorFromSavedPosition(int map, int room, int position) {
        components.roomLoadCoordinator().loadIndoorFromSavedPosition(previousPalettes(), map, room, position);
    }

    int tickDungeonTitleMessage(
            boolean dialogActive, boolean inventoryClosed, int linkMotionState, boolean freeMovement) {
        if (components.puzzles().dungeonTitleMessageCountdown == 0 || dialogActive || !inventoryClosed) return -1;
        if (--components.puzzles().dungeonTitleMessageCountdown != 0 || linkMotionState != 0 || freeMovement) return -1;
        return 0x56 + java.util.Objects.requireNonNull(loadedRoom).room.mapId();
    }

    RoomRuntime loadWarpDestination(Warp warp) {
        // GameplayWorldLoadRoomHandler ($01:442B) clears $C000..$D2FF,
        // including the recent-room ring and transient entity-clear masks.
        // Scrolling between adjacent rooms does not take this path.
        int category = warp.category();
        boolean wasIndoor = hasRoom() && loadedRoom.room.mapCategory() != Warp.CATEGORY_OVERWORLD;
        if (category != Warp.CATEGORY_SIDESCROLL) {
            Warp spawn = warp;
            int indoorRoom = wasIndoor ? components.location().indoorMapPosition : -1;
            if (indoorRoom < 0) indoorRoom = components.location().respawnIndoorRoom;
            if (warp.category() == Warp.CATEGORY_INDOOR && (warp.destMap() < 0x0A || warp.destMap() == 0xFF)) {
                var rom = components.romData();
                var address = new linksawakening.rom.RomAddress(
                        0x14, warp.destMap() == 0xFF ? 0x4E3C : 0x4DF1 + warp.destMap() * 5);
                spawn = new Warp(
                        rom.u8(address),
                        rom.u8(address.plus(1)),
                        rom.u8(address.plus(2)),
                        rom.u8(address.plus(3)),
                        rom.u8(address.plus(4)),
                        -1);
                indoorRoom = warp.destMap() == 0xFF
                        ? 0x3A
                        : rom.u8(new linksawakening.rom.RomAddress(0x14, 0x4E41 + warp.destMap()));
            }
            components.location().setRespawnPoint(spawn, indoorRoom);
        }
        components.entityFrame().recentRoomEntityClears.reset();
        // hFreeWarpDataAddress is the source wIsIndoor ($00:18AD–18B0).
        // $00:1904/$00:1952 clear only when destination indoor status differs.
        if (wasIndoor != (warp.category() != Warp.CATEGORY_OVERWORLD)) {
            components.location().mapBoundaryWarp.run();
        }
        components.puzzles().dungeonTitleMessageCountdown = 0;
        RoomRuntime loaded;
        if (warp.category() == Warp.CATEGORY_OVERWORLD) {
            if (wasIndoor) {
                components.gpu().loadBaseOverworldTiles(components.romData());
                components.location().currentOverworldTilesetId = RoomSessionRules.W_TILESET_NO_UPDATE;
            }
            loaded = components.roomLoadCoordinator().loadOverworld(warp.destRoom());
            components.roomLoadCoordinator().loadRoomSpecificTilesIfNeeded(warp.destRoom());
        } else {
            loaded = components
                    .roomLoadCoordinator()
                    .loadIndoor(previousPalettes(), warp.destMap(), warp.destRoom(), warp.category());
            // $00:194C-1965: old wIsIndoor is hFreeWarpDataAddress.
            if (!wasIndoor && warp.destMap() < 0x0A) components.puzzles().dungeonTitleMessageCountdown = 0x30;
        }
        return loaded;
    }

    void startAdjacentOverworldScroll(
            linksawakening.world.transition.ScrollController scroll, int direction, int x, int y) {
        components
                .roomTransitionController()
                .startAdjacentOverworldScroll(java.util.Objects.requireNonNull(loadedRoom), scroll, direction, x, y);
    }

    void startAdjacentIndoorScroll(
            linksawakening.world.transition.ScrollController scroll, int direction, int x, int y) {
        components
                .roomTransitionController()
                .startAdjacentIndoorScroll(java.util.Objects.requireNonNull(loadedRoom), scroll, direction, x, y);
    }

    boolean startOcarina(int countdown, int flags, int song) {
        RoomTransitionController.validateOcarina(countdown, flags, song);
        return hasRoom() && components.roomTransitionController().startOcarina(countdown, flags, song);
    }

    Warp consumeManboPondTransitionRequest() {
        return hasRoom() ? components.roomTransitionController().consumeManboPondTransitionRequest(loadedRoom) : null;
    }

    Warp consumeInstrumentTransitionRequest() {
        return hasRoom() ? components.roomTransitionController().consumeInstrumentTransitionRequest(loadedRoom) : null;
    }

    Warp pollStaircaseWarp(int x, int y, int z, boolean carrying) {
        return hasRoom()
                ? components.roomTransitionController().pollStaircaseWarp(loadedRoom, x, y, z, carrying)
                : null;
    }

    @Override
    public void handleWarpAndIndoorBoundaries(RoomTransitionCoordinator coordinator, Link link) {
        if (hasRoom()) coordinator.handleWarpAndIndoorBoundaries(loadedRoom, link);
    }

    @Override
    public void handleOverworldBoundary(RoomTransitionCoordinator coordinator, Link link) {
        if (hasRoom()) coordinator.handleOverworldBoundary(loadedRoom, link);
    }

    @Override
    public boolean handlePendingManboTransition(RoomTransitionCoordinator coordinator, Link link) {
        return hasRoom() && coordinator.handlePendingManboTransition(loadedRoom, link);
    }

    @Override
    public boolean handlePendingInstrumentTransition(RoomTransitionCoordinator coordinator, Link link) {
        return hasRoom() && coordinator.handlePendingInstrumentTransition(loadedRoom, link);
    }

    @Override
    public void applyWarp(Warp warp, Link link) {
        var destination = loadWarpDestination(warp).room;
        link.motion()
                .setRoomPhysicsForRoom(
                        components.roomSessionViews().roomLoadToken(),
                        destination.mapId(),
                        destination.roomId(),
                        destination.mapCategory());
        link.position().setRoomEntryPixelPosition(warp.javaPixelX(), warp.javaPixelY());
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

    void setLiftStrengthBoost(boolean redTunic, boolean power) {
        if (hasRoom()) loadedRoom.entities.configuration().setLiftStrengthBoost(redTunic, power);
    }

    void initializeNewGameWorldState() {
        components.roomStoryState().initializeNewGameWorldState();
        resetRoomTransients();
    }

    ActiveRoom requireActiveRoom() {
        return java.util.Objects.requireNonNull(loadedRoom).room;
    }

    /** ResetRoomVariables runs even when subsequent room acquisition fails. */
    void resetRoomTransients() {
        if (hasRoom()) components.roomTransientState().clearTransientRoomState(loadedRoom.contents);
        else components.roomTransientState().clearTransientRoomState();
    }
}
