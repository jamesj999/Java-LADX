package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.world.entity.ColorShellWorld;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.RomEnemyCombatTables;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.object.SwitchBlockAnimation;
import linksawakening.world.transition.Warp;

/** RoomLoadCoordinator operations for the live room session. */
final class RoomLoadCoordinator {
    private final Dependencies dependencies;
    private final RoomActiveRoomInitializer roomActiveRoomInitializer;

    boolean musicSelectionIsScroll;

    RoomLoadCoordinator(Dependencies dependencies) {
        this.dependencies = dependencies;
        roomActiveRoomInitializer = new RoomActiveRoomInitializer(new RoomActiveRoomInitializer.Dependencies(
                dependencies.chestContentsTable(),
                dependencies.colorShellWorld(),
                dependencies.enemyCombatTables(),
                dependencies.entityBackgroundInteraction(),
                dependencies.entityBridge(),
                dependencies.entityInputs(),
                dependencies.entityRandomByteSource(),
                dependencies.entitySpriteHandlerCatalog(),
                dependencies.followers(),
                dependencies.gpu(),
                dependencies.location(),
                dependencies.owlEventDialogResolver(),
                dependencies.puzzles(),
                dependencies.romData(),
                dependencies.romTables(),
                dependencies.roomCollisionQueries(),
                dependencies.roomLoadCoordinator(),
                dependencies.roomLoadListener(),
                dependencies.roomPuzzleController(),
                dependencies.roomSessionViews(),
                dependencies.roomState(),
                dependencies.roomStoryState(),
                dependencies.roomTransitionController()));
    }

    void loadInitialOverworld(int roomId) {
        dependencies.gpu().loadBaseOverworldTiles(dependencies.romData());
        dependencies.roomLoadCoordinator().get().loadRoomSpecificTilesIfNeeded(roomId);
        dependencies.roomLoadCoordinator().get().loadOverworld(roomId);
    }

    void loadOverworld(int roomId) {
        loadOverworld(roomId, false);
    }

    void loadOverworld(int roomId, boolean scrolling) {
        musicSelectionIsScroll = scrolling;
        dependencies.location().roomLoadSerial++;
        dependencies.location().indoorMapPosition = -1;
        dependencies.roomTransientState().get().clearTransientRoomState();
        dependencies.indoorTorchPaletteEffect().clear();
        dependencies.entityFrame().recentRoomEntityClears.visit(roomId);
        LoadedRoom room = dependencies
                .roomLoader()
                .loadOverworld(
                        roomId,
                        dependencies.entityFrame().recentRoomEntityClears.mask(roomId),
                        dependencies.roomState().overworldRoomStatus);
        dependencies.gpu().loadAnimatedTilesGroup(dependencies.romData(), room.animatedTilesGroup());
        dependencies.roomLoadCoordinator().get().setActiveRoom(room);
        dependencies.roomStoryState().get().updateWitchEnvironment();
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        dependencies
                .overworldCollision()
                .setGbcOverlay(dependencies.location().activeRoom.gbcOverlay());
        dependencies.overworldCollision().setPhysicsTable(RomTables.PHYSICS_TABLE_OVERWORLD);
        dependencies.roomLoadCoordinator().get().synchronizeSwitchBlockCollisionState();
    }

    void loadIndoor(int mapId, int roomId) {
        dependencies.roomLoadCoordinator().get().loadIndoor(mapId, roomId, Warp.CATEGORY_INDOOR);
    }

    void loadIndoorFromSavedPosition(int mapId, int roomId, int savedIndoorMapPosition) {
        dependencies.roomLoadCoordinator().get().loadIndoor(mapId, roomId, Warp.CATEGORY_INDOOR);
        dependencies.location().indoorMapPosition = savedIndoorMapPosition & 0xFF;
    }

    void loadIndoor(int mapId, int roomId, int mapCategory) {
        dependencies.roomLoadCoordinator().get().loadIndoor(mapId, roomId, mapCategory, 4);
    }

