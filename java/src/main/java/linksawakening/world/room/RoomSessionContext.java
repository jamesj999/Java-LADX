package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.ColorShellWorld;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.EntityBackgroundCollisionResolver;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityCollisionPointProbe;
import linksawakening.world.entity.FollowingNpcEntitySpawner;
import linksawakening.world.entity.RomEnemyCombatTables;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.object.OverworldBushInteraction;

/** Private dependency wiring; runtime collaborators receive only their own dependencies. */
final class RoomSessionContext {
    private byte[] romData;
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
    private RoomLoadListener roomLoadListener;
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
    private RoomEntityBackgroundInteraction entityBackgroundInteraction;
    private RoomWorldState roomState;
    private RoomScrollNavigation scrollNavigation;
    private ColorShellWorld colorShellWorld;
    private RoomSessionViews roomSessionViews;
    private RoomTransientState roomTransientState;
    private RoomEntityRewards roomEntityRewards;
    private RoomTileRedraw roomTileRedraw;
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
    private RoomEntityAfterTick entityAfterTick;
    private RoomEntityFrameSetup entityFrameSetup;
    private RoomEntityCombatBridge entityCombatBridge;
    private final RoomLocationState location = new RoomLocationState();
    private final RoomTerrainState terrain = new RoomTerrainState();
    private final RoomFollowerState followers = new RoomFollowerState();
    private final RoomEntityFrameState entityFrame = new RoomEntityFrameState();
    private final RoomItemUseState items = new RoomItemUseState();
    private final RoomPuzzleState puzzles = new RoomPuzzleState();

    static RoomSessionComponents create(RoomSessionConfig config) {
        return new RoomSessionContext(config).components();
    }

