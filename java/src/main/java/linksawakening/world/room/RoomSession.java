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

    /** Applies the live frame inputs in the original setter order before ticking slots. */
    public List<EntityProjectileEvent> tickEntities(EntityFrameInputs inputs) {
        entityState().setEntityDialogActive(inputs.ui().dialogActive());
        entityState()
                .setEntityTalkState(
                        inputs.ui().inventoryAppearing(),
                        inputs.ui().dialogCooldown(),
                        inputs.ui().windowY());
        components.entityInputs().setEntityDialogAskSelectionIndex(inputs.ui().selectionIndex());
        components
                .roomSessionViews()
                .setHeartPiecesCount(inputs.player().progress().heartPieces());
        components.entityInputs().setEntityMusicActive(inputs.ui().musicActive());
        entityState()
                .setEntityActionButtonsHeld(
                        inputs.buttons().aHeld(), inputs.buttons().bHeld());
        components.entityInputs().setEntityJoypadHeld(inputs.buttons().joypadHeld());
        components.entityInputs().setEntityPressedButtonsMask(inputs.buttons().pressedMask());
        entityState().setEntityPowerBraceletButtonHeld(inputs.buttons().braceletHeld());
        entityState().setEntityBombButtonHeld(inputs.buttons().bombHeld());
        entityState().setEntityAttackStepAnimationCountdown(inputs.link().attackCountdown());
        components.entityInputs().setEntityFinalPositionX(inputs.link().finalX());
        story().setBirdKeyOwned(inputs.player().progress().birdKey());
        story().setTailKeyOwned(inputs.player().progress().tailKey());
        components
                .entityInputs()
                .setEntityInventorySlots(
                        inputs.player().inventory().itemA(),
                        inputs.player().inventory().itemB());
        setLiftStrengthBoost(
                inputs.player().inventory().redTunic(),
                inputs.player().inventory().pieceOfPower());
        story().setHouseNpcTradeItem(inputs.player().progress().tradeItem());
        story().setChestPlayerLevels(
                        inputs.player().levels().shield(),
                        inputs.player().levels().sword(),
                        inputs.player().levels().bracelet());
        entityState()
                .setToadstoolPlayerState(
                        inputs.player().progress().hasToadstool(),
                        inputs.player().progress().powder());
        components
                .entityInputs()
                .setEntityGoldenLeavesCount(inputs.player().progress().leaves());
        setEnemyDropPlayerState(
                inputs.player().health().maxHearts(),
                inputs.player().health().health(),
                inputs.player().health().powerUp(),
                inputs.player().health().lowHealth());
        components.entityBridge().outbox.setFreeMovementMode(inputs.link().freeMovement());
        components.entityInputs().setEntityLinkVelocityZ(inputs.link().velocityZ());
        return tickEntities(new EntityTickInputs(
                inputs.frameCounter(),
                new LinkPosition(
                        inputs.link().position().x(),
                        inputs.link().position().y(),
                        inputs.link().position().z(),
                        inputs.link().position().direction(),
                        inputs.link().position().collisionType()),
                new LinkMotion(
                        inputs.link().motion().airborne(),
                        inputs.link().motion().state(),
                        inputs.link().motion().shield(),
                        inputs.link().motion().invincibility(),
                        inputs.link().motion().speedX(),
                        inputs.link().motion().speedY()),
                inputs.player().levels().shield(),
                new SwordBox(
                        inputs.sword().active(),
                        inputs.sword().x(),
                        inputs.sword().width(),
                        inputs.sword().y(),
                        inputs.sword().height())));
    }

    /** Drains only the position channels: applying these can load a different room. */
    List<LinkWrite> consumePositionWrites() {
        var platforms = components.entityBridge().outbox.consumeSideViewPlatformLinkRequests();
        var pots = components.entityBridge().outbox.consumeSideViewPotLinkRequests();
        var positions = components.entityBridge().outbox.consumeLinkFinalPositionRequests();
        return RoomLinkWrites.positions(platforms, pots, positions);
    }

    List<LinkWrite> consumeAttackWrites() {
        return RoomLinkWrites.attacks(
                entityState().consumeLinkAttackClearRequests(),
                components.entityBridge().outbox.consumeLinkAttackStepRequests());
    }

    public EntityFrameResults entityFrameResults() {
        return new EntityFrameResults(
                new LinkOutputs(components.linkOutputsDependencies()),
                new RewardOutputs(components.rewardOutputsDependencies()),
                new PresentationOutputs(components.presentationOutputsDependencies()));
    }

    public RoomDoors doors() {
        return new RoomDoorsView(components.roomDoorInteractions());
    }

    public RoomTransitions transitions() {
        return new RoomTransitionsView(components.roomTransitionController());
    }

    public RoomPlayerItems playerItems() {
        return new RoomPlayerItemsView(components.roomPlayerItemActions());
    }

    public RoomStory story() {
        return new RoomStoryView(components.roomStoryState());
    }

    final RoomSessionComponents components;
    private final RoomRoleViews roles;
    /** Compatibility for SlimeEyeControlsTest reflection until the entity test migration. */
    @Deprecated(forRemoval = true)
    private final RoomSessionComponents context;

    static long nextRoomSessionId;

    void loadIndoor(int mapId, int roomId, int mapCategory, int transitionDirection) {
        components.roomLoadCoordinator().loadIndoor(mapId, roomId, mapCategory, transitionDirection);
    }

    /** Supplies the player fields consumed by the ROM enemy-drop resolver. */
    void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp) {
        components.entityBridge().setEnemyDropPlayerState(maxHearts, health, activePowerUp);
    }

    /** Register inputs used by ApplyLinkMotionState/ApplyLinkGroundPhysics ($00:1794/$02:75BD). */

    /** Result of the ROM ShootArrow bridge, including its conditional whoosh. */

    /**
     * Applies the ROM pickup collision cadence to the active room entities.
     * Item-specific effects are returned to the gameplay layer so room state
     * and persistent entity state stay separate.
     */
    void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp, boolean lowHealth) {
        setEnemyDropPlayerState(maxHearts, health, activePowerUp);
        components.entityFrame().enemyDropLowHealth = lowHealth;
        components.roomLoadCoordinator().configureEnemyDropRuntime();
    }

    /** Result of the ROM shovel probe made when Link begins using the item. */

    /** Result of the source closed-chest action bridge. */

    /** Mirrors bank 0's sustained Power Bracelet pull on indoor object $20/$8E. */
    void setLiftStrengthBoost(boolean redTunic, boolean pieceOfPower) {
        if (components.entityBridge().entityRuntime != null) {
            components.entityBridge().entityRuntime.configuration().setLiftStrengthBoost(redTunic, pieceOfPower);
        }
    }

    public RoomSession(RoomSessionConfig config) {
        components = RoomSessionContext.create(config);
        context = components;
        roles = RoomRoleViews.create(components);
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
    /** @deprecated Use RoomSession(RoomSessionConfig); retained for world/entity/TarinRaccoonRuntimeTest. */
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
