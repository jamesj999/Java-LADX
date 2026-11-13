package linksawakening.world.room;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.rom.RomTables;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.EntityBackgroundCollisionResolver;
import linksawakening.world.entity.EntityCollisionPointProbe;
import linksawakening.world.entity.FollowingNpcEntitySpawner;
import linksawakening.world.entity.RomEnemyCombatTables;
import linksawakening.world.object.OverworldBushInteraction;

/** Private, eager dependency wiring; no collaborator lookup is deferred to hide initialization order. */
final class RoomSessionContext {
    private final RoomSessionComponents.Builder values = new RoomSessionComponents.Builder();

    static RoomSessionComponents create(
            RoomSessionConfig config,
            java.util.function.Consumer<RoomRuntime> installRoom,
            java.util.function.UnaryOperator<RoomRuntime> roomLoaded,
            Runnable resetRoomTransients) {
        return new RoomSessionContext(config, installRoom, roomLoaded, resetRoomTransients).values.build();
    }

    private RoomSessionContext(
            RoomSessionConfig config,
            java.util.function.Consumer<RoomRuntime> installRoom,
            java.util.function.UnaryOperator<RoomRuntime> roomLoaded,
            Runnable resetRoomTransients) {

        var romData = config.romData();
        var gpu = config.gpu();
        var roomLoader = config.roomLoader();
        var overworldTilesetTable = config.overworldTilesetTable();
        var overworldCollision = config.overworldCollision();
        boolean transientVfxEnabled = config.presentation().transientVfxSystem() != null;
        var transientVfxSystem = transientVfxEnabled
                ? config.presentation().transientVfxSystem()
                : new linksawakening.vfx.TransientVfxSystem(0);
        var droppableRupeeSystem = config.presentation().droppableRupeeSystem() == null
                ? new DroppableRupeeSystem(0, () -> 0)
                : config.presentation().droppableRupeeSystem();
        var randomByteSource = config.randomByteSource();
        values.entityRandomByteSource = java.util.Objects.requireNonNull(randomByteSource);
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
        values.roomTilemapBuilder = new RoomTilemapBuilder(romData);
        values.entitySpriteHandlerCatalog = new EntitySpriteHandlerCatalog(romData);
        values.followingNpcEntitySpawner = new FollowingNpcEntitySpawner(values.entitySpriteHandlerCatalog);
        values.enemyCombatTables = new RomEnemyCombatTables(romData);
        values.chestContentsTable = new ChestContentsTable(romData);
        values.dungeonRoomEventTable = new DungeonRoomEventTable(romData);
        values.romTables = RomTables.load(romData);
        values.entityBackgroundCollisionResolver = new EntityBackgroundCollisionResolver(
                values.romTables.collisionTables(), values.romTables.entityPropertyTables());
        values.enemyDropResolver = new EnemyDropResolver(romData);
        values.entityCollisionPointProbe = new EntityCollisionPointProbe(
                values.romTables.collisionTables(), values.romTables.entityPropertyTables());
        values.overworldBushInteraction = new OverworldBushInteraction(romData, values.romTables);
        values.roomSessionViews = new RoomSessionViews(new RoomSessionViews.Dependencies(
                values.entityFrame, values.location, values.roomState, values.scrollNavigation));
        values.roomTransientState = new RoomTransientState(new RoomTransientState.Dependencies(
                values.entityFrame,
                values.items,
                values.location,
                values.droppableRupeeSystem,
                values.entityFrame,
                values.transientVfxSystem));
        values.roomCollisionQueries = new RoomCollisionQueries(new RoomCollisionQueries.Dependencies(
                values.entityFrame,
                values.terrain,
                values.entityBackgroundCollisionResolver,
                values.entityCollisionPointProbe,
                values.overworldCollision,
                values.romTables));
        values.roomObjectMutations = new RoomObjectMutations(new RoomObjectMutations.Dependencies(
                values.entityFrame,
                values.location,
                values.puzzles,
                values.terrain,
                values.entityFrame,
                values.indoorTorchPaletteEffect,
                values.overworldBushInteraction,
                values.overworldCollision,
                values.romTables,
                values.roomSessionViews,
                values.roomState,
                values.transientVfxSystem,
                values.romData,
                values.roomTilemapBuilder));
        values.entityInputs = new RoomEntityInputs();
        values.roomRuntimeServices = new RoomRuntime.Services(
                values.roomCollisionQueries, values.roomObjectMutations, values.entityFrame, transientVfxSystem);
        values.roomPlayerItemActions = new RoomPlayerItemActions(new RoomPlayerItemActions.Dependencies(
                values.followers,
                values.items,
                values.location,
                values.entityFrame,
                values.overworldBushInteraction,
                values.overworldCollision,
                values.romTables,
                values.roomObjectMutations,
                values.roomSessionViews,
                values.roomObjectMutations));
        values.roomPuzzleController = new RoomPuzzleController(new RoomPuzzleController.Dependencies(
                values.entityFrame,
                values.puzzles,
                values.entityFrame,
                values.entityInputs,
                values.gpu,
                values.overworldCollision,
                values.romData,
                values.romTables,
                values.roomObjectMutations,
                values.roomSessionViews,
                values.roomState,
                values.roomObjectMutations,
                values.transientVfxSystem));
        values.roomStoryState = new RoomStoryState(new RoomStoryState.Dependencies(
                values.entityFrame,
                values.followers,
                values.location,
                values.puzzles,
                values.terrain,
                values.chestContentsTable,
                values.enemyCombatTables,
                values.entityInputs,
                values.entityRandomByteSource,
                values.entitySpriteHandlerCatalog,
                values.followingNpcEntitySpawner,
                values.indoorTorchPaletteEffect,
                values.owlEventDialogResolver,
                values.romData,
                values.romTables,
                values.enemyDropResolver,
                values.roomSessionViews,
                values.roomState,
                installRoom,
                values.roomRuntimeServices));
        values.roomDoorInteractions = new RoomDoorInteractions(new RoomDoorInteractions.Dependencies(
                values.entityFrame,
                values.followers,
                values.location,
                values.puzzles,
                values.chestContentsTable,
                values.entityFrame,
                values.overworldBushInteraction,
                values.overworldCollision,
                values.romData,
                values.roomObjectMutations,
                values.roomSessionViews,
                values.roomState,
                values.roomObjectMutations));
        values.roomLoadCoordinator = new RoomLoadCoordinator(new RoomLoadCoordinator.Dependencies(
                values.entityFrame,
                values.followers,
                values.location,
                values.terrain,
                values.chestContentsTable,
                values.dungeonRoomEventTable,
                values.enemyCombatTables,
                values.enemyDropResolver,
                values.entityInputs,
                values.entityRandomByteSource,
                values.entitySpriteHandlerCatalog,
                values.gpu,
                values.indoorTorchPaletteEffect,
                values.overworldCollision,
                values.overworldTilesetTable,
                values.owlEventDialogResolver,
                values.romData,
                values.romTables,
                roomLoaded,
                values.roomLoader,
                values.roomObjectMutations,
                values.roomPaletteLoader,
                values.roomPuzzleController,
                values.roomSessionViews,
                values.roomState,
                values.roomStoryState,
                resetRoomTransients,
                installRoom,
                values.roomRuntimeServices));
        values.roomEntityRewards = new RoomEntityRewards(new RoomEntityRewards.Dependencies(
                values.followers,
                values.items,
                values.entityFrame,
                values.entityInputs,
                values.roomSessionViews,
                values.roomState,
                values.roomStoryState));
        values.roomTransitionController = new RoomTransitionController(new RoomTransitionController.Dependencies(
                values.followers,
                values.items,
                values.location,
                values.entityFrame,
                values.manboWarpResolver,
                values.roomLoadCoordinator,
                values.roomState,
                values.scrollNavigation));
        values.entityEffects = new RoomEntitySimulation.EffectsDependencies(
                values.entityFrame,
                values.followers,
                values.items,
                values.location,
                values.terrain,
                values.entityFrame,
                values.roomEntityRewards,
                values.roomObjectMutations,
                values.roomPuzzleController,
                values.roomSessionViews,
                values.roomState,
                values.roomObjectMutations,
                values.roomTransitionController,
                values.transientVfxSystem,
                transientVfxEnabled);
        values.entityConfiguration = new RoomEntitySimulation.SetupDependencies(
                values.entityFrame,
                values.followers,
                values.items,
                values.location,
                values.puzzles,
                values.terrain,
                values.entityFrame,
                values.entityInputs,
                values.indoorTorchPaletteEffect,
                values.owlStatueDialogResolver,
                values.roomObjectMutations,
                values.roomState,
                values.roomStoryState,
                values.roomTransitionController);
        values.entityCombatBridge = new RoomEntityCombatBridge(new RoomEntityCombatBridge.Dependencies(
                values.entityFrame, values.followers, values.entityFrame, values.roomEntityRewards, values.roomState));
        values.entityBridge = new RoomEntityBridge(new RoomEntityBridge.Dependencies(
                values.entityFrame,
                values.followers,
                values.items,
                values.location,
                values.terrain,
                values.entityEffects,
                values.entityCombatBridge,
                values.entityConfiguration,
                values.entityRandomByteSource,
                values.gpu,
                values.romData,
                values.roomLoadCoordinator));
    }
}
