package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
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

/** RoomLoadCoordinator operations for the live room session. */
final class RoomLoadCoordinator {
    private final Dependencies dependencies;
    boolean musicSelectionIsScroll;

    RoomLoadCoordinator(Dependencies dependencies) {
        this.dependencies = dependencies;
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
                .loadIndoor(
                        mapId,
                        roomId,
                        dependencies.location().activeRoom == null
                                ? null
                                : dependencies.location().activeRoom.palettes(),
                        mapCategory,
                        dependencies.entityFrame().recentRoomEntityClears.mask(roomId),
                        dependencies.roomSessionViews().get().indoorStatusTableForMap(mapId),
                        dependencies.followers().hasBirdKey,
                        dependencies.followers().tarinFlag,
                        dependencies.followers().playerShieldLevel);
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
        dependencies.roomLoadCoordinator().get().setActiveRoom(room, null);
    }

    void setActiveRoom(LoadedRoom room, int[][] objectPalettes) {
        dependencies.entityInputs().get().secretSeashellScreenShakeActive = false;
        dependencies.entityInputs().get().secretSeashellPegasusCollisionActive = false;
        dependencies.entityInputs().get().secretSeashellPegasusCollisionX = 0;
        dependencies.entityInputs().get().secretSeashellPegasusCollisionY = 0;
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
        dependencies.location().activeRoom = ActiveRoom.from(room, entities);
        if (dependencies.location().activeRoom.shutterDoorMask() != 0
                && dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            boolean ordinaryEventPending = dependencies.puzzles().activeRoomEvent != 0
                    && ((dependencies.puzzles().activeRoomEvent & EVENT_EFFECT_MASK) == EVENT_EFFECT_OPEN_LOCKED_DOORS
                            || (dependencies.roomSessionViews().get().activeRoomStatusFlags() & ROOM_STATUS_EVENT_1)
                                    == 0);
            if (ordinaryEventPending) {
                // $02:5E7B keeps entry doors open until Link is inside the room.
                dependencies.puzzles().pendingShutterCloseMask =
                        dependencies.location().activeRoom.shutterDoorMask();
            } else {
                dependencies.roomPuzzleController().get().openActiveRoomShutterDoors();
            }
        }
        dependencies
                .roomTransitionController()
                .get()
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
                        || (dependencies.roomSessionViews().get().activeRoomStatusFlags() & ROOM_STATUS_EVENT_2) != 0);
        if (minibossDefeated && dependencies.puzzles().activeRoomEvent == EVENT_CLEAR_MIDBOSS) {
            dependencies.roomState()
                    .dungeonProgressFlags[dependencies.location().activeRoom.mapId()] |= 0x01;
            dependencies.roomPuzzleController().get().openActiveRoomShutterDoors();
            dependencies.puzzles().activeRoomEvent = 0;
        }
        dependencies.entityBridge().get().entityRuntime = entities == null
                ? null
                : RoomEntityRuntime.from(
                        entities,
                        dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD,
                        dependencies.entityRandomByteSource(),
                        dependencies.entitySpriteHandlerCatalog(),
                        dependencies.enemyCombatTables(),
                        dependencies.chestContentsTable(),
                        dependencies.romTables(),
                        dependencies.romData());
        if (dependencies.entityBridge().get().entityRuntime != null) {
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setBooBuddyTriggerCount(dependencies.puzzles().roomTriggerCount);
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setColorShellWorld(dependencies.colorShellWorld());
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setFollowingNpcState(dependencies.followers().followingNpcState);
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setEntityMapId(dependencies.location().activeRoom.mapId());
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setEntityRoomId(dependencies.location().activeRoom.roomId());
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setOwlDialogResolver(dependencies.owlEventDialogResolver()::globalDialogId);
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setOwlDefaultMusicResolver(dependencies.owlEventDialogResolver()::defaultMusicTrack);
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setGroundInteraction(dependencies.roomCollisionQueries().get()::entityGroundInteraction);
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setBackgroundInteraction(dependencies.entityBackgroundInteraction());
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setObjectQuery(dependencies.roomCollisionQueries().get()::entityObjectSample);
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setObjectIntersectionQuery(
                            dependencies.roomCollisionQueries().get()::entityObjectIntersectionSample);
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setGroundInteractionSideScrolling(
                            dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL);
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setActionButtonsHeld(
                            dependencies.entityInputs().get().actionButtonAHeld,
                            dependencies.entityInputs().get().actionButtonBHeld);
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setTalkState(
                            dependencies.entityInputs().get().entityInventoryAppearing,
                            dependencies.entityInputs().get().entityDialogCooldown,
                            dependencies.entityInputs().get().entityWindowY);
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setJoypadHeld(dependencies.entityInputs().get().joypadHeld);
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setPressedButtonsMask(dependencies.entityInputs().get().entityPressedButtonsMask);
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setPowerBraceletButtonHeld(dependencies.entityInputs().get().powerBraceletButtonHeld);
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setBombButtonHeld(dependencies.entityInputs().get().bombButtonHeld);
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setLiftedLinkC13B(dependencies.followers().followingEntityYOffset);
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setEntityRoomStatus(dependencies.roomSessionViews().get().activeRoomStatusFlags());
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setDungeonMinibossReady(dependencies.location().activeRoom.mapId() >= 0
                            && dependencies.location().activeRoom.mapId()
                                    < dependencies.roomState().dungeonProgressFlags.length
                            && (dependencies.roomState()
                                                    .dungeonProgressFlags[
                                                    dependencies
                                                            .location()
                                                            .activeRoom
                                                            .mapId()]
                                            & 0x01)
                                    != 0);
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setTailKeyOwned(dependencies.followers().hasTailKey);
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setOwlInstrumentFlags(
                            Byte.toUnsignedInt(dependencies.roomState().dungeonProgressFlags[0]),
                            Byte.toUnsignedInt(dependencies.roomState().dungeonProgressFlags[1]));
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setSecretSeashellPegasusCollisionState(
                            dependencies.entityInputs().get().secretSeashellScreenShakeActive,
                                    dependencies.entityInputs().get().secretSeashellPegasusCollisionActive,
                            dependencies.entityInputs().get().secretSeashellPegasusCollisionX,
                                    dependencies.entityInputs().get().secretSeashellPegasusCollisionY);
            dependencies.roomLoadCoordinator().get().configureEnemyDropRuntime();
        }
        dependencies.followers().followingNpcRoomNeedsSync = true;
        dependencies.roomStoryState().get().synchronizeFollowingNpcEntitiesIfNeeded();
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
