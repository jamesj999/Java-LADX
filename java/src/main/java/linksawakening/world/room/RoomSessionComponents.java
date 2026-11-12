package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.entity.ColorShellWorld;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.EntityBackgroundCollisionResolver;
import linksawakening.world.entity.EntityCollisionPointProbe;
import linksawakening.world.entity.FollowingNpcEntitySpawner;
import linksawakening.world.entity.RomEnemyCombatTables;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.object.OverworldBushInteraction;

/** Wiring result consumed only by the facade and compatibility adapters. */
record RoomSessionComponents(
        RoomLocationState location,
        RoomTerrainState terrain,
        RoomFollowerState followers,
        RoomEntityFrameState entityFrame,
        RoomItemUseState items,
        RoomPuzzleState puzzles,
        byte[] romData,
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
        RoomEntityBackgroundInteraction entityBackgroundInteraction,
        RoomWorldState roomState,
        RoomScrollNavigation scrollNavigation,
        ColorShellWorld colorShellWorld,
        RoomSessionViews roomSessionViews,
        RoomTransientState roomTransientState,
        RoomEntityRewards roomEntityRewards,
        RoomTileRedraw roomTileRedraw,
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
        RoomEntityAfterTick entityAfterTick,
        RoomEntityFrameSetup entityFrameSetup,
        RoomEntityCombatBridge entityCombatBridge,
        LinkOutputs.Dependencies linkOutputsDependencies,
        RewardOutputs.Dependencies rewardOutputsDependencies,
        PresentationOutputs.Dependencies presentationOutputsDependencies) {
    static final class Builder {
        byte[] romData;
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
        RoomEntityBackgroundInteraction entityBackgroundInteraction;
        RoomWorldState roomState;
        RoomScrollNavigation scrollNavigation;
        ColorShellWorld colorShellWorld;
        RoomSessionViews roomSessionViews;
        RoomTransientState roomTransientState;
        RoomEntityRewards roomEntityRewards;
        RoomTileRedraw roomTileRedraw;
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
        RoomEntityAfterTick entityAfterTick;
        RoomEntityFrameSetup entityFrameSetup;
        RoomEntityCombatBridge entityCombatBridge;
        final RoomLocationState location = new RoomLocationState();
        final RoomTerrainState terrain = new RoomTerrainState();
        final RoomFollowerState followers = new RoomFollowerState();
        final RoomEntityFrameState entityFrame = new RoomEntityFrameState();
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
}