    private RoomSessionContext(RoomSessionConfig config) {
        var romData = config.romData();
        var gpu = config.gpu();
        var roomLoader = config.roomLoader();
        var overworldTilesetTable = config.overworldTilesetTable();
        var overworldCollision = config.overworldCollision();
        var transientVfxSystem = config.presentation().transientVfxSystem();
        var droppableRupeeSystem = config.presentation().droppableRupeeSystem();
        var roomLoadListener = config.presentation().roomLoadListener();
        var randomByteSource = config.randomByteSource();
        entityRandomByteSource = java.util.Objects.requireNonNull(randomByteSource);
        entityBackgroundInteraction = new RoomEntityBackgroundInteraction() {
            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction, int nextX, int nextY) {
                return roomCollisionQueries.entityBackgroundCollisionResult(entity, direction, nextX, nextY, 0);
            }

            @Override
            public EntityBackgroundCollisionResult probe(
                    RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown) {
                return roomCollisionQueries.entityBackgroundCollisionResult(
                        entity, direction, nextX, nextY, ignoreHitsCountdown);
            }

            @Override
            public EntityBackgroundCollisionResult probe(
                    RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown, int frameCounter) {
                return roomCollisionQueries.entityBackgroundCollisionResult(
                        entity, direction, nextX, nextY, ignoreHitsCountdown, frameCounter);
            }
        };
        colorShellWorld = new ColorShellWorld() {
            @Override
            public int objectAt(RoomEntity entity, int relativeOffset) {
                return roomObjectMutations.colorShellObjectAt(entity, relativeOffset);
            }

            @Override
            public void writeObject(RoomEntity entity, int objectId) {
                roomObjectMutations.writeColorShellObject(entity, objectId);
            }

            @Override
            public void playJingle(int id) {
                if ((id & 0xFF) == 0x1D) {
                    entityFrame.colorShellSoundSink.play(GameplaySoundEvent.WRONG_ANSWER);
                }
            }

            @Override
            public void playNoise(int id) {
                if ((id & 0xFF) == 0x04) {
                    entityFrame.colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
                }
            }

            @Override
            public void spawnPoof(int x, int y) {
                if (transientVfxSystem != null) {
                    transientVfxSystem.spawn(TransientVfxType.POOF, x, y);
                }
            }
        };
        this.romData = romData;
        this.roomState = new RoomWorldState(romData);
        this.scrollNavigation = new RoomScrollNavigation(romData, roomState);
        this.indoorTorchPaletteEffect = new IndoorTorchPaletteEffect(romData);
        this.gpu = gpu;
        this.roomLoader = roomLoader;
        this.roomPaletteLoader = new RoomPaletteLoader(romData);
        this.overworldTilesetTable = overworldTilesetTable;
        this.manboWarpResolver = new ManboWarpResolver(romData);
        this.owlEventDialogResolver = new OwlEventDialogResolver(romData);
        this.owlStatueDialogResolver = new OwlStatueDialogResolver(romData);
        this.overworldCollision = overworldCollision;
        this.transientVfxSystem = transientVfxSystem;
        this.droppableRupeeSystem = droppableRupeeSystem;
        this.roomLoadListener = roomLoadListener;
        this.roomTilemapBuilder = new RoomTilemapBuilder(romData);
        this.entitySpriteHandlerCatalog = new EntitySpriteHandlerCatalog(romData);
        this.followingNpcEntitySpawner = new FollowingNpcEntitySpawner(entitySpriteHandlerCatalog);
        this.enemyCombatTables = new RomEnemyCombatTables(romData);
        this.chestContentsTable = new ChestContentsTable(romData);
        this.dungeonRoomEventTable = new DungeonRoomEventTable(romData);
        this.romTables = RomTables.loadFromRom(romData);
        this.entityBackgroundCollisionResolver = new EntityBackgroundCollisionResolver(romTables);
        this.enemyDropResolver = new EnemyDropResolver(romData);
        this.entityCollisionPointProbe = new EntityCollisionPointProbe(romTables);
        this.overworldBushInteraction = new OverworldBushInteraction(romData, romTables);
        entityAfterTick = new RoomEntityAfterTick(new RoomEntityAfterTick.Dependencies(
                entityFrame,
                followers,
                items,
                location,
                terrain,
                () -> entityBridge,
                () -> roomEntityRewards,
                () -> roomObjectMutations,
                () -> roomPuzzleController,
                () -> roomSessionViews,
                roomState,
                () -> roomTileRedraw,
                () -> roomTransitionController,
                transientVfxSystem));
        entityFrameSetup = new RoomEntityFrameSetup(new RoomEntityFrameSetup.Dependencies(
                entityFrame,
                followers,
                items,
                location,
                puzzles,
                terrain,
                () -> entityBridge,
                () -> entityInputs,
                indoorTorchPaletteEffect,
                owlStatueDialogResolver,
                () -> roomObjectMutations,
                roomState,
                () -> roomStoryState,
                () -> roomTransitionController));
        entityCombatBridge = new RoomEntityCombatBridge(new RoomEntityCombatBridge.Dependencies(
                entityFrame,
                followers,
                location,
                () -> entityBridge,
                () -> roomEntityRewards,
                roomState,
                () -> roomStoryState));
        roomSessionViews = new RoomSessionViews(new RoomSessionViews.Dependencies(
                entityFrame,
                location,
                puzzles,
                terrain,
                () -> entityBridge,
                indoorTorchPaletteEffect,
                overworldCollision,
                () -> roomLoadCoordinator,
                roomState,
                () -> roomStoryState,
                scrollNavigation));
        roomTransientState = new RoomTransientState(new RoomTransientState.Dependencies(
                entityFrame,
                items,
                location,
                puzzles,
                terrain,
                droppableRupeeSystem,
                () -> entityBridge,
                transientVfxSystem));
        roomEntityRewards = new RoomEntityRewards(new RoomEntityRewards.Dependencies(
                followers,
                items,
                location,
                () -> entityBridge,
                () -> entityInputs,
                () -> roomSessionViews,
                roomState,
                () -> roomStoryState));
        roomTileRedraw = new RoomTileRedraw(new RoomTileRedraw.Dependencies(
                location,
                puzzles,
                terrain,
                romData,
                () -> roomObjectMutations,
                () -> roomTileRedraw,
                roomTilemapBuilder));
        roomCollisionQueries = new RoomCollisionQueries(new RoomCollisionQueries.Dependencies(
                entityFrame,
                location,
                puzzles,
                terrain,
                entityBackgroundCollisionResolver,
                () -> entityBridge,
                entityCollisionPointProbe,
                overworldCollision,
                romTables,
                () -> roomCollisionQueries));
        roomPlayerItemActions = new RoomPlayerItemActions(new RoomPlayerItemActions.Dependencies(
                followers,
                items,
                location,
                () -> entityBridge,
                overworldBushInteraction,
                overworldCollision,
                romTables,
                () -> roomObjectMutations,
                () -> roomPlayerItemActions,
                () -> roomSessionViews,
                () -> roomTileRedraw));
        roomPuzzleController = new RoomPuzzleController(new RoomPuzzleController.Dependencies(
                entityFrame,
                location,
                puzzles,
                () -> entityBridge,
                () -> entityInputs,
                gpu,
                overworldCollision,
                romData,
                romTables,
                () -> roomObjectMutations,
                () -> roomPuzzleController,
                () -> roomSessionViews,
                roomState,
                () -> roomStoryState,
                () -> roomTileRedraw,
                () -> roomTransitionController,
                transientVfxSystem));
        roomStoryState = new RoomStoryState(new RoomStoryState.Dependencies(
                entityFrame,
                followers,
                location,
                puzzles,
                terrain,
                chestContentsTable,
                colorShellWorld,
                enemyCombatTables,
                entityBackgroundInteraction,
                () -> entityBridge,
                () -> entityInputs,
                entityRandomByteSource,
                entitySpriteHandlerCatalog,
                followingNpcEntitySpawner,
                indoorTorchPaletteEffect,
                owlEventDialogResolver,
                romData,
                romTables,
                () -> roomCollisionQueries,
                () -> roomLoadCoordinator,
                () -> roomSessionViews,
                roomState,
                () -> roomStoryState,
                () -> roomTransientState));
        roomDoorInteractions = new RoomDoorInteractions(new RoomDoorInteractions.Dependencies(
                entityFrame,
                followers,
                location,
                puzzles,
                chestContentsTable,
                () -> entityBridge,
                overworldBushInteraction,
                overworldCollision,
                romData,
                () -> roomDoorInteractions,
                () -> roomObjectMutations,
                () -> roomSessionViews,
                roomState,
                () -> roomTileRedraw));
        roomObjectMutations = new RoomObjectMutations(new RoomObjectMutations.Dependencies(
                entityFrame,
                location,
                puzzles,
                terrain,
                () -> entityBridge,
                indoorTorchPaletteEffect,
                overworldBushInteraction,
                overworldCollision,
                romTables,
                () -> roomObjectMutations,
                () -> roomPlayerItemActions,
                () -> roomSessionViews,
                roomState,
                () -> roomTileRedraw,
                transientVfxSystem));
        roomTransitionController = new RoomTransitionController(new RoomTransitionController.Dependencies(
                entityFrame,
                followers,
                items,
                location,
                puzzles,
                () -> entityBridge,
                gpu,
                manboWarpResolver,
                romData,
                () -> roomLoadCoordinator,
                () -> roomSessionViews,
                roomState,
                () -> roomTransitionController,
                scrollNavigation));
        roomLoadCoordinator = new RoomLoadCoordinator(new RoomLoadCoordinator.Dependencies(
                entityFrame,
                followers,
                location,
                puzzles,
                terrain,
                chestContentsTable,
                colorShellWorld,
                dungeonRoomEventTable,
                enemyCombatTables,
                enemyDropResolver,
                entityBackgroundInteraction,
                () -> entityBridge,
                () -> entityInputs,
                entityRandomByteSource,
                entitySpriteHandlerCatalog,
                gpu,
                indoorTorchPaletteEffect,
                overworldCollision,
                overworldTilesetTable,
                owlEventDialogResolver,
                romData,
                romTables,
                () -> roomCollisionQueries,
                () -> roomLoadCoordinator,
                roomLoadListener,
                roomLoader,
                () -> roomObjectMutations,
                roomPaletteLoader,
                () -> roomPuzzleController,
                () -> roomSessionViews,
                roomState,
                () -> roomStoryState,
                () -> roomTransientState,
                () -> roomTransitionController));
        entityBridge = new RoomEntityBridge(new RoomEntityBridge.Dependencies(
                entityFrame,
                followers,
                items,
                location,
                terrain,
                () -> entityAfterTick,
                () -> entityBridge,
                () -> entityCombatBridge,
                () -> entityFrameSetup,
                () -> entityInputs,
                entityRandomByteSource,
                gpu,
                romData,
                () -> roomCollisionQueries,
                () -> roomLoadCoordinator));
        entityInputs = new RoomEntityInputs(() -> entityBridge.entityRuntime);
    }

    private RoomSessionComponents components() {
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
                roomLoadListener,
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
                entityBackgroundInteraction,
                roomState,
                scrollNavigation,
                colorShellWorld,
                roomSessionViews,
                roomTransientState,
                roomEntityRewards,
                roomTileRedraw,
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
                entityAfterTick,
                entityFrameSetup,
                entityCombatBridge,
                new LinkOutputs.Dependencies(() -> entityBridge),
                new RewardOutputs.Dependencies(() -> entityBridge, () -> roomSessionViews),
                new PresentationOutputs.Dependencies(() -> entityBridge));
    }
}
