package linksawakening.world.room;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.EntityBackgroundCollisionResolver;
import linksawakening.world.entity.EntityCollisionPointProbe;
import linksawakening.world.entity.FollowingNpcEntitySpawner;
import linksawakening.world.entity.RomEnemyCombatTables;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.object.OverworldBushInteraction;

/** Construction inventory used only inside RoomSessionContext; never retained by a runtime role. */
record RoomSessionComponents(
        RoomLocationState location,
        RoomTerrainState terrain,
        RoomFollowerState followers,
        RoomEntitySessionState entityFrame,
        RoomItemUseState items,
        RoomPuzzleState puzzles,
        Rom romData,
        GPU gpu,
        RoomLoader roomLoader,
        RoomPaletteLoader roomPaletteLoader,
        OverworldTilesetTable overworldTilesetTable,
        ManboWarpResolver manboWarpResolver,
        OwlEventDialogResolver owlEventDialogResolver,
        OwlStatueDialogResolver owlStatueDialogResolver,
        OverworldCollision overworldCollision,
        TransientVfxSystem transientVfxSystem,
        DroppableRupeeSystem droppableRupeeSystem,
        RoomTilemapBuilder roomTilemapBuilder,
        RomRandomByteSource entityRandomByteSource,
        EntitySpriteHandlerCatalog entitySpriteHandlerCatalog,
        FollowingNpcEntitySpawner followingNpcEntitySpawner,
        RomEnemyCombatTables enemyCombatTables,
        ChestContentsTable chestContentsTable,
        DungeonRoomEventTable dungeonRoomEventTable,
        RomTables romTables,
        EnemyDropResolver enemyDropResolver,
        EntityCollisionPointProbe entityCollisionPointProbe,
        EntityBackgroundCollisionResolver entityBackgroundCollisionResolver,
        OverworldBushInteraction overworldBushInteraction,
        IndoorTorchPaletteEffect indoorTorchPaletteEffect,
        RoomWorldState roomState,
        RoomScrollNavigation scrollNavigation,
        RoomSessionViews roomSessionViews,
        RoomTransientState roomTransientState,
        RoomEntityRewards roomEntityRewards,
        RoomCollisionQueries roomCollisionQueries,
        RoomPlayerItemActions roomPlayerItemActions,
        RoomPuzzleController roomPuzzleController,
        RoomStoryState roomStoryState,
        RoomDoorInteractions roomDoorInteractions,
        RoomObjectMutations roomObjectMutations,
        RoomTransitionController roomTransitionController,
        RoomLoadCoordinator roomLoadCoordinator,
        RoomEntityBridge entityBridge,
        RoomEntityInputs entityInputs,
        RoomEntitySimulation.EffectsDependencies entityEffects,
        RoomEntitySimulation.SetupDependencies entityConfiguration,
        LinkOutputs.Dependencies linkOutputsDependencies,
        RewardOutputs.Dependencies rewardOutputsDependencies,
        RoomRuntime.Services roomRuntimeServices) {
    static final class Builder {
        private RoomRuntime.Services roomRuntimeServices;
        private Rom romData;
        private GPU gpu;
        private RoomLoader roomLoader;
        private RoomPaletteLoader roomPaletteLoader;
        private OverworldTilesetTable overworldTilesetTable;
        private ManboWarpResolver manboWarpResolver;
        private OwlEventDialogResolver owlEventDialogResolver;
        private OwlStatueDialogResolver owlStatueDialogResolver;
        private OverworldCollision overworldCollision;
        private TransientVfxSystem transientVfxSystem;
        private DroppableRupeeSystem droppableRupeeSystem;
        private RoomTilemapBuilder roomTilemapBuilder;
        private RomRandomByteSource entityRandomByteSource;
        private EntitySpriteHandlerCatalog entitySpriteHandlerCatalog;
        private FollowingNpcEntitySpawner followingNpcEntitySpawner;
        private RomEnemyCombatTables enemyCombatTables;
        private ChestContentsTable chestContentsTable;
        private DungeonRoomEventTable dungeonRoomEventTable;
        private RomTables romTables;
        private EnemyDropResolver enemyDropResolver;
        private EntityCollisionPointProbe entityCollisionPointProbe;
        private EntityBackgroundCollisionResolver entityBackgroundCollisionResolver;
        private OverworldBushInteraction overworldBushInteraction;
        private IndoorTorchPaletteEffect indoorTorchPaletteEffect;
        private RoomWorldState roomState;
        private RoomScrollNavigation scrollNavigation;
        private RoomSessionViews roomSessionViews;
        private RoomTransientState roomTransientState;
        private RoomEntityRewards roomEntityRewards;
        private RoomCollisionQueries roomCollisionQueries;
        private RoomPlayerItemActions roomPlayerItemActions;
        private RoomPuzzleController roomPuzzleController;
        private RoomStoryState roomStoryState;
        private RoomDoorInteractions roomDoorInteractions;
        private RoomObjectMutations roomObjectMutations;
        private RoomTransitionController roomTransitionController;
        private RoomLoadCoordinator roomLoadCoordinator;
        private RoomEntityBridge entityBridge;
        private RoomEntityInputs entityInputs;
        private RoomEntitySimulation.EffectsDependencies entityEffects;
        private RoomEntitySimulation.SetupDependencies entityConfiguration;
        private final RoomLocationState location = new RoomLocationState();
        private final RoomTerrainState terrain = new RoomTerrainState();
        private final RoomFollowerState followers = new RoomFollowerState();
        private final RoomEntitySessionState entityFrame = new RoomEntitySessionState();
        private final RoomItemUseState items = new RoomItemUseState();
        private final RoomPuzzleState puzzles = new RoomPuzzleState();

        RoomSessionComponents build() {
            return new RoomSessionComponents(
                    location,
                    terrain,
                    followers,
                    entityFrame,
                    items,
                    puzzles,
                    romData,
                    gpu,
                    roomLoader,
                    roomPaletteLoader,
                    overworldTilesetTable,
                    manboWarpResolver,
                    owlEventDialogResolver,
                    owlStatueDialogResolver,
                    overworldCollision,
                    transientVfxSystem,
                    droppableRupeeSystem,
                    roomTilemapBuilder,
                    entityRandomByteSource,
                    entitySpriteHandlerCatalog,
                    followingNpcEntitySpawner,
                    enemyCombatTables,
                    chestContentsTable,
                    dungeonRoomEventTable,
                    romTables,
                    enemyDropResolver,
                    entityCollisionPointProbe,
                    entityBackgroundCollisionResolver,
                    overworldBushInteraction,
                    indoorTorchPaletteEffect,
                    roomState,
                    scrollNavigation,
                    roomSessionViews,
                    roomTransientState,
                    roomEntityRewards,
                    roomCollisionQueries,
                    roomPlayerItemActions,
                    roomPuzzleController,
                    roomStoryState,
                    roomDoorInteractions,
                    roomObjectMutations,
                    roomTransitionController,
                    roomLoadCoordinator,
                    entityBridge,
                    entityInputs,
                    entityEffects,
                    entityConfiguration,
                    new LinkOutputs.Dependencies(entityBridge),
                    new RewardOutputs.Dependencies(entityFrame),
                    roomRuntimeServices);
        }

        RomRandomByteSource entityRandomByteSource() {
            return this.entityRandomByteSource;
        }

        RomRandomByteSource entityRandomByteSource(RomRandomByteSource value) {
            this.entityRandomByteSource = value;
            return this.entityRandomByteSource;
        }

        Rom romData() {
            return this.romData;
        }

        Rom romData(Rom value) {
            this.romData = value;
            return this.romData;
        }

        RoomWorldState roomState() {
            return this.roomState;
        }

        RoomWorldState roomState(RoomWorldState value) {
            this.roomState = value;
            return this.roomState;
        }

        RoomScrollNavigation scrollNavigation() {
            return this.scrollNavigation;
        }

        RoomScrollNavigation scrollNavigation(RoomScrollNavigation value) {
            this.scrollNavigation = value;
            return this.scrollNavigation;
        }

        IndoorTorchPaletteEffect indoorTorchPaletteEffect() {
            return this.indoorTorchPaletteEffect;
        }

        IndoorTorchPaletteEffect indoorTorchPaletteEffect(IndoorTorchPaletteEffect value) {
            this.indoorTorchPaletteEffect = value;
            return this.indoorTorchPaletteEffect;
        }

        GPU gpu() {
            return this.gpu;
        }

        GPU gpu(GPU value) {
            this.gpu = value;
            return this.gpu;
        }

        RoomLoader roomLoader() {
            return this.roomLoader;
        }

        RoomLoader roomLoader(RoomLoader value) {
            this.roomLoader = value;
            return this.roomLoader;
        }

        RoomPaletteLoader roomPaletteLoader() {
            return this.roomPaletteLoader;
        }

        RoomPaletteLoader roomPaletteLoader(RoomPaletteLoader value) {
            this.roomPaletteLoader = value;
            return this.roomPaletteLoader;
        }

        OverworldTilesetTable overworldTilesetTable() {
            return this.overworldTilesetTable;
        }

        OverworldTilesetTable overworldTilesetTable(OverworldTilesetTable value) {
            this.overworldTilesetTable = value;
            return this.overworldTilesetTable;
        }

        ManboWarpResolver manboWarpResolver() {
            return this.manboWarpResolver;
        }

        ManboWarpResolver manboWarpResolver(ManboWarpResolver value) {
            this.manboWarpResolver = value;
            return this.manboWarpResolver;
        }

        OwlEventDialogResolver owlEventDialogResolver() {
            return this.owlEventDialogResolver;
        }

        OwlEventDialogResolver owlEventDialogResolver(OwlEventDialogResolver value) {
            this.owlEventDialogResolver = value;
            return this.owlEventDialogResolver;
        }

        OwlStatueDialogResolver owlStatueDialogResolver() {
            return this.owlStatueDialogResolver;
        }

        OwlStatueDialogResolver owlStatueDialogResolver(OwlStatueDialogResolver value) {
            this.owlStatueDialogResolver = value;
            return this.owlStatueDialogResolver;
        }

        OverworldCollision overworldCollision() {
            return this.overworldCollision;
        }

        OverworldCollision overworldCollision(OverworldCollision value) {
            this.overworldCollision = value;
            return this.overworldCollision;
        }

        TransientVfxSystem transientVfxSystem() {
            return this.transientVfxSystem;
        }

        TransientVfxSystem transientVfxSystem(TransientVfxSystem value) {
            this.transientVfxSystem = value;
            return this.transientVfxSystem;
        }

        DroppableRupeeSystem droppableRupeeSystem() {
            return this.droppableRupeeSystem;
        }

        DroppableRupeeSystem droppableRupeeSystem(DroppableRupeeSystem value) {
            this.droppableRupeeSystem = value;
            return this.droppableRupeeSystem;
        }

        RoomTilemapBuilder roomTilemapBuilder() {
            return this.roomTilemapBuilder;
        }

        RoomTilemapBuilder roomTilemapBuilder(RoomTilemapBuilder value) {
            this.roomTilemapBuilder = value;
            return this.roomTilemapBuilder;
        }

        EntitySpriteHandlerCatalog entitySpriteHandlerCatalog() {
            return this.entitySpriteHandlerCatalog;
        }

        EntitySpriteHandlerCatalog entitySpriteHandlerCatalog(EntitySpriteHandlerCatalog value) {
            this.entitySpriteHandlerCatalog = value;
            return this.entitySpriteHandlerCatalog;
        }

        FollowingNpcEntitySpawner followingNpcEntitySpawner() {
            return this.followingNpcEntitySpawner;
        }

        FollowingNpcEntitySpawner followingNpcEntitySpawner(FollowingNpcEntitySpawner value) {
            this.followingNpcEntitySpawner = value;
            return this.followingNpcEntitySpawner;
        }

        RomEnemyCombatTables enemyCombatTables() {
            return this.enemyCombatTables;
        }

        RomEnemyCombatTables enemyCombatTables(RomEnemyCombatTables value) {
            this.enemyCombatTables = value;
            return this.enemyCombatTables;
        }

        ChestContentsTable chestContentsTable() {
            return this.chestContentsTable;
        }

        ChestContentsTable chestContentsTable(ChestContentsTable value) {
            this.chestContentsTable = value;
            return this.chestContentsTable;
        }

        DungeonRoomEventTable dungeonRoomEventTable() {
            return this.dungeonRoomEventTable;
        }

        DungeonRoomEventTable dungeonRoomEventTable(DungeonRoomEventTable value) {
            this.dungeonRoomEventTable = value;
            return this.dungeonRoomEventTable;
        }

        RomTables romTables() {
            return this.romTables;
        }

        RomTables romTables(RomTables value) {
            this.romTables = value;
            return this.romTables;
        }

        EntityBackgroundCollisionResolver entityBackgroundCollisionResolver() {
            return this.entityBackgroundCollisionResolver;
        }

        EntityBackgroundCollisionResolver entityBackgroundCollisionResolver(EntityBackgroundCollisionResolver value) {
            this.entityBackgroundCollisionResolver = value;
            return this.entityBackgroundCollisionResolver;
        }

        EnemyDropResolver enemyDropResolver() {
            return this.enemyDropResolver;
        }

        EnemyDropResolver enemyDropResolver(EnemyDropResolver value) {
            this.enemyDropResolver = value;
            return this.enemyDropResolver;
        }

        EntityCollisionPointProbe entityCollisionPointProbe() {
            return this.entityCollisionPointProbe;
        }

        EntityCollisionPointProbe entityCollisionPointProbe(EntityCollisionPointProbe value) {
            this.entityCollisionPointProbe = value;
            return this.entityCollisionPointProbe;
        }

        OverworldBushInteraction overworldBushInteraction() {
            return this.overworldBushInteraction;
        }

        OverworldBushInteraction overworldBushInteraction(OverworldBushInteraction value) {
            this.overworldBushInteraction = value;
            return this.overworldBushInteraction;
        }

        RoomSessionViews roomSessionViews() {
            return this.roomSessionViews;
        }

        RoomSessionViews roomSessionViews(RoomSessionViews value) {
            this.roomSessionViews = value;
            return this.roomSessionViews;
        }

        RoomEntitySessionState entityFrame() {
            return this.entityFrame;
        }

        RoomLocationState location() {
            return this.location;
        }

        RoomTransientState roomTransientState() {
            return this.roomTransientState;
        }

        RoomTransientState roomTransientState(RoomTransientState value) {
            this.roomTransientState = value;
            return this.roomTransientState;
        }

        RoomItemUseState items() {
            return this.items;
        }

        RoomCollisionQueries roomCollisionQueries() {
            return this.roomCollisionQueries;
        }

        RoomCollisionQueries roomCollisionQueries(RoomCollisionQueries value) {
            this.roomCollisionQueries = value;
            return this.roomCollisionQueries;
        }

        RoomTerrainState terrain() {
            return this.terrain;
        }

        RoomObjectMutations roomObjectMutations() {
            return this.roomObjectMutations;
        }

        RoomObjectMutations roomObjectMutations(RoomObjectMutations value) {
            this.roomObjectMutations = value;
            return this.roomObjectMutations;
        }

        RoomPuzzleState puzzles() {
            return this.puzzles;
        }

        RoomEntityInputs entityInputs() {
            return this.entityInputs;
        }

        RoomEntityInputs entityInputs(RoomEntityInputs value) {
            this.entityInputs = value;
            return this.entityInputs;
        }

        RoomRuntime.Services roomRuntimeServices() {
            return this.roomRuntimeServices;
        }

        RoomRuntime.Services roomRuntimeServices(RoomRuntime.Services value) {
            this.roomRuntimeServices = value;
            return this.roomRuntimeServices;
        }

        RoomFollowerState followers() {
            return this.followers;
        }

        RoomPlayerItemActions roomPlayerItemActions() {
            return this.roomPlayerItemActions;
        }

        RoomPlayerItemActions roomPlayerItemActions(RoomPlayerItemActions value) {
            this.roomPlayerItemActions = value;
            return this.roomPlayerItemActions;
        }

        RoomPuzzleController roomPuzzleController() {
            return this.roomPuzzleController;
        }

        RoomPuzzleController roomPuzzleController(RoomPuzzleController value) {
            this.roomPuzzleController = value;
            return this.roomPuzzleController;
        }

        RoomStoryState roomStoryState() {
            return this.roomStoryState;
        }

        RoomStoryState roomStoryState(RoomStoryState value) {
            this.roomStoryState = value;
            return this.roomStoryState;
        }

        RoomDoorInteractions roomDoorInteractions() {
            return this.roomDoorInteractions;
        }

        RoomDoorInteractions roomDoorInteractions(RoomDoorInteractions value) {
            this.roomDoorInteractions = value;
            return this.roomDoorInteractions;
        }

        RoomLoadCoordinator roomLoadCoordinator() {
            return this.roomLoadCoordinator;
        }

        RoomLoadCoordinator roomLoadCoordinator(RoomLoadCoordinator value) {
            this.roomLoadCoordinator = value;
            return this.roomLoadCoordinator;
        }

        RoomEntityRewards roomEntityRewards() {
            return this.roomEntityRewards;
        }

        RoomEntityRewards roomEntityRewards(RoomEntityRewards value) {
            this.roomEntityRewards = value;
            return this.roomEntityRewards;
        }

        RoomTransitionController roomTransitionController() {
            return this.roomTransitionController;
        }

        RoomTransitionController roomTransitionController(RoomTransitionController value) {
            this.roomTransitionController = value;
            return this.roomTransitionController;
        }

        RoomEntitySimulation.EffectsDependencies entityEffects() {
            return this.entityEffects;
        }

        RoomEntitySimulation.EffectsDependencies entityEffects(RoomEntitySimulation.EffectsDependencies value) {
            this.entityEffects = value;
            return this.entityEffects;
        }

        RoomEntitySimulation.SetupDependencies entityConfiguration() {
            return this.entityConfiguration;
        }

        RoomEntitySimulation.SetupDependencies entityConfiguration(RoomEntitySimulation.SetupDependencies value) {
            this.entityConfiguration = value;
            return this.entityConfiguration;
        }

        RoomEntityBridge entityBridge() {
            return this.entityBridge;
        }

        RoomEntityBridge entityBridge(RoomEntityBridge value) {
            this.entityBridge = value;
            return this.entityBridge;
        }
    }
}
