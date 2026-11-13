package linksawakening.world.room;

import java.util.ArrayList;
import java.util.List;
import linksawakening.state.Direction;
import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.FollowingNpcState;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
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

/** Public room API delegating to room-scoped collaborators. */
public final class RoomSession implements RoomEntityOutputSource {

    /** Applies the live frame inputs in source order before ticking slots. */
    public List<EntityProjectileEvent> tickEntities(EntityFrameInputs inputs) {
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

    RoomRuntime loadedRoomOrNull() {
        return loadedRoom;
    }

    void installRoom(RoomRuntime room) {
        java.util.Objects.requireNonNull(room, "room");
        // Transitional aliases retained only while collaborators migrate to explicit arguments.
        components.location().activeRoom = room.room;
        components.location().contents = room.contents;
        components.entityFrame().runtime = room.entities;
        components.entityFrame().loaded = room;
        components.puzzles().current = room.contents.puzzles;
        components.terrain().overrides = room.contents.overrides;
        loadedRoom = room;
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

    static long nextRoomSessionId;

    public RoomSession(RoomSessionConfig config) {
        components = RoomSessionContext.create(config, this::installRoom);

        roles = RoomRoleViews.create(components, this);
        doors = new RoomDoorsView(this);
        transitions = new RoomTransitionsView(components.roomTransitionController());
        playerItems = new RoomPlayerItemsView(this);
        story = new RoomStoryView(components.roomStoryState(), this);
        frameInputs = new RoomFrameInputApplier(
                components.entityInputs(),
                components.entityBridge(),
                components.entityFrame(),
                components.roomSessionViews(),
                roles.entityState(),
                story,
                components.roomLoadCoordinator(),
                this);
        frameResults = new EntityFrameResults(
                new LinkOutputs(components.linkOutputsDependencies(), this),
                new RewardOutputs(components.rewardOutputsDependencies()),
                new PresentationOutputs(components.presentationOutputsDependencies(), this));
    }

    public List<EntityProjectileEvent> tickEntities(EntityTickInputs inputs) {
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
        return loadedRoom != null && loadedRoom.entities.inspection().inBossBattle();
    }

    java.util.List<linksawakening.world.entity.event.EntityCombatEvent> resolveEntityCombat(EntityCombatInputs inputs) {
        java.util.Objects.requireNonNull(inputs.sword(), "sword");
        return loadedRoom == null
                ? java.util.List.of()
                : components.entityCombatBridge().resolveEntityCombat(loadedRoom, inputs);
    }

    boolean deferContactRecoil(int slot, int x, int y) {
        return loadedRoom != null && components.entityCombatBridge().deferContactRecoil(loadedRoom, slot, x, y);
    }

    int clearEntity(int slot) {
        return loadedRoom == null ? 0 : components.entityCombatBridge().clearEntity(loadedRoom, slot);
    }

    linksawakening.world.entity.EntityPickupEvent collectEntityIfNeeded(EntityPickupInputs inputs) {
        java.util.Objects.requireNonNull(inputs.link(), "link");
        java.util.Objects.requireNonNull(inputs.sword(), "sword");
        return loadedRoom == null ? null : components.entityCombatBridge().collectEntityIfNeeded(loadedRoom, inputs);
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

    boolean tickCrackedFloor(LinkGroundFrame frame, boolean dialogGotItemActive) {
        return hasRoom() && components.roomObjectMutations().tickCrackedFloor(loadedRoom, frame, dialogGotItemActive);
    }

    boolean tickCrackedFloorOnLanding(LinkGroundFrame frame, boolean dialogGotItemActive) {
        return hasRoom()
                && components
                        .roomObjectMutations()
                        .applyCrackedFloorGroundPhysics(loadedRoom.contents, frame, dialogGotItemActive);
    }

    boolean breakIndoorSwordCrystal(int location) {
        return hasRoom() && components.roomObjectMutations().breakIndoorSwordCrystal(loadedRoom, location);
    }

    ChestOpenResult tryOpenChest(
            int linkPixelX, int linkPixelY, Direction linkDirection, boolean actionPressed, int swordLevel) {
        if (!hasRoom()) {
            return new ChestOpenResult(false, -1, -1, -1);
        }
        return components
                .roomDoorInteractions()
                .tryOpenChest(loadedRoom, linkPixelX, linkPixelY, linkDirection, actionPressed, swordLevel);
    }

    boolean tryUnlockIndoorKeyDoor(int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType) {
        if (!hasRoom()) {
            return false;
        }
        return components
                .roomDoorInteractions()
                .tryUnlockIndoorKeyDoor(loadedRoom.contents, linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    boolean tryPushIndoorBlock(int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType) {
        if (!hasRoom()) {
            return false;
        }
        return components
                .roomDoorInteractions()
                .tryPushIndoorBlock(loadedRoom, linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    boolean tryLiftObject(
            int x,
            int y,
            Direction direction,
            boolean held,
            int buttons,
            boolean power,
            int level,
            boolean anyPowerUp) {
        if (!hasRoom()) {
            components.puzzles().current.indoorPotPullFrames = 0;
            return false;
        }
        return components
                .roomDoorInteractions()
                .tryLiftObject(loadedRoom, x, y, direction, held, buttons, power, level, anyPowerUp);
    }

    boolean tryLiftObject(int x, int y, Direction direction, boolean held, int buttons, boolean power, int level) {
        if (!hasRoom()) {
            components.puzzles().current.indoorPotPullFrames = 0;
            return false;
        }
        return components
                .roomDoorInteractions()
                .tryLiftObject(loadedRoom, x, y, direction, held, buttons, power, level);
    }

    boolean tryLiftIndoorObject(
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask) {
        if (!hasRoom()) {
            components.puzzles().current.indoorPotPullFrames = 0;
            return false;
        }
        return components
                .roomDoorInteractions()
                .tryLiftIndoorObject(
                        loadedRoom, linkPixelX, linkPixelY, linkDirection, braceletButtonHeld, pressedButtonsMask);
    }

    boolean tryLiftIndoorObject(
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask,
            boolean pieceOfPowerActive) {
        if (!hasRoom()) {
            components.puzzles().current.indoorPotPullFrames = 0;
            return false;
        }
        return components
                .roomDoorInteractions()
                .tryLiftIndoorObject(
                        loadedRoom,
                        linkPixelX,
                        linkPixelY,
                        linkDirection,
                        braceletButtonHeld,
                        pressedButtonsMask,
                        pieceOfPowerActive);
    }

    boolean tryInteractWithIndoorBlock(int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType) {
        if (!hasRoom()) {
            return false;
        }
        return components
                .roomDoorInteractions()
                .tryInteractWithIndoorBlock(loadedRoom, linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    boolean tryUnlockIndoorKeyholeBlock(int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType) {
        if (!hasRoom()) {
            return false;
        }
        return components
                .roomDoorInteractions()
                .tryUnlockIndoorKeyholeBlock(loadedRoom.contents, linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    boolean tryUnlockTailCaveKeyhole(
            int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType, boolean hasTailKey) {
        if (!hasRoom()) {
            return false;
        }
        return components
                .roomDoorInteractions()
                .tryUnlockTailCaveKeyhole(
                        loadedRoom.contents, linkPixelX, linkPixelY, linkDirection, collisionType, hasTailKey);
    }

    boolean tryUnlockKeyCavernKeyhole(
            int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType, int goldenLeavesCount) {
        if (!hasRoom()) {
            return false;
        }
        return components
                .roomDoorInteractions()
                .tryUnlockKeyCavernKeyhole(
                        loadedRoom.contents, linkPixelX, linkPixelY, linkDirection, collisionType, goldenLeavesCount);
    }

    int tickTailCaveKeyholeSequence() {
        if (!hasRoom()) {
            components.puzzles().current.tailCaveFinalMotionBlockPending = false;
            return 0;
        }
        return components.roomDoorInteractions().tickTailCaveKeyholeSequence(loadedRoom.contents);
    }

    boolean tailCaveKeyholeSequenceActive() {
        if (!hasRoom()) {
            return components.puzzles().current.tailCaveKeyholeCountdown != 0
                    || components.puzzles().current.tailCaveFinalMotionBlockPending;
        }
        return components.roomDoorInteractions().tailCaveKeyholeSequenceActive(loadedRoom.contents);
    }
}