    void loadIndoor(int mapId, int roomId, int mapCategory, int transitionDirection) {
        musicSelectionIsScroll = transitionDirection < 4;
        dependencies.location().roomLoadSerial++;
        dependencies.location().indoorMapPosition =
                dependencies.roomSessionViews().get().findIndoorMapPosition(mapId, roomId);
        dependencies.roomTransientState().get().clearTransientRoomState();
        dependencies.entityFrame().recentRoomEntityClears.visit(roomId);
        dependencies.roomState().dungeonItemState.loadForMap(mapId, true);
        // LoadRoom $00:3162: side-view passages do not mark the map visited.
        if (mapCategory != Warp.CATEGORY_SIDESCROLL)
            dependencies.roomSessionViews().get().indoorStatusTableForMap(mapId)[roomId] |= (byte) 0x80;
        dependencies.puzzles().activeRoomEvent =
                dependencies.dungeonRoomEventTable().eventFor(mapId, roomId);
        dependencies
                .gpu()
                .loadIndoorTiles(dependencies.romData(), mapId, roomId, mapCategory == Warp.CATEGORY_SIDESCROLL);
        LoadedRoom room = dependencies
                .roomLoader()
                .loadIndoor(new IndoorRoomLoadInputs(
                        mapId,
                        roomId,
                        dependencies.location().activeRoom == null
                                ? null
                                : dependencies.location().activeRoom.palettes(),
                        mapCategory,
                        dependencies.entityFrame().recentRoomEntityClears.mask(roomId),
                        dependencies.roomSessionViews().get().indoorStatusTableForMap(mapId),
                        dependencies.followers().hasBirdKey,
                        new RoomNpcPaletteInputs(
                                dependencies.followers().tarinFlag, dependencies.followers().playerShieldLevel)));
        dependencies.gpu().loadAnimatedTilesGroup(dependencies.romData(), room.animatedTilesGroup());
        var oldSelection = dependencies.location().activeRoom == null
                        || dependencies.location().activeRoom.entities() == null
                ? null
                : dependencies.location().activeRoom.entities().spriteSelection();
        var palettes = dependencies
                .roomPaletteLoader()
                .indoorTransition(
                        mapId,
                        roomId,
                        transitionDirection,
                        room.palettes(),
                        room.entities().spriteSelection().objectPalettes(),
                        dependencies.location().activeRoom == null
                                ? null
                                : dependencies.location().activeRoom.palettes(),
                        oldSelection == null ? null : oldSelection.objectPalettes());
        dependencies.roomLoadCoordinator().get().setActiveRoom(room, palettes.objects());
        for (int row = 0; row < 8; row++)
            System.arraycopy(
                    palettes.background()[row],
                    0,
                    dependencies.location().activeRoom.palettes()[row],
                    0,
                    4);
        dependencies.roomLoadCoordinator().get().initializeSwitchBlockTiles();
        dependencies
                .indoorTorchPaletteEffect()
                .reset(
                        dependencies.location().activeRoom.palettes(),
                        dependencies.roomObjectMutations().get().countUnlitTorches());
        dependencies.roomObjectMutations().get().copyIndoorTorchPalettesToActiveRoom();
        dependencies.roomStoryState().get().updateWitchEnvironment();
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(null);
        dependencies.overworldCollision().setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
        dependencies.roomLoadCoordinator().get().synchronizeSwitchBlockCollisionState();
    }

    void setActiveRoom(LoadedRoom room) {
        roomActiveRoomInitializer.setActiveRoom(room);
    }

    void setActiveRoom(LoadedRoom room, int[][] objectPalettes) {
        roomActiveRoomInitializer.setActiveRoom(room, objectPalettes);
    }

