package linksawakening.world.room;

import java.util.List;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.entity.event.EntityProjectileEvent;

/** Public room API delegating to room-scoped collaborators. */
public final class RoomSession extends RoomSessionRules
        implements RoomNavigationCompatibility,
                RoomProgressCompatibility,
                RoomInspectionCompatibility,
                RoomStoryCompatibility,
                RoomEntityCompatibility,
                RoomItemCompatibility,
                RoomInteractionCompatibility,
                RoomEntitySimulationCompatibility1,
                RoomEntitySimulationCompatibility2 {
    @Override
    public RoomSession facade() {
        return this;
    }

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

    public RoomSession(
            byte[] romData,
            GPU gpu,
            RoomLoader roomLoader,
            OverworldTilesetTable overworldTilesetTable,
            OverworldCollision overworldCollision,
            TransientVfxSystem transientVfxSystem,
            DroppableRupeeSystem droppableRupeeSystem) {
        this(
                romData,
                gpu,
                roomLoader,
                overworldTilesetTable,
                overworldCollision,
                transientVfxSystem,
                droppableRupeeSystem,
                null);
    }

    public RoomSession(
            byte[] romData,
            GPU gpu,
            RoomLoader roomLoader,
            OverworldTilesetTable overworldTilesetTable,
            OverworldCollision overworldCollision,
            TransientVfxSystem transientVfxSystem,
            DroppableRupeeSystem droppableRupeeSystem,
            RoomLoadListener roomLoadListener) {
        this(new RoomSessionConfig(
                romData,
                gpu,
                roomLoader,
                overworldTilesetTable,
                overworldCollision,
                new RoomSessionConfig.Presentation(transientVfxSystem, droppableRupeeSystem, roomLoadListener),
                new linksawakening.world.entity.RomRandomByteSource()));
    }

    /** @deprecated Use RoomSession(RoomSessionConfig); retained for app/SessionBootstrap.java. */
    @Deprecated(forRemoval = true)
    /** Shares GetRandomByte's single wRandomSeed with equipment and world actions. */
    public RoomSession(
            byte[] romData,
            GPU gpu,
            RoomLoader roomLoader,
            OverworldTilesetTable overworldTilesetTable,
            OverworldCollision overworldCollision,
            TransientVfxSystem transientVfxSystem,
            DroppableRupeeSystem droppableRupeeSystem,
            RoomLoadListener roomLoadListener,
            linksawakening.world.entity.RomRandomByteSource randomByteSource) {
        this(new RoomSessionConfig(
                romData,
                gpu,
                roomLoader,
                overworldTilesetTable,
                overworldCollision,
                new RoomSessionConfig.Presentation(transientVfxSystem, droppableRupeeSystem, roomLoadListener),
                randomByteSource));
    }

    void loadIndoor(int mapId, int roomId, int mapCategory, int transitionDirection) {
        components.roomLoadCoordinator().loadIndoor(mapId, roomId, mapCategory, transitionDirection);
    }

    /** Supplies the player fields consumed by the ROM enemy-drop resolver. */
    void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp) {
        components.entityBridge().setEnemyDropPlayerState(maxHearts, health, activePowerUp);
    }

    /** Register inputs used by ApplyLinkMotionState/ApplyLinkGroundPhysics ($00:1794/$02:75BD). */
    /** Move to top-level after app merge. */
    @Deprecated(forRemoval = true)
    public record LinkGroundFrame(
            int pixelX,
            int pixelY,
            boolean airborne,
            boolean interactionBlocked,
            boolean dialogActive,
            boolean roomTransitionActive,
            int motionState,
            int slowWalkingSpeed,
            int itemUsageContext) {}

    /** @deprecated Use {@code playerItems().placeBomb()}. */
    @Deprecated(forRemoval = true)
    public boolean placeBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return playerItems().placeBomb(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    /** Result of the ROM ShootArrow bridge, including its conditional whoosh. */
    /** Move to top-level after app merge. */
    @Deprecated(forRemoval = true)
    public record ArrowShotResult(boolean spawned, boolean playWhoosh) {}

    /** @deprecated Use {@code entities().tickEntities()}. */
    @Deprecated(forRemoval = true)
    public void tickEntities(int frameCounter) {
        entities().tickEntities(frameCounter);
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
    /** Move to top-level after app merge. */
    @Deprecated(forRemoval = true)
    public record ShovelStartResult(boolean started, boolean poking, int location) {}

    /** Result of the source closed-chest action bridge. */
    /** Move to top-level after app merge. */
    @Deprecated(forRemoval = true)
    public record ChestOpenResult(boolean opened, int itemType, int entitySlot, int location) {}

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
    // Declared bridges preserve DebugMenu method-reference owners until the app merge.
    @Override
    /** @deprecated Use {@code story().bowWowState()}. */
    @Deprecated(forRemoval = true)
    public int bowWowState() {
        return story().bowWowState();
    }

    @Override
    /** @deprecated Use {@code story().setBowWowState()}. */
    @Deprecated(forRemoval = true)
    public void setBowWowState(int state) {
        story().setBowWowState(state);
    }

    @Override
    /** @deprecated Use {@code story().richardSpokenFlag()}. */
    @Deprecated(forRemoval = true)
    public int richardSpokenFlag() {
        return story().richardSpokenFlag();
    }

    @Override
    /** @deprecated Use {@code story().setRichardSpokenFlag()}. */
    @Deprecated(forRemoval = true)
    public void setRichardSpokenFlag(int flag) {
        story().setRichardSpokenFlag(flag);
    }

    @Override
    /** @deprecated Use {@code story().tarinFlag()}. */
    @Deprecated(forRemoval = true)
    public int tarinFlag() {
        return story().tarinFlag();
    }

    @Override
    /** @deprecated Use {@code story().setTarinFlag()}. */
    @Deprecated(forRemoval = true)
    public void setTarinFlag(int flag) {
        story().setTarinFlag(flag);
    }
}
