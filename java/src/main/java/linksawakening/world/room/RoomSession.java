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
        setEntityDialogActive(inputs.ui().dialogActive());
        setEntityTalkState(
                inputs.ui().inventoryAppearing(),
                inputs.ui().dialogCooldown(),
                inputs.ui().windowY());
        context.entityInputs.setEntityDialogAskSelectionIndex(inputs.ui().selectionIndex());
        context.roomSessionViews.setHeartPiecesCount(inputs.player().progress().heartPieces());
        context.entityInputs.setEntityMusicActive(inputs.ui().musicActive());
        setEntityActionButtonsHeld(inputs.buttons().aHeld(), inputs.buttons().bHeld());
        context.entityInputs.setEntityJoypadHeld(inputs.buttons().joypadHeld());
        context.entityInputs.setEntityPressedButtonsMask(inputs.buttons().pressedMask());
        setEntityPowerBraceletButtonHeld(inputs.buttons().braceletHeld());
        setEntityBombButtonHeld(inputs.buttons().bombHeld());
        setEntityAttackStepAnimationCountdown(inputs.link().attackCountdown());
        context.entityInputs.setEntityFinalPositionX(inputs.link().finalX());
        setBirdKeyOwned(inputs.player().progress().birdKey());
        setTailKeyOwned(inputs.player().progress().tailKey());
        context.entityInputs.setEntityInventorySlots(
                inputs.player().inventory().itemA(), inputs.player().inventory().itemB());
        setLiftStrengthBoost(
                inputs.player().inventory().redTunic(),
                inputs.player().inventory().pieceOfPower());
        setHouseNpcTradeItem(inputs.player().progress().tradeItem());
        setChestPlayerLevels(
                inputs.player().levels().shield(),
                inputs.player().levels().sword(),
                inputs.player().levels().bracelet());
        setToadstoolPlayerState(
                inputs.player().progress().hasToadstool(),
                inputs.player().progress().powder());
        context.entityInputs.setEntityGoldenLeavesCount(
                inputs.player().progress().leaves());
        setEnemyDropPlayerState(
                inputs.player().health().maxHearts(),
                inputs.player().health().health(),
                inputs.player().health().powerUp(),
                inputs.player().health().lowHealth());
        context.entityBridge.outbox.setFreeMovementMode(inputs.link().freeMovement());
        context.entityInputs.setEntityLinkVelocityZ(inputs.link().velocityZ());
        return tickEntitiesWithProjectileEvents(
                inputs.frameCounter(),
                inputs.link().position().x(),
                inputs.link().position().y(),
                inputs.link().position().z(),
                inputs.link().motion().airborne(),
                inputs.link().motion().state(),
                inputs.link().position().direction(),
                inputs.link().position().collisionType(),
                inputs.link().motion().shield(),
                inputs.player().levels().shield(),
                inputs.link().motion().invincibility(),
                inputs.sword().active(),
                inputs.sword().x(),
                inputs.sword().width(),
                inputs.sword().y(),
                inputs.sword().height(),
                inputs.link().motion().speedX(),
                inputs.link().motion().speedY());
    }

    /** Drains only the position channels: applying these can load a different room. */
    List<LinkWrite> consumePositionWrites() {
        var platforms = context.entityBridge.outbox.consumeSideViewPlatformLinkRequests();
        var pots = context.entityBridge.outbox.consumeSideViewPotLinkRequests();
        var positions = context.entityBridge.outbox.consumeLinkFinalPositionRequests();
        return RoomLinkWrites.positions(platforms, pots, positions);
    }

    List<LinkWrite> consumeAttackWrites() {
        return RoomLinkWrites.attacks(
                consumeLinkAttackClearRequests(), context.entityBridge.outbox.consumeLinkAttackStepRequests());
    }

    public EntityFrameResults entityFrameResults() {
        return new EntityFrameResults(new LinkOutputs(this), new RewardOutputs(this), new PresentationOutputs(this));
    }

    public RoomDoors doors() {
        return new RoomDoorsView(context.roomDoorInteractions);
    }

    public RoomTransitions transitions() {
        return new RoomTransitionsView(context.roomTransitionController);
    }

    public RoomPlayerItems playerItems() {
        return new RoomPlayerItemsView(context.roomPlayerItemActions);
    }

    public RoomStory story() {
        return new RoomStoryView(context.roomStoryState);
    }

    final RoomSessionContext context;
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
        this(
                romData,
                gpu,
                roomLoader,
                overworldTilesetTable,
                overworldCollision,
                transientVfxSystem,
                droppableRupeeSystem,
                roomLoadListener,
                new linksawakening.world.entity.RomRandomByteSource());
    }

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
        context = new RoomSessionContext(
                this,
                romData,
                gpu,
                roomLoader,
                overworldTilesetTable,
                overworldCollision,
                transientVfxSystem,
                droppableRupeeSystem,
                roomLoadListener,
                randomByteSource);
    }

    void loadIndoor(int mapId, int roomId, int mapCategory, int transitionDirection) {
        context.roomLoadCoordinator.loadIndoor(mapId, roomId, mapCategory, transitionDirection);
    }

    /** Supplies the player fields consumed by the ROM enemy-drop resolver. */
    void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp) {
        context.entityBridge.setEnemyDropPlayerState(maxHearts, health, activePowerUp);
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

    /**
     * Mirrors the ordinary player-bomb placement bridge. The item has already
     * applied PlaceBomb's inventory ordering; this method owns the room/runtime
     * allocation and immutable snapshot refresh.
     */
    public boolean placeBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return playerItems().placeBomb(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    /** Result of the ROM ShootArrow bridge, including its conditional whoosh. */
    /** Move to top-level after app merge. */
    @Deprecated(forRemoval = true)
    public record ArrowShotResult(boolean spawned, boolean playWhoosh) {}

    public void tickEntities(int frameCounter) {
        context.entityBridge.tickEntities(frameCounter);
    }

    public void tickEntities(int frameCounter, int linkEntityX, int linkEntityY) {
        context.entityBridge.tickEntities(frameCounter, linkEntityX, linkEntityY);
    }

    public void tickEntities(int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkDirection) {
        context.entityBridge.tickEntities(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkDirection);
    }

    public void tickEntities(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            boolean linkAirborne,
            int linkDirection) {
        context.entityBridge.tickEntities(
                frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkAirborne, linkDirection);
    }

    /**
     * Applies the ROM pickup collision cadence to the active room entities.
     * Item-specific effects are returned to the gameplay layer so room state
     * and persistent entity state stay separate.
     */
    void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp, boolean lowHealth) {
        setEnemyDropPlayerState(maxHearts, health, activePowerUp);
        context.enemyDropLowHealth = lowHealth;
        context.roomLoadCoordinator.configureEnemyDropRuntime();
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
        if (context.entityBridge.entityRuntime != null) {
            context.entityBridge.entityRuntime.configuration().setLiftStrengthBoost(redTunic, pieceOfPower);
        }
    }
}
