package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.rom.RomTables;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.EntityBackgroundCollisionResolver;
import linksawakening.world.entity.EntityCollisionPointProbe;
import linksawakening.world.entity.FollowingNpcEntitySpawner;
import linksawakening.world.entity.RomEnemyCombatTables;
import linksawakening.world.object.OverworldBushInteraction;

/** Private dependency wiring; runtime collaborators receive only their own dependencies. */
final class RoomSessionContext {
    private final RoomSessionComponents.Builder values = new RoomSessionComponents.Builder();

    static RoomSessionComponents create(RoomSessionConfig config) {
        return new RoomSessionContext(config).components();
    }

    private RoomSessionContext(RoomSessionConfig config) {
        initializeResources(config);
        wireEntityStages();
        wireViewsAndRewards();
        wireCollisionAndItems();
        wirePuzzlesAndStory();
        wireDoorsAndLoads();
    }

    private void initializeResources(RoomSessionConfig config) {
        var romData = config.romData();
        var gpu = config.gpu();
        var roomLoader = config.roomLoader();
        var overworldTilesetTable = config.overworldTilesetTable();
        var overworldCollision = config.overworldCollision();
        var transientVfxSystem = config.presentation().transientVfxSystem();
        var droppableRupeeSystem = config.presentation().droppableRupeeSystem();
        var roomLoadListener = config.presentation().roomLoadListener();
        var randomByteSource = config.randomByteSource();
        values.entityRandomByteSource = java.util.Objects.requireNonNull(randomByteSource);
        values.entityBackgroundInteraction = RoomEntityWorldAdapters.background(() -> values.roomCollisionQueries);
        values.colorShellWorld = RoomEntityWorldAdapters.colorShell(
                () -> values.roomObjectMutations, values.entityFrame, transientVfxSystem);
        values.romData = romData;
        values.roomState = new RoomWorldState(romData);
        values.scrollNavigation = new RoomScrollNavigation(romData, values.roomState);
        values.indoorTorchPaletteEffect = new IndoorTorchPaletteEffect(romData);
        values.gpu = gpu;
        values.roomLoader = roomLoader;
        values.roomPaletteLoader = new RoomPaletteLoader(romData);
        values.overworldTilesetTable = overworldTilesetTable;
        values.manboWarpResolver = new ManboWarpResolver(romData);
        values.owlEventDialogResolver = new OwlEventDialogResolver(romData);
        values.owlStatueDialogResolver = new OwlStatueDialogResolver(romData);
        values.overworldCollision = overworldCollision;
        values.transientVfxSystem = transientVfxSystem;
        values.droppableRupeeSystem = droppableRupeeSystem;
        values.roomLoadListener = roomLoadListener;
        values.roomTilemapBuilder = new RoomTilemapBuilder(romData);
        values.entitySpriteHandlerCatalog = new EntitySpriteHandlerCatalog(romData);
        values.followingNpcEntitySpawner = new FollowingNpcEntitySpawner(values.entitySpriteHandlerCatalog);
        values.enemyCombatTables =
                new RomEnemyCombatTables(romData == null ? null : new linksawakening.rom.Rom(romData));
        values.chestContentsTable = new ChestContentsTable(romData);
        values.dungeonRoomEventTable = new DungeonRoomEventTable(romData);
        values.romTables = RomTables.loadFromRom(romData);
        values.entityBackgroundCollisionResolver = new EntityBackgroundCollisionResolver(
                values.romTables.collisionTables(), values.romTables.entityPropertyTables());
        values.enemyDropResolver = new EnemyDropResolver(romData == null ? null : new linksawakening.rom.Rom(romData));
        values.entityCollisionPointProbe = new EntityCollisionPointProbe(
                values.romTables.collisionTables(), values.romTables.entityPropertyTables());
        values.overworldBushInteraction = new OverworldBushInteraction(romData, values.romTables);
    }

