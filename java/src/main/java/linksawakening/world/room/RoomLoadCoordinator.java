package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.EVENT_CLEAR_MIDBOSS;
import static linksawakening.world.room.RoomSessionRules.EVENT_EFFECT_MASK;
import static linksawakening.world.room.RoomSessionRules.EVENT_EFFECT_OPEN_LOCKED_DOORS;
import static linksawakening.world.room.RoomSessionRules.ROOM_STATUS_EVENT_1;
import static linksawakening.world.room.RoomSessionRules.ROOM_STATUS_EVENT_2;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.world.entity.ColorShellWorld;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RomEnemyCombatTables;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.object.SwitchBlockAnimation;
import linksawakening.world.transition.Warp;

/** LoadRoom: coordinates ROM room acquisition, initialization and entity replacement. */
final class RoomLoadCoordinator {
    private final Dependencies dependencies;

    boolean musicSelectionIsScroll;

    RoomLoadCoordinator(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    void loadInitialOverworld(int roomId) {
        dependencies.gpu().loadBaseOverworldTiles(dependencies.romData());
        this.loadRoomSpecificTilesIfNeeded(roomId);
        this.loadOverworld(roomId);
    }

    void loadOverworld(int roomId) {
        loadOverworld(roomId, false);
    }

    void loadOverworld(int roomId, boolean scrolling) {
        musicSelectionIsScroll = scrolling;
        dependencies.location().roomLoadSerial++;
        dependencies.location().indoorMapPosition = -1;
        dependencies.roomTransientState().clearTransientRoomState();
        dependencies.indoorTorchPaletteEffect().clear();
        dependencies.entityFrame().recentRoomEntityClears.visit(roomId);
        LoadedRoom room = dependencies
                .roomLoader()
                .loadOverworld(
                        roomId,
                        dependencies.entityFrame().recentRoomEntityClears.mask(roomId),
                        dependencies.roomState().overworldRoomStatus);
        dependencies.gpu().loadAnimatedTilesGroup(dependencies.romData(), room.animatedTilesGroup());
        this.setActiveRoom(room);
        dependencies.roomStoryState().updateWitchEnvironment();
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        dependencies
                .overworldCollision()
                .setGbcOverlay(dependencies.location().activeRoom.gbcOverlay());
        dependencies.overworldCollision().setPhysicsTable(RomTables.PHYSICS_TABLE_OVERWORLD);
        this.synchronizeSwitchBlockCollisionState();
    }

    void loadIndoor(int mapId, int roomId) {
        this.loadIndoor(mapId, roomId, Warp.CATEGORY_INDOOR);
    }

    void loadIndoorFromSavedPosition(int mapId, int roomId, int savedIndoorMapPosition) {
        this.loadIndoor(mapId, roomId, Warp.CATEGORY_INDOOR);
        dependencies.location().indoorMapPosition = savedIndoorMapPosition & 0xFF;
    }

    void loadIndoor(int mapId, int roomId, int mapCategory) {
        this.loadIndoor(mapId, roomId, mapCategory, 4);
    }

    void loadIndoor(int mapId, int roomId, int mapCategory, int transitionDirection) {
        musicSelectionIsScroll = transitionDirection < 4;
        dependencies.location().roomLoadSerial++;
        dependencies.location().indoorMapPosition =
                dependencies.roomSessionViews().findIndoorMapPosition(mapId, roomId);
        dependencies.roomTransientState().clearTransientRoomState();
        dependencies.entityFrame().recentRoomEntityClears.visit(roomId);
        dependencies.roomState().dungeonItemState.loadForMap(mapId, true);
        // LoadRoom $00:3162: side-view passages do not mark the map visited.
        if (mapCategory != Warp.CATEGORY_SIDESCROLL)
            dependencies.roomSessionViews().indoorStatusTableForMap(mapId)[roomId] |= (byte) 0x80;
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
                        dependencies.roomSessionViews().indoorStatusTableForMap(mapId),
                        dependencies.followers().hasBirdKey,
                        new RoomNpcPaletteInputs(
                                dependencies.followers().tarinFlag, dependencies.followers().playerShieldLevel)));
        dependencies.gpu().loadAnimatedTilesGroup(dependencies.romData(), room.animatedTilesGroup());
        var oldSelection = dependencies.location().activeRoom == null
                        || dependencies.location().activeRoom.entityView() == null
                ? null
                : dependencies.location().activeRoom.entityView().spriteSelection();
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
        this.setActiveRoom(room, palettes.objects());
        for (int row = 0; row < 8; row++)
            System.arraycopy(
                    palettes.background()[row],
                    0,
                    dependencies.location().activeRoom.palettes()[row],
                    0,
                    4);
        this.initializeSwitchBlockTiles();
        dependencies
                .indoorTorchPaletteEffect()
                .reset(
                        dependencies.location().activeRoom.palettes(),
                        dependencies.roomObjectMutations().countUnlitTorches());
        dependencies.roomObjectMutations().copyIndoorTorchPalettesToActiveRoom();
        dependencies.roomStoryState().updateWitchEnvironment();
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(null);
        dependencies.overworldCollision().setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
        this.synchronizeSwitchBlockCollisionState();
    }

    void setActiveRoom(LoadedRoom room) {
        this.setActiveRoom(room, null);
    }

    void setActiveRoom(LoadedRoom room, int[][] objectPalettes) {
        dependencies.entityInputs().secretSeashellScreenShakeActive = false;
        dependencies.entityInputs().secretSeashellPegasusCollisionActive = false;
        dependencies.entityInputs().secretSeashellPegasusCollisionX = 0;
        dependencies.entityInputs().secretSeashellPegasusCollisionY = 0;
        RoomEntitySnapshot entities = prepareEntities(room, objectPalettes);
        dependencies.location().activeRoom = ActiveRoom.from(room, entities);
        configureDoorAndEventState();
        dependencies.entityRuntimeState().runtime = entities == null
                ? null
                : RoomEntityRuntime.builder(entities)
                        .indoorRoom(dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD)
                        .randomByteSupplier(dependencies.entityRandomByteSource())
                        .spriteHandlers(dependencies.entitySpriteHandlerCatalog())
                        .enemyCombatTables(dependencies.enemyCombatTables())
                        .chestContentsTable(dependencies.chestContentsTable())
                        .romTables(dependencies.romTables())
                        .rom(dependencies.romData())
                        .build();
        if (dependencies.entityRuntimeState().runtime != null) {
            configureRuntimeWorld();
            configureRuntimeInputs();
            configureRuntimeProgress();
        }
        dependencies.followers().followingNpcRoomNeedsSync = true;
        dependencies.roomStoryState().synchronizeFollowingNpcEntitiesIfNeeded();
        if (dependencies.roomLoadListener() != null) {
            dependencies.roomLoadListener().roomLoaded(dependencies.location().activeRoom);
        }
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
        dependencies
                .entityFrame()
                .configureEnemyDropRuntime(dependencies.enemyDropResolver(), dependencies.entityInputs());
    }

    boolean roomHasBossBattle() {
        return dependencies.entityRuntimeState().runtime != null
                && dependencies.entityRuntimeState().runtime.inspection().inBossBattle();
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
            RoomEntityFrameState entityRuntimeState,
            RoomEntityInputs entityInputs,
            RomRandomByteSource entityRandomByteSource,
            EntitySpriteHandlerCatalog entitySpriteHandlerCatalog,
            GPU gpu,
            IndoorTorchPaletteEffect indoorTorchPaletteEffect,
            OverworldCollision overworldCollision,
            OverworldTilesetTable overworldTilesetTable,
            OwlEventDialogResolver owlEventDialogResolver,
            Rom romData,
            RomTables romTables,
            RoomCollisionQueries roomCollisionQueries,
            RoomLoadListener roomLoadListener,
            RoomLoader roomLoader,
            RoomObjectMutations roomObjectMutations,
            RoomPaletteLoader roomPaletteLoader,
            RoomPuzzleController roomPuzzleController,
            RoomSessionViews roomSessionViews,
            RoomWorldState roomState,
            RoomStoryState roomStoryState,
            RoomTransientState roomTransientState) {
        Dependencies {
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(puzzles, "puzzles");
            java.util.Objects.requireNonNull(terrain, "terrain");
            java.util.Objects.requireNonNull(chestContentsTable, "chestContentsTable");
            java.util.Objects.requireNonNull(colorShellWorld, "colorShellWorld");
            java.util.Objects.requireNonNull(dungeonRoomEventTable, "dungeonRoomEventTable");
            java.util.Objects.requireNonNull(enemyCombatTables, "enemyCombatTables");
            java.util.Objects.requireNonNull(enemyDropResolver, "enemyDropResolver");
            java.util.Objects.requireNonNull(entityBackgroundInteraction, "entityBackgroundInteraction");
            java.util.Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
            java.util.Objects.requireNonNull(entityInputs, "entityInputs");
            java.util.Objects.requireNonNull(entityRandomByteSource, "entityRandomByteSource");
            java.util.Objects.requireNonNull(entitySpriteHandlerCatalog, "entitySpriteHandlerCatalog");
            java.util.Objects.requireNonNull(gpu, "gpu");
            java.util.Objects.requireNonNull(indoorTorchPaletteEffect, "indoorTorchPaletteEffect");
            java.util.Objects.requireNonNull(overworldCollision, "overworldCollision");
            java.util.Objects.requireNonNull(overworldTilesetTable, "overworldTilesetTable");
            java.util.Objects.requireNonNull(owlEventDialogResolver, "owlEventDialogResolver");
            java.util.Objects.requireNonNull(romData, "romData");
            java.util.Objects.requireNonNull(romTables, "romTables");
            java.util.Objects.requireNonNull(roomCollisionQueries, "roomCollisionQueries");
            java.util.Objects.requireNonNull(roomLoader, "roomLoader");
            java.util.Objects.requireNonNull(roomObjectMutations, "roomObjectMutations");
            java.util.Objects.requireNonNull(roomPaletteLoader, "roomPaletteLoader");
            java.util.Objects.requireNonNull(roomPuzzleController, "roomPuzzleController");
            java.util.Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            java.util.Objects.requireNonNull(roomState, "roomState");
            java.util.Objects.requireNonNull(roomStoryState, "roomStoryState");
            java.util.Objects.requireNonNull(roomTransientState, "roomTransientState");
        }
    }

    private RoomEntitySnapshot prepareEntities(LoadedRoom room, int[][] objectPalettes) {
        RoomEntitySnapshot entities = room.entities();
        if (objectPalettes != null && entities != null && entities.spriteSelection() != null) {
            entities = entities.withSpriteSelection(entities.spriteSelection().withObjectPalettes(objectPalettes));
        }
        if (entities != null && entities.spriteSelection() != null) {
            var selection = entities.spriteSelection();
            if (selection.hasStandardSheets()) {
                dependencies.gpu().loadEntitySpriteSheets(dependencies.romData(), selection.sheetValues());
                entities = entities.withSpriteTiles(dependencies.gpu().snapshotEntityTiles());
            } else if (selection.roomTable() == EntityRoomLoader.RoomTable.COLOR_DUNGEON) {
                dependencies.gpu().loadColorDungeonEntitySheets(dependencies.romData(), selection.roomId());
                entities = entities.withSpriteTiles(dependencies.gpu().snapshotEntityTiles());
            }
        }
        if (entities != null) {
            entities = entities.withSideScrolling(room.mapCategory() == Warp.CATEGORY_SIDESCROLL);
        }
        return entities;
    }

    private void configureDoorAndEventState() {
        if (dependencies.location().activeRoom.shutterDoorMask() != 0
                && dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            boolean ordinaryEventPending = dependencies.puzzles().activeRoomEvent != 0
                    && ((dependencies.puzzles().activeRoomEvent & EVENT_EFFECT_MASK) == EVENT_EFFECT_OPEN_LOCKED_DOORS
                            || (dependencies.roomSessionViews().activeRoomStatusFlags() & ROOM_STATUS_EVENT_1) == 0);
            if (ordinaryEventPending) {
                // $02:5E7B keeps entry doors open until Link is inside the room.
                dependencies.puzzles().pendingShutterCloseMask =
                        dependencies.location().activeRoom.shutterDoorMask();
            } else {
                dependencies.roomPuzzleController().openActiveRoomShutterDoors();
            }
        }
        dependencies
                .puzzles()
                .configureStaircase(dependencies.location().activeRoom.staircaseLocation());
        boolean minibossDefeated = dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
                && dependencies.location().activeRoom.mapId() >= 0
                && dependencies.location().activeRoom.mapId() < dependencies.roomState().dungeonProgressFlags.length
                && ((dependencies.roomState()
                                                .dungeonProgressFlags[
                                                dependencies
                                                        .location()
                                                        .activeRoom
                                                        .mapId()]
                                        & 0x01)
                                != 0
                        || (dependencies.roomSessionViews().activeRoomStatusFlags() & ROOM_STATUS_EVENT_2) != 0);
        if (minibossDefeated && dependencies.puzzles().activeRoomEvent == EVENT_CLEAR_MIDBOSS) {
            dependencies.roomState()
                    .dungeonProgressFlags[dependencies.location().activeRoom.mapId()] |= 0x01;
            dependencies.roomPuzzleController().openActiveRoomShutterDoors();
            dependencies.puzzles().activeRoomEvent = 0;
        }
    }

    private void configureRuntimeWorld() {
        var configuration = dependencies.entityRuntimeState().runtime.configuration();

        configuration.setBooBuddyTriggerCount(dependencies.puzzles().roomTriggerCount);
        configuration.setColorShellWorld(dependencies.colorShellWorld());
        configuration.setFollowingNpcState(dependencies.followers().followingNpcState);
        configuration.setEntityMapId(dependencies.location().activeRoom.mapId());
        configuration.setEntityRoomId(dependencies.location().activeRoom.roomId());
        configuration.setOwlDialogResolver(dependencies.owlEventDialogResolver()::globalDialogId);
        configuration.setOwlDefaultMusicResolver(dependencies.owlEventDialogResolver()::defaultMusicTrack);
        configuration.setGroundInteraction(dependencies.roomCollisionQueries()::entityGroundInteraction);
        configuration.setBackgroundInteraction(dependencies.entityBackgroundInteraction());
    }

    private void configureRuntimeInputs() {
        var configuration = dependencies.entityRuntimeState().runtime.configuration();

        configuration.setObjectQuery(dependencies.roomCollisionQueries()::entityObjectSample);
        configuration.setObjectIntersectionQuery(dependencies.roomCollisionQueries()::entityObjectIntersectionSample);
        configuration.setGroundInteractionSideScrolling(
                dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL);
        configuration.setActionButtonsHeld(
                dependencies.entityInputs().actionButtonAHeld, dependencies.entityInputs().actionButtonBHeld);
        configuration.setTalkState(
                dependencies.entityInputs().entityInventoryAppearing,
                dependencies.entityInputs().entityDialogCooldown,
                dependencies.entityInputs().entityWindowY);
        configuration.setJoypadHeld(dependencies.entityInputs().joypadHeld);
        configuration.setPressedButtonsMask(dependencies.entityInputs().entityPressedButtonsMask);
        configuration.setPowerBraceletButtonHeld(dependencies.entityInputs().powerBraceletButtonHeld);
        configuration.setBombButtonHeld(dependencies.entityInputs().bombButtonHeld);
    }

    private void configureRuntimeProgress() {
        var configuration = dependencies.entityRuntimeState().runtime.configuration();

        configuration.setLiftedLinkC13B(dependencies.followers().followingEntityYOffset);
        configuration.setEntityRoomStatus(dependencies.roomSessionViews().activeRoomStatusFlags());
        configuration.setDungeonMinibossReady(dependencies.location().activeRoom.mapId() >= 0
                && dependencies.location().activeRoom.mapId() < dependencies.roomState().dungeonProgressFlags.length
                && (dependencies.roomState()
                                        .dungeonProgressFlags[
                                        dependencies.location().activeRoom.mapId()]
                                & 0x01)
                        != 0);
        configuration.setTailKeyOwned(dependencies.followers().hasTailKey);
        configuration.setOwlInstrumentFlags(
                Byte.toUnsignedInt(dependencies.roomState().dungeonProgressFlags[0]),
                Byte.toUnsignedInt(dependencies.roomState().dungeonProgressFlags[1]));
        configuration.setSecretSeashellPegasusCollisionState(
                dependencies.entityInputs().secretSeashellScreenShakeActive,
                        dependencies.entityInputs().secretSeashellPegasusCollisionActive,
                dependencies.entityInputs().secretSeashellPegasusCollisionX,
                        dependencies.entityInputs().secretSeashellPegasusCollisionY);
        this.configureEnemyDropRuntime();
    }
}
