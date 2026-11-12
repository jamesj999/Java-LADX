package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import java.util.List;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.entity.EnemyAttackContext;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.LinkAttackClearRequest;

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

    final RoomSessionComponents components;
    private final RoomRoleViews roles;
    private final RoomFrameInputApplier frameInputs;
    private final EntityFrameResults frameResults;
    private final RoomDoors doors;
    private final RoomTransitions transitions;
    private final RoomPlayerItems playerItems;
    private final RoomStory story;

    /** Compatibility for SlimeEyeControlsTest reflection until the entity test migration. */
    @Deprecated(forRemoval = true)
    private final RoomSessionComponents context;

    static long nextRoomSessionId;

    /** Register inputs used by ApplyLinkMotionState/ApplyLinkGroundPhysics ($00:1794/$02:75BD). */

    /** Result of the ROM ShootArrow bridge, including its conditional whoosh. */

    /** Result of the ROM shovel probe made when Link begins using the item. */

    /** Result of the source closed-chest action bridge. */
    public RoomSession(RoomSessionConfig config) {
        components = RoomSessionContext.create(config);
        context = components;
        roles = RoomRoleViews.create(components);
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
                components.roomLoadCoordinator());
        frameResults = new EntityFrameResults(
                new LinkOutputs(components.linkOutputsDependencies()),
                new RewardOutputs(components.rewardOutputsDependencies()),
                new PresentationOutputs(components.presentationOutputsDependencies()));
    }

    public List<EntityProjectileEvent> tickEntities(EntityTickInputs inputs) {
        return components.entityBridge().tickEntities(inputs);
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

    // Deprecated bridges awaiting entity-test migration
    /** @deprecated Use RoomSession(RoomSessionConfig); retained for world/entity/TarinRaccoonRuntimeTest and SlimeEyeRuntimeTest. */
    @Deprecated(forRemoval = true)
    public RoomSession(
            byte[] romData,
            GPU gpu,
            RoomLoader roomLoader,
            OverworldTilesetTable overworldTilesetTable,
            OverworldCollision overworldCollision,
            TransientVfxSystem transientVfxSystem,
            DroppableRupeeSystem droppableRupeeSystem) {
        this(new RoomSessionConfig(
                romData,
                gpu,
                roomLoader,
                overworldTilesetTable,
                overworldCollision,
                new RoomSessionConfig.Presentation(transientVfxSystem, droppableRupeeSystem, null),
                new linksawakening.world.entity.RomRandomByteSource()));
    }

    /** @deprecated Use {@code entities().tickEntities()}. */
    @Deprecated(forRemoval = true)
    public void tickEntities(int frameCounter, int linkEntityX, int linkEntityY) {
        entities().tickEntities(frameCounter, linkEntityX, linkEntityY);
    }

    /** @deprecated Use {@code entities().tickEntities()}. */
    @Deprecated(forRemoval = true)
    public void tickEntities(int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkDirection) {
        entities().tickEntities(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkDirection);
    }

    /** @deprecated Use {@code entities().tickEntities()}. */
    @Deprecated(forRemoval = true)
    public void tickEntities(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            boolean linkAirborne,
            int linkDirection) {
        entities().tickEntities(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkAirborne, linkDirection);
    }

    /** @deprecated Use {@code story().tarinFlag()}. */
    @Deprecated(forRemoval = true)
    public int tarinFlag() {
        return story().tarinFlag();
    }

    /** @deprecated Use {@code story().setTarinFlag()}. */
    @Deprecated(forRemoval = true)
    public void setTarinFlag(int flag) {
        story().setTarinFlag(flag);
    }

    /** @deprecated Use {@code entityState().setEntityDialogActive()}. */
    @Deprecated(forRemoval = true)
    public void setEntityDialogActive(boolean active) {
        entityState().setEntityDialogActive(active);
    }

    /** @deprecated Use {@code entityState().setEntityTalkState()}. */
    @Deprecated(forRemoval = true)
    public void setEntityTalkState(boolean inventoryAppearing, int dialogCooldown, int windowY) {
        entityState().setEntityTalkState(inventoryAppearing, dialogCooldown, windowY);
    }

    /** @deprecated Use {@code entityState().setEntityActionButtonsHeld()}. */
    @Deprecated(forRemoval = true)
    public void setEntityActionButtonsHeld(boolean actionButtonAHeld, boolean actionButtonBHeld) {
        entityState().setEntityActionButtonsHeld(actionButtonAHeld, actionButtonBHeld);
    }

    /** @deprecated Use {@code entityState().setEntityScreenShakeCountdown()}. */
    @Deprecated(forRemoval = true)
    public void setEntityScreenShakeCountdown(int countdown) {
        entityState().setEntityScreenShakeCountdown(countdown);
    }

    /** @deprecated Use {@code entityState().setEntityAttackStepAnimationCountdown()}. */
    @Deprecated(forRemoval = true)
    public void setEntityAttackStepAnimationCountdown(int countdown) {
        entityState().setEntityAttackStepAnimationCountdown(countdown);
    }

    /** @deprecated Use {@code entityState().consumeLinkAttackClearRequests()}. */
    @Deprecated(forRemoval = true)
    public List<LinkAttackClearRequest> consumeLinkAttackClearRequests() {
        return entityState().consumeLinkAttackClearRequests();
    }

    /** @deprecated Use {@code entityState().consumeEntityDialogRequests()}. */
    @Deprecated(forRemoval = true)
    public List<DialogRequest> consumeEntityDialogRequests() {
        return entityState().consumeEntityDialogRequests();
    }

    /** @deprecated Use {@code story().initializeNewGameWorldState()}. */
    @Deprecated(forRemoval = true)
    public void initializeNewGameWorldState() {
        story().initializeNewGameWorldState();
    }

    /** @deprecated Use {@code story().restoreRoomStatuses()}. */
    @Deprecated(forRemoval = true)
    public void restoreRoomStatuses(
            byte[] overworldStatus, byte[] indoorAStatus, byte[] indoorBStatus, byte[] colorDungeonStatus) {
        story().restoreRoomStatuses(overworldStatus, indoorAStatus, indoorBStatus, colorDungeonStatus);
    }

    /** @deprecated Use {@code story().setChestPlayerLevels()}. */
    @Deprecated(forRemoval = true)
    public void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        story().setChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel);
    }

    /** @deprecated Use {@code story().shouldGetLostInMysteriousWoods()}. */
    @Deprecated(forRemoval = true)
    public boolean shouldGetLostInMysteriousWoods() {
        return story().shouldGetLostInMysteriousWoods();
    }

    /** @deprecated Use {@code playerItems().sprinkleMagicPowder()}. */
    @Deprecated(forRemoval = true)
    public boolean sprinkleMagicPowder(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return playerItems().sprinkleMagicPowder(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    /** @deprecated Use {@code entities().tickEntitiesWithProjectileEvents()}. */
    @Deprecated(forRemoval = true)
    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int linkMotionState,
            int linkDirection,
            boolean usingShield) {
        return entities()
                .tickEntitiesWithProjectileEvents(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkEntityZ,
                        linkMotionState,
                        linkDirection,
                        usingShield);
    }

    /** @deprecated Use {@code entities().resolveEntityCombat(EntityCombatInputs)}. Retained for java/src/test/java/linksawakening/world/entity/SlimeEyeRuntimeTest.java. */
    @Deprecated(forRemoval = true)
    public List<EntityCombatEvent> resolveEntityCombat(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            boolean swordCollisionActive,
            int swordX,
            int swordWidth,
            int swordY,
            int swordHeight,
            EnemyAttackContext attackContext) {
        return entities()
                .resolveEntityCombat(new EntityCombatInputs(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkAirborne,
                        0,
                        linkInteractive,
                        new SwordBox(swordCollisionActive, swordX, swordWidth, swordY, swordHeight),
                        attackContext));
    }

    /** @deprecated Use {@code rooms().loadInitialOverworld()}. */
    @Deprecated(forRemoval = true)
    public void loadInitialOverworld(int roomId) {
        rooms().loadInitialOverworld(roomId);
    }

    /** @deprecated Use {@code rooms().loadOverworld()}. */
    @Deprecated(forRemoval = true)
    public void loadOverworld(int roomId) {
        rooms().loadOverworld(roomId);
    }

    /** @deprecated Use {@code rooms().loadIndoor()}. */
    @Deprecated(forRemoval = true)
    public void loadIndoor(int mapId, int roomId) {
        rooms().loadIndoor(mapId, roomId);
    }

    /** @deprecated Use {@code entityState().entityHealth()}. */
    @Deprecated(forRemoval = true)
    public int entityHealth(int slot) {
        return entityState().entityHealth(slot);
    }

    /** @deprecated Use {@code rooms().activeRoom()}. */
    @Deprecated(forRemoval = true)
    public ActiveRoom activeRoom() {
        return rooms().activeRoom();
    }

    /** @deprecated Use {@code rooms().palettes()}. */
    @Deprecated(forRemoval = true)
    public int[][] palettes() {
        return rooms().palettes();
    }

    /** @deprecated Use {@code rooms().renderSnapshot()}. */
    @Deprecated(forRemoval = true)
    public RoomRenderSnapshot renderSnapshot() {
        return rooms().renderSnapshot();
    }
}
