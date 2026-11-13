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

/** Wiring result consumed only by the facade and compatibility adapters. */
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
        RoomLoadListener roomLoadListener,
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
        RoomEntityCombatBridge entityCombatBridge,
        LinkOutputs.Dependencies linkOutputsDependencies,
        RewardOutputs.Dependencies rewardOutputsDependencies,
        PresentationOutputs.Dependencies presentationOutputsDependencies,
        RoomRuntime.Services roomRuntimeServices) {
    static final class Builder {
        RoomRuntime.Services roomRuntimeServices;
        Rom romData;
        GPU gpu;
        RoomLoader roomLoader;
        RoomPaletteLoader roomPaletteLoader;
        OverworldTilesetTable overworldTilesetTable;
        ManboWarpResolver manboWarpResolver;
        OwlEventDialogResolver owlEventDialogResolver;
        OwlStatueDialogResolver owlStatueDialogResolver;
        OverworldCollision overworldCollision;
        TransientVfxSystem transientVfxSystem;
        DroppableRupeeSystem droppableRupeeSystem;
        RoomLoadListener roomLoadListener;
        RoomTilemapBuilder roomTilemapBuilder;
        RomRandomByteSource entityRandomByteSource;
        EntitySpriteHandlerCatalog entitySpriteHandlerCatalog;
        FollowingNpcEntitySpawner followingNpcEntitySpawner;
        RomEnemyCombatTables enemyCombatTables;
        ChestContentsTable chestContentsTable;
        DungeonRoomEventTable dungeonRoomEventTable;
        RomTables romTables;
        EnemyDropResolver enemyDropResolver;
        EntityCollisionPointProbe entityCollisionPointProbe;
        EntityBackgroundCollisionResolver entityBackgroundCollisionResolver;
        OverworldBushInteraction overworldBushInteraction;
        IndoorTorchPaletteEffect indoorTorchPaletteEffect;
        RoomWorldState roomState;
        RoomScrollNavigation scrollNavigation;
        RoomSessionViews roomSessionViews;
        RoomTransientState roomTransientState;
        RoomEntityRewards roomEntityRewards;
        RoomCollisionQueries roomCollisionQueries;
        RoomPlayerItemActions roomPlayerItemActions;
        RoomPuzzleController roomPuzzleController;
        RoomStoryState roomStoryState;
        RoomDoorInteractions roomDoorInteractions;
        RoomObjectMutations roomObjectMutations;
        RoomTransitionController roomTransitionController;
        RoomLoadCoordinator roomLoadCoordinator;
        RoomEntityBridge entityBridge;
        RoomEntityInputs entityInputs;
        RoomEntitySimulation.EffectsDependencies entityEffects;
        RoomEntitySimulation.SetupDependencies entityConfiguration;
        RoomEntityCombatBridge entityCombatBridge;
        final RoomLocationState location = new RoomLocationState();
        final RoomTerrainState terrain = new RoomTerrainState();
        final RoomFollowerState followers = new RoomFollowerState();
        final RoomEntitySessionState entityFrame = new RoomEntitySessionState();
        final RoomItemUseState items = new RoomItemUseState();
        final RoomPuzzleState puzzles = new RoomPuzzleState();

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
                    entityCombatBridge,
                    new LinkOutputs.Dependencies(entityBridge),
                    new RewardOutputs.Dependencies(entityFrame, roomSessionViews),
                    new PresentationOutputs.Dependencies(),
                    roomRuntimeServices);
        }
    }
}
