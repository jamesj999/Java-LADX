package linksawakening.world.room;

import java.util.List;
import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.event.EntityProjectileEvent;

/** Public room API delegating to room-scoped collaborators. */
public final class RoomSession {

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
        components.entityFrame().runtime = room.entities;
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
        doors = new RoomDoorsView(components.roomDoorInteractions());
        transitions = new RoomTransitionsView(components.roomTransitionController());
        playerItems = new RoomPlayerItemsView(components.roomPlayerItemActions());
        story = new RoomStoryView(components.roomStoryState());
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
                new LinkOutputs(components.linkOutputsDependencies()),
                new RewardOutputs(components.rewardOutputsDependencies()),
                new PresentationOutputs(components.presentationOutputsDependencies()));
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
}