    private void wireEntityStages() {
        values.entityAfterTick = new RoomEntityAfterTick(new RoomEntityAfterTick.Dependencies(
                values.entityFrame,
                values.followers,
                values.items,
                values.location,
                values.terrain,
                () -> values.entityBridge,
                () -> values.roomEntityRewards,
                () -> values.roomObjectMutations,
                () -> values.roomPuzzleController,
                () -> values.roomSessionViews,
                values.roomState,
                () -> values.roomTileRedraw,
                () -> values.roomTransitionController,
                values.transientVfxSystem));
        values.entityFrameSetup = new RoomEntityFrameSetup(new RoomEntityFrameSetup.Dependencies(
                values.entityFrame,
                values.followers,
                values.items,
                values.location,
                values.puzzles,
                values.terrain,
                () -> values.entityBridge,
                () -> values.entityInputs,
                values.indoorTorchPaletteEffect,
                values.owlStatueDialogResolver,
                () -> values.roomObjectMutations,
                values.roomState,
                () -> values.roomStoryState,
                () -> values.roomTransitionController));
        values.entityCombatBridge = new RoomEntityCombatBridge(new RoomEntityCombatBridge.Dependencies(
                values.entityFrame,
                values.followers,
                values.location,
                () -> values.entityBridge,
                () -> values.roomEntityRewards,
                values.roomState,
                () -> values.roomStoryState));
        values.roomSessionViews = new RoomSessionViews(new RoomSessionViews.Dependencies(
                values.entityFrame,
                values.location,
                values.puzzles,
                values.terrain,
                () -> values.entityBridge,
                values.indoorTorchPaletteEffect,
                values.overworldCollision,
                () -> values.roomLoadCoordinator,
                values.roomState,
                () -> values.roomStoryState,
                values.scrollNavigation));
    }

    private void wireViewsAndRewards() {

        values.roomTransientState = new RoomTransientState(new RoomTransientState.Dependencies(
                values.entityFrame,
                values.items,
                values.location,
                values.puzzles,
                values.terrain,
                values.droppableRupeeSystem,
                () -> values.entityBridge,
                values.transientVfxSystem));
        values.roomEntityRewards = new RoomEntityRewards(new RoomEntityRewards.Dependencies(
                values.followers,
                values.items,
                values.location,
                () -> values.entityBridge,
                () -> values.entityInputs,
                () -> values.roomSessionViews,
                values.roomState,
                () -> values.roomStoryState));
        values.roomTileRedraw = new RoomTileRedraw(new RoomTileRedraw.Dependencies(
                values.location,
                values.puzzles,
                values.terrain,
                values.romData,
                () -> values.roomObjectMutations,
                () -> values.roomTileRedraw,
                values.roomTilemapBuilder));
        values.roomCollisionQueries = new RoomCollisionQueries(new RoomCollisionQueries.Dependencies(
                values.entityFrame,
                values.location,
                values.puzzles,
                values.terrain,
                values.entityBackgroundCollisionResolver,
                () -> values.entityBridge,
                values.entityCollisionPointProbe,
                values.overworldCollision,
                values.romTables,
                () -> values.roomCollisionQueries));
        values.roomPlayerItemActions = new RoomPlayerItemActions(new RoomPlayerItemActions.Dependencies(
                values.followers,
                values.items,
                values.location,
                () -> values.entityBridge,
                values.overworldBushInteraction,
                values.overworldCollision,
                values.romTables,
                () -> values.roomObjectMutations,
                () -> values.roomPlayerItemActions,
                () -> values.roomSessionViews,
                () -> values.roomTileRedraw));
    }

    private void wireCollisionAndItems() {

        values.roomPuzzleController = new RoomPuzzleController(new RoomPuzzleController.Dependencies(
                values.entityFrame,
                values.location,
                values.puzzles,
                () -> values.entityBridge,
                () -> values.entityInputs,
                values.gpu,
                values.overworldCollision,
                values.romData,
                values.romTables,
                () -> values.roomObjectMutations,
                () -> values.roomPuzzleController,
                () -> values.roomSessionViews,
                values.roomState,
                () -> values.roomStoryState,
                () -> values.roomTileRedraw,
                () -> values.roomTransitionController,
                values.transientVfxSystem));
        values.roomStoryState = new RoomStoryState(new RoomStoryState.Dependencies(
                values.entityFrame,
                values.followers,
                values.location,
                values.puzzles,
                values.terrain,
                values.chestContentsTable,
                values.colorShellWorld,
                values.enemyCombatTables,
                values.entityBackgroundInteraction,
                () -> values.entityBridge,
                () -> values.entityInputs,
                values.entityRandomByteSource,
                values.entitySpriteHandlerCatalog,
                values.followingNpcEntitySpawner,
                values.indoorTorchPaletteEffect,
                values.owlEventDialogResolver,
                values.romData,
                values.romTables,
                () -> values.roomCollisionQueries,
                () -> values.roomLoadCoordinator,
                () -> values.roomSessionViews,
                values.roomState,
                () -> values.roomStoryState,
                () -> values.roomTransientState));
    }

