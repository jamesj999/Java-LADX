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
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RomEnemyCombatTables;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.entity.event.LiftedEntityState;
import linksawakening.world.object.SwitchBlockAnimation;
import linksawakening.world.transition.Warp;

/** LoadRoom: coordinates ROM room acquisition, initialization and entity replacement. */
final class RoomLoadCoordinator implements RoomRooms {
    private final LoadedRoomHandle rooms;
    private final Dependencies dependencies;

    PreviousPalettes previousPalettes() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? PreviousPalettes.NONE : PreviousPalettes.from(loaded);
    }

    @Override
    public boolean musicSelectionIsScroll() {
        return musicSelectionIsScroll;
    }

    @Override
    public ActiveRoom requireActiveRoom() {
        RoomRuntime loaded = rooms.currentOrNull();
        return java.util.Objects.requireNonNull(loaded).room;
    }

    @Override
    public void loadIndoorFromSavedPosition(int map, int room, int position) {
        loadIndoorFromSavedPosition(previousPalettes(), map, room, position);
    }

    @Override
    public void loadIndoor(int map, int room, int category) {
        loadIndoor(previousPalettes(), map, room, category);
    }

    @Override
    public void loadIndoor(int map, int room) {
        loadIndoor(previousPalettes(), map, room);
    }

    @Override
    public void loadOverworld(int room) {
        acquireLoadOverworld(room);
    }

    @Override
    public void loadInitialOverworld(int room) {
        acquireLoadInitialOverworld(room);
    }

    @Override
    public LiftedEntityState liftedEntityState() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null
                ? LiftedEntityState.none()
                : loaded.entities.inspection().liftedEntityState();
    }

    @Override
    public RoomRenderSnapshot renderSnapshotOrNull() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded != null ? loaded.renderSnapshot() : null;
    }

    @Override
    public RoomRenderSnapshot.View renderViewOrNull() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded != null ? loaded.renderView : null;
    }

    @Override
    public String moblinKingTrace() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? "-1" : loaded.entities.inspection().moblinKingTrace();
    }

    @Override
    public String instrumentTrace() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? "" : loaded.entities.inspection().instrumentTrace();
    }

    @Override
    public boolean heartPiecePickupActive() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded != null && loaded.entities.inspection().heartPiecePickupActive();
    }

    @Override
    public int[][] palettesOrNull() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded == null ? null : loaded.room.palettes();
    }

    @Override
    public int indoorRoomPositionForSave() {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded == null || loaded.room.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            return -1;
        }
        return dependencies.location().indoorMapPosition;
    }

    @Override
    public int mapId() {
        RoomRuntime loaded = rooms.currentOrNull();
        return java.util.Objects.requireNonNull(loaded).room.mapId();
    }

    @Override
    public int mapCategory() {
        RoomRuntime loaded = rooms.currentOrNull();
        return java.util.Objects.requireNonNull(loaded).room.mapCategory();
    }

    @Override
    public int currentRoomId() {
        RoomRuntime loaded = rooms.currentOrNull();
        return java.util.Objects.requireNonNull(loaded).room.roomId();
    }

    @Override
    public boolean hasActiveRoom() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded != null;
    }

    @Override
    public ActiveRoom activeRoomOrNull() {
        RoomRuntime loaded = rooms.currentOrNull();
        return loaded != null ? loaded.room : null;
    }

    boolean musicSelectionIsScroll;

    RoomLoadCoordinator(LoadedRoomHandle rooms, Dependencies dependencies) {
        this.rooms = java.util.Objects.requireNonNull(rooms);
        this.dependencies = dependencies;
    }

    RoomRuntime acquireLoadInitialOverworld(int roomId) {
        dependencies.gpu().loadBaseOverworldTiles(dependencies.romData());
        this.loadRoomSpecificTilesIfNeeded(roomId);
        return this.acquireLoadOverworld(roomId);
    }

    RoomRuntime acquireLoadOverworld(int roomId) {
        return acquireLoadOverworld(roomId, false);
    }

    RoomRuntime acquireLoadOverworld(int roomId, boolean scrolling) {
        musicSelectionIsScroll = scrolling;
        dependencies.location().roomLoadSerial++;
        dependencies.location().indoorMapPosition = -1;
        dependencies.resetRoomTransients().run();
        var puzzles = new RoomPuzzleState.PerRoom();
        var overrides = new RoomTerrainState.Overrides();
        dependencies.indoorTorchPaletteEffect().clear();
        dependencies.entityFrame().recentRoomEntityClears.visit(roomId);
        LoadedRoom room = dependencies
                .roomLoader()
                .loadOverworld(
                        roomId,
                        dependencies.entityFrame().recentRoomEntityClears.mask(roomId),
                        dependencies.roomState().overworldRoomStatus);
        dependencies.gpu().loadAnimatedTilesGroup(dependencies.romData(), room.animatedTilesGroup());
        RoomRuntime loaded = this.setActiveRoom(room, null, puzzles, overrides);
        dependencies.roomStoryState().updateWitchEnvironment(loaded);
        dependencies.overworldCollision().setRoom(loaded.room.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(loaded.room.gbcOverlay());
        dependencies.overworldCollision().setPhysicsTable(RomTables.PHYSICS_TABLE_OVERWORLD);
        this.synchronizeSwitchBlockCollisionState();
        return loaded;
    }

    RoomRuntime loadIndoor(PreviousPalettes previous, int mapId, int roomId) {
        return this.loadIndoor(previous, mapId, roomId, Warp.CATEGORY_INDOOR);
    }

    RoomRuntime loadIndoorFromSavedPosition(
            PreviousPalettes previous, int mapId, int roomId, int savedIndoorMapPosition) {
        RoomRuntime loaded = this.loadIndoor(previous, mapId, roomId, Warp.CATEGORY_INDOOR);
        dependencies.location().indoorMapPosition = savedIndoorMapPosition & 0xFF;
        return loaded;
    }

    RoomRuntime loadIndoor(PreviousPalettes previous, int mapId, int roomId, int mapCategory) {
        return this.loadIndoor(previous, mapId, roomId, mapCategory, 4);
    }

    RoomRuntime loadIndoor(PreviousPalettes previous, int mapId, int roomId, int mapCategory, int transitionDirection) {
        musicSelectionIsScroll = transitionDirection < 4;
        dependencies.location().roomLoadSerial++;
        dependencies.location().indoorMapPosition =
                dependencies.roomSessionViews().findIndoorMapPosition(mapId, roomId);
        dependencies.resetRoomTransients().run();
        var puzzles = new RoomPuzzleState.PerRoom();
        var overrides = new RoomTerrainState.Overrides();
        dependencies.entityFrame().recentRoomEntityClears.visit(roomId);
        dependencies.roomState().dungeonItemState.loadForMap(mapId, true);
        // LoadRoom $00:3162: side-view passages do not mark the map visited.
        if (mapCategory != Warp.CATEGORY_SIDESCROLL)
            dependencies.roomSessionViews().indoorStatusTableForMap(mapId)[roomId] |= (byte) 0x80;
        puzzles.activeRoomEvent = dependencies.dungeonRoomEventTable().eventFor(mapId, roomId);
        dependencies
                .gpu()
                .loadIndoorTiles(dependencies.romData(), mapId, roomId, mapCategory == Warp.CATEGORY_SIDESCROLL);
        LoadedRoom room = dependencies
                .roomLoader()
                .loadIndoor(new IndoorRoomLoadInputs(
                        mapId,
                        roomId,
                        previous.background(),
                        mapCategory,
                        dependencies.entityFrame().recentRoomEntityClears.mask(roomId),
                        dependencies.roomSessionViews().indoorStatusTableForMap(mapId),
                        dependencies.followers().hasBirdKey,
                        new RoomNpcPaletteInputs(
                                dependencies.followers().tarinFlag, dependencies.followers().playerShieldLevel)));
        dependencies.gpu().loadAnimatedTilesGroup(dependencies.romData(), room.animatedTilesGroup());
        var palettes = dependencies
                .roomPaletteLoader()
                .indoorTransition(
                        mapId,
                        roomId,
                        transitionDirection,
                        room.palettes(),
                        room.entities().spriteSelection().objectPalettes(),
                        previous.background(),
                        previous.objects());
        RoomRuntime loaded = this.setActiveRoom(room, palettes.objects(), puzzles, overrides);
        for (int row = 0; row < 8; row++)
            System.arraycopy(palettes.background()[row], 0, loaded.room.palettes()[row], 0, 4);
        this.initializeSwitchBlockTiles(loaded);
        dependencies
                .indoorTorchPaletteEffect()
                .reset(
                        loaded.room.palettes(),
                        dependencies.roomObjectMutations().countUnlitTorches(loaded.contents));
        dependencies.roomObjectMutations().copyIndoorTorchPalettesToActiveRoom(loaded.contents);
        dependencies.roomStoryState().updateWitchEnvironment(loaded);
        dependencies.overworldCollision().setRoom(loaded.room.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(null);
        dependencies.overworldCollision().setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
        this.synchronizeSwitchBlockCollisionState();
        return loaded;
    }

    RoomRuntime setActiveRoom(
            LoadedRoom room,
            int[][] objectPalettes,
            RoomPuzzleState.PerRoom puzzles,
            RoomTerrainState.Overrides overrides) {
        dependencies.entityInputs().secretSeashellScreenShakeActive = false;
        dependencies.entityInputs().secretSeashellPegasusCollisionActive = false;
        dependencies.entityInputs().secretSeashellPegasusCollisionX = 0;
        dependencies.entityInputs().secretSeashellPegasusCollisionY = 0;
        RoomEntitySnapshot entities = prepareEntities(room, objectPalettes);
        ActiveRoom active = ActiveRoom.from(room, entities);
        RoomContents contents = new RoomContents(active, puzzles, overrides);
        configureDoorAndEventState(contents);
        var runtime = RoomEntityRuntime.builder(entities)
                .indoorRoom(active.mapCategory() != Warp.CATEGORY_OVERWORLD)
                .randomByteSupplier(dependencies.entityRandomByteSource())
                .spriteHandlers(dependencies.entitySpriteHandlerCatalog())
                .enemyCombatTables(dependencies.enemyCombatTables())
                .chestContentsTable(dependencies.chestContentsTable())
                .romTables(dependencies.romTables())
                .rom(dependencies.romData())
                .build();
        RoomRuntime loaded = new RoomRuntime(contents, runtime, dependencies.roomRuntimeServices());
        dependencies.installRoom().accept(loaded);
        configureRuntimeWorld(loaded);
        configureRuntimeInputs(loaded);
        configureRuntimeProgress(loaded);
        dependencies.followers().followingNpcRoomNeedsSync = true;
        loaded = dependencies.roomStoryState().synchronizeFollowingNpcEntitiesIfNeeded(loaded);
        return dependencies.roomLoaded().apply(loaded);
    }

    void initializeSwitchBlockTiles(RoomRuntime loaded) {
        // LoadRoom configures wRoomSwitchableObject only for $DB/$DC.
        // These VRAM slots hold furniture in houses and must stay intact there.
        boolean hasBlocks = false;
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int col = 0; col < RoomConstants.OBJECTS_PER_ROW; col++) {
                int object = loaded.room
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

    void configureEnemyDropRuntime(RoomRuntime loaded) {
        dependencies
                .entityFrame()
                .configureEnemyDropRuntime(loaded, dependencies.enemyDropResolver(), dependencies.entityInputs());
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
            RoomEntitySessionState entityFrame,
            RoomFollowerState followers,
            RoomLocationState location,
            RoomTerrainState terrain,
            ChestContentsTable chestContentsTable,
            DungeonRoomEventTable dungeonRoomEventTable,
            RomEnemyCombatTables enemyCombatTables,
            EnemyDropResolver enemyDropResolver,
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
            java.util.function.UnaryOperator<RoomRuntime> roomLoaded,
            RoomLoader roomLoader,
            RoomObjectMutations roomObjectMutations,
            RoomPaletteLoader roomPaletteLoader,
            RoomPuzzleController roomPuzzleController,
            RoomSessionViews roomSessionViews,
            RoomWorldState roomState,
            RoomStoryState roomStoryState,
            Runnable resetRoomTransients,
            java.util.function.Consumer<RoomRuntime> installRoom,
            RoomRuntime.Services roomRuntimeServices) {
        Dependencies {
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(terrain, "terrain");
            java.util.Objects.requireNonNull(chestContentsTable, "chestContentsTable");
            java.util.Objects.requireNonNull(dungeonRoomEventTable, "dungeonRoomEventTable");
            java.util.Objects.requireNonNull(enemyCombatTables, "enemyCombatTables");
            java.util.Objects.requireNonNull(enemyDropResolver, "enemyDropResolver");
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
            java.util.Objects.requireNonNull(roomLoader, "roomLoader");
            java.util.Objects.requireNonNull(roomObjectMutations, "roomObjectMutations");
            java.util.Objects.requireNonNull(roomPaletteLoader, "roomPaletteLoader");
            java.util.Objects.requireNonNull(roomPuzzleController, "roomPuzzleController");
            java.util.Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            java.util.Objects.requireNonNull(roomState, "roomState");
            java.util.Objects.requireNonNull(roomStoryState, "roomStoryState");
            java.util.Objects.requireNonNull(resetRoomTransients, "resetRoomTransients");
            java.util.Objects.requireNonNull(installRoom, "installRoom");
            java.util.Objects.requireNonNull(roomLoaded, "roomLoaded");
            java.util.Objects.requireNonNull(roomRuntimeServices, "roomRuntimeServices");
        }
    }

    private RoomEntitySnapshot prepareEntities(LoadedRoom room, int[][] objectPalettes) {
        RoomEntitySnapshot entities = java.util.Objects.requireNonNull(room.entities(), "loaded room entities");
        if (objectPalettes != null && entities.spriteSelection() != null) {
            entities = entities.withSpriteSelection(entities.spriteSelection().withObjectPalettes(objectPalettes));
        }
        if (entities.spriteSelection() != null) {
            var selection = entities.spriteSelection();
            if (selection.hasStandardSheets()) {
                dependencies.gpu().loadEntitySpriteSheets(dependencies.romData(), selection.sheetValues());
                entities = entities.withSpriteTiles(dependencies.gpu().snapshotEntityTiles());
            } else if (selection.roomTable() == EntityRoomLoader.RoomTable.COLOR_DUNGEON) {
                dependencies.gpu().loadColorDungeonEntitySheets(dependencies.romData(), selection.roomId());
                entities = entities.withSpriteTiles(dependencies.gpu().snapshotEntityTiles());
            }
        }
        entities = entities.withSideScrolling(room.mapCategory() == Warp.CATEGORY_SIDESCROLL);
        return entities;
    }

    private void configureDoorAndEventState(RoomContents contents) {
        if (contents.room.shutterDoorMask() != 0 && contents.room.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            boolean ordinaryEventPending = contents.puzzles.activeRoomEvent != 0
                    && ((contents.puzzles.activeRoomEvent & EVENT_EFFECT_MASK) == EVENT_EFFECT_OPEN_LOCKED_DOORS
                            || (dependencies.roomSessionViews().activeRoomStatusFlags(contents.room)
                                            & ROOM_STATUS_EVENT_1)
                                    == 0);
            if (ordinaryEventPending) {
                // $02:5E7B keeps entry doors open until Link is inside the room.
                contents.puzzles.pendingShutterCloseMask = contents.room.shutterDoorMask();
            } else {
                dependencies.roomPuzzleController().openActiveRoomShutterDoors(contents);
            }
        }
        contents.puzzles.configureStaircase(contents.room.staircaseLocation());
        boolean minibossDefeated = contents.room.mapCategory() != Warp.CATEGORY_OVERWORLD
                && contents.room.mapId() >= 0
                && contents.room.mapId() < dependencies.roomState().dungeonProgressFlags.length
                && ((dependencies.roomState().dungeonProgressFlags[contents.room.mapId()] & 0x01) != 0
                        || (dependencies.roomSessionViews().activeRoomStatusFlags(contents.room) & ROOM_STATUS_EVENT_2)
                                != 0);
        if (minibossDefeated && contents.puzzles.activeRoomEvent == EVENT_CLEAR_MIDBOSS) {
            dependencies.roomState().dungeonProgressFlags[contents.room.mapId()] |= 0x01;
            dependencies.roomPuzzleController().openActiveRoomShutterDoors(contents);
            contents.puzzles.activeRoomEvent = 0;
        }
    }

    private void configureRuntimeWorld(RoomRuntime loaded) {
        var configuration = loaded.entities.configuration();

        configuration.setBooBuddyTriggerCount(loaded.contents.puzzles.roomTriggerCount);
        configuration.setColorShellWorld(loaded.colorShellWorld);
        configuration.setFollowingNpcState(dependencies.followers().followingNpcState);
        configuration.setEntityMapId(loaded.room.mapId());
        configuration.setEntityRoomId(loaded.room.roomId());
        configuration.setOwlDialogResolver(dependencies.owlEventDialogResolver()::globalDialogId);
        configuration.setOwlDefaultMusicResolver(dependencies.owlEventDialogResolver()::defaultMusicTrack);
        configuration.setGroundInteraction(loaded.groundInteraction);
        configuration.setBackgroundInteraction(loaded.backgroundInteraction);
    }

    private void configureRuntimeInputs(RoomRuntime loaded) {
        var configuration = loaded.entities.configuration();

        configuration.setObjectQuery(loaded.objectQuery);
        configuration.setObjectIntersectionQuery(loaded.objectIntersectionQuery);
        configuration.setGroundInteractionSideScrolling(loaded.room.mapCategory() == Warp.CATEGORY_SIDESCROLL);
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

    private void configureRuntimeProgress(RoomRuntime loaded) {
        var configuration = loaded.entities.configuration();

        configuration.setLiftedLinkC13B(dependencies.followers().followingEntityYOffset);
        configuration.setEntityRoomStatus(dependencies.roomSessionViews().activeRoomStatusFlags(loaded.room));
        configuration.setDungeonMinibossReady(loaded.room.mapId() >= 0
                && loaded.room.mapId() < dependencies.roomState().dungeonProgressFlags.length
                && (dependencies.roomState().dungeonProgressFlags[loaded.room.mapId()] & 0x01) != 0);
        configuration.setTailKeyOwned(dependencies.followers().hasTailKey);
        configuration.setOwlInstrumentFlags(
                Byte.toUnsignedInt(dependencies.roomState().dungeonProgressFlags[0]),
                Byte.toUnsignedInt(dependencies.roomState().dungeonProgressFlags[1]));
        configuration.setSecretSeashellPegasusCollisionState(
                dependencies.entityInputs().secretSeashellScreenShakeActive,
                        dependencies.entityInputs().secretSeashellPegasusCollisionActive,
                dependencies.entityInputs().secretSeashellPegasusCollisionX,
                        dependencies.entityInputs().secretSeashellPegasusCollisionY);
        this.configureEnemyDropRuntime(loaded);
    }

    /** Optional previous palette planes; absence means the first room or missing sprite metadata. */
    record PreviousPalettes(int[][] background, int[][] objects) {
        static final PreviousPalettes NONE = new PreviousPalettes(null, null);

        static PreviousPalettes from(RoomRuntime loaded) {
            var selection = loaded.room.entityView().spriteSelection();
            return new PreviousPalettes(loaded.room.palettes(), selection == null ? null : selection.objectPalettes());
        }
    }
}