    void initializeSwitchBlockTiles() {
        // LoadRoom configures wRoomSwitchableObject only for $DB/$DC.
        // These VRAM slots hold furniture in houses and must stay intact there.
        boolean hasBlocks = false;
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int col = 0; col < RoomConstants.OBJECTS_PER_ROW; col++) {
                int object = dependencies.location()
                        .activeRoom
                        .roomObjectsArea()[
                        RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + col];
                hasBlocks |= object == 0xDB || object == 0xDC;
            }
        }
        if (!hasBlocks) return;
        for (SwitchBlockAnimation.TileCopy copy :
                SwitchBlockAnimation.initialCopies(dependencies.terrain().switchBlocksState)) {
            dependencies
                    .gpu()
                    .copySwitchBlockTiles(dependencies.romData(), copy.sourceOffset(), copy.destinationTile());
        }
    }

    void synchronizeSwitchBlockCollisionState() {
        dependencies.overworldCollision().setSwitchBlocksState(dependencies.terrain().switchBlocksState);
    }

    void configureEnemyDropRuntime() {
        if (dependencies.entityBridge().get().entityRuntime == null) {
            return;
        }
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setDialogActive(dependencies.entityInputs().get().entityDialogActive);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setEnemyDropResolver(dependencies.enemyDropResolver());
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setEnemyDropCounters(dependencies.entityFrame().enemyDropCounters);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setEnemyDropPlayerState(
                        dependencies.entityFrame().enemyDropMaxHearts,
                        dependencies.entityFrame().enemyDropHealth,
                        dependencies.entityFrame().enemyDropActivePowerUp);
        if (dependencies.entityFrame().enemyDropLowHealth != null) {
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setEnemyDropPlayerState(
                            dependencies.entityFrame().enemyDropMaxHearts, dependencies.entityFrame().enemyDropHealth,
                            dependencies.entityFrame().enemyDropActivePowerUp,
                                    dependencies.entityFrame().enemyDropLowHealth);
        }
    }

    boolean roomHasBossBattle() {
        return dependencies.entityBridge().get().entityRuntime != null
                && dependencies.entityBridge().get().entityRuntime.inspection().inBossBattle();
    }

    void loadRoomSpecificTilesIfNeeded(int roomId) {
        int tilesetId = dependencies.overworldTilesetTable().tilesetIdForRoom(roomId);
        if (OverworldTilesetTable.shouldLoadRoomSpecificTileset(
                roomId, tilesetId, dependencies.location().currentOverworldTilesetId)) {
            dependencies.gpu().loadRoomSpecificTiles(dependencies.romData(), roomId, tilesetId);
            dependencies.location().currentOverworldTilesetId = tilesetId;
        }
    }

    record Dependencies(
            RoomEntityFrameState entityFrame,
            RoomFollowerState followers,
            RoomLocationState location,
            RoomPuzzleState puzzles,
            RoomTerrainState terrain,
            ChestContentsTable chestContentsTable,
            ColorShellWorld colorShellWorld,
            DungeonRoomEventTable dungeonRoomEventTable,
            RomEnemyCombatTables enemyCombatTables,
            EnemyDropResolver enemyDropResolver,
            RoomEntityBackgroundInteraction entityBackgroundInteraction,
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            java.util.function.Supplier<RoomEntityInputs> entityInputs,
            RomRandomByteSource entityRandomByteSource,
            EntitySpriteHandlerCatalog entitySpriteHandlerCatalog,
            GPU gpu,
            IndoorTorchPaletteEffect indoorTorchPaletteEffect,
            OverworldCollision overworldCollision,
            OverworldTilesetTable overworldTilesetTable,
            OwlEventDialogResolver owlEventDialogResolver,
            byte[] romData,
            RomTables romTables,
            java.util.function.Supplier<RoomCollisionQueries> roomCollisionQueries,
            java.util.function.Supplier<RoomLoadCoordinator> roomLoadCoordinator,
            RoomLoadListener roomLoadListener,
            RoomLoader roomLoader,
            java.util.function.Supplier<RoomObjectMutations> roomObjectMutations,
            RoomPaletteLoader roomPaletteLoader,
            java.util.function.Supplier<RoomPuzzleController> roomPuzzleController,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomStoryState> roomStoryState,
            java.util.function.Supplier<RoomTransientState> roomTransientState,
            java.util.function.Supplier<RoomTransitionController> roomTransitionController) {}
}