    private void wirePuzzlesAndStory() {

        values.roomDoorInteractions = new RoomDoorInteractions(new RoomDoorInteractions.Dependencies(
                values.entityFrame,
                values.followers,
                values.location,
                values.puzzles,
                values.chestContentsTable,
                () -> values.entityBridge,
                values.overworldBushInteraction,
                values.overworldCollision,
                values.romData,
                () -> values.roomDoorInteractions,
                () -> values.roomObjectMutations,
                () -> values.roomSessionViews,
                values.roomState,
                () -> values.roomTileRedraw));
        values.roomObjectMutations = new RoomObjectMutations(new RoomObjectMutations.Dependencies(
                values.entityFrame,
                values.location,
                values.puzzles,
                values.terrain,
                () -> values.entityBridge,
                values.indoorTorchPaletteEffect,
                values.overworldBushInteraction,
                values.overworldCollision,
                values.romTables,
                () -> values.roomObjectMutations,
                () -> values.roomPlayerItemActions,
                () -> values.roomSessionViews,
                values.roomState,
                () -> values.roomTileRedraw,
                values.transientVfxSystem));
        values.roomTransitionController = new RoomTransitionController(new RoomTransitionController.Dependencies(
                values.entityFrame,
                values.followers,
                values.items,
                values.location,
                values.puzzles,
                () -> values.entityBridge,
                values.gpu,
                values.manboWarpResolver,
                values.romData,
                () -> values.roomLoadCoordinator,
                () -> values.roomSessionViews,
                values.roomState,
                () -> values.roomTransitionController,
                values.scrollNavigation));
    }

    private void wireDoorsAndLoads() {

        values.roomLoadCoordinator = new RoomLoadCoordinator(new RoomLoadCoordinator.Dependencies(
                values.entityFrame,
                values.followers,
                values.location,
                values.puzzles,
                values.terrain,
                values.chestContentsTable,
                values.colorShellWorld,
                values.dungeonRoomEventTable,
                values.enemyCombatTables,
                values.enemyDropResolver,
                values.entityBackgroundInteraction,
                () -> values.entityBridge,
                () -> values.entityInputs,
                values.entityRandomByteSource,
                values.entitySpriteHandlerCatalog,
                values.gpu,
                values.indoorTorchPaletteEffect,
                values.overworldCollision,
                values.overworldTilesetTable,
                values.owlEventDialogResolver,
                values.romData,
                values.romTables,
                () -> values.roomCollisionQueries,
                () -> values.roomLoadCoordinator,
                values.roomLoadListener,
                values.roomLoader,
                () -> values.roomObjectMutations,
                values.roomPaletteLoader,
                () -> values.roomPuzzleController,
                () -> values.roomSessionViews,
                values.roomState,
                () -> values.roomStoryState,
                () -> values.roomTransientState,
                () -> values.roomTransitionController));
        values.entityBridge = new RoomEntityBridge(new RoomEntityBridge.Dependencies(
                values.entityFrame,
                values.followers,
                values.items,
                values.location,
                values.terrain,
                () -> values.entityAfterTick,
                () -> values.entityBridge,
                () -> values.entityCombatBridge,
                () -> values.entityFrameSetup,
                () -> values.entityInputs,
                values.entityRandomByteSource,
                values.gpu,
                values.romData,
                () -> values.roomCollisionQueries,
                () -> values.roomLoadCoordinator));
        values.entityInputs = new RoomEntityInputs(() -> values.entityBridge.entityRuntime);
    }

    private RoomSessionComponents components() {
        return values.build();
    }
}
