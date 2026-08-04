package linksawakening.world;

import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;

import static linksawakening.world.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_WIDTH;

public final class RoomSession {
    private static final int W_TILESET_NO_UPDATE = 0xFF;

    private final byte[] romData;
    private final GPU gpu;
    private final RoomLoader roomLoader;
    private final OverworldTilesetTable overworldTilesetTable;
    private final OverworldCollision overworldCollision;
    private final TransientVfxSystem transientVfxSystem;
    private final DroppableRupeeSystem droppableRupeeSystem;
    private final RoomLoadListener roomLoadListener;
    private final RomRandomByteSource entityRandomByteSource = new RomRandomByteSource();

    private ActiveRoom activeRoom;
    private RoomEntityRuntime entityRuntime;
    private final int[] clearedEntitiesByRoom = new int[0x100];
    private int currentOverworldTilesetId = W_TILESET_NO_UPDATE;

    public RoomSession(byte[] romData,
                       GPU gpu,
                       RoomLoader roomLoader,
                       OverworldTilesetTable overworldTilesetTable,
                       OverworldCollision overworldCollision,
                       TransientVfxSystem transientVfxSystem,
                       DroppableRupeeSystem droppableRupeeSystem) {
        this(romData, gpu, roomLoader, overworldTilesetTable, overworldCollision,
            transientVfxSystem, droppableRupeeSystem, null);
    }

    public RoomSession(byte[] romData,
                       GPU gpu,
                       RoomLoader roomLoader,
                       OverworldTilesetTable overworldTilesetTable,
                       OverworldCollision overworldCollision,
                       TransientVfxSystem transientVfxSystem,
                       DroppableRupeeSystem droppableRupeeSystem,
                       RoomLoadListener roomLoadListener) {
        this.romData = romData;
        this.gpu = gpu;
        this.roomLoader = roomLoader;
        this.overworldTilesetTable = overworldTilesetTable;
        this.overworldCollision = overworldCollision;
        this.transientVfxSystem = transientVfxSystem;
        this.droppableRupeeSystem = droppableRupeeSystem;
        this.roomLoadListener = roomLoadListener;
    }

    public void loadInitialOverworld(int roomId) {
        gpu.loadBaseOverworldTiles(romData);
        loadRoomSpecificTilesIfNeeded(roomId);
        loadOverworld(roomId);
    }

    public void loadWarpDestination(Warp warp) {
        boolean wasIndoor = activeRoom != null && activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD;
        if (warp.category() == Warp.CATEGORY_OVERWORLD) {
            if (wasIndoor) {
                gpu.loadBaseOverworldTiles(romData);
                currentOverworldTilesetId = W_TILESET_NO_UPDATE;
            }
            loadOverworld(warp.destRoom());
            loadRoomSpecificTilesIfNeeded(warp.destRoom());
        } else {
            loadIndoor(warp.destMap(), warp.destRoom(), warp.category());
        }
    }

    public void startAdjacentOverworldScroll(ScrollController scrollController,
                                             int direction,
                                             int linkScreenX,
                                             int linkScreenY) {
        RoomRenderSnapshot previousRoom = renderSnapshot();
        int nextRoomId = adjacentOverworldRoomId(direction);
        loadRoomSpecificTilesIfNeeded(nextRoomId);
        loadOverworld(nextRoomId);
        scrollController.start(direction, linkScreenX, linkScreenY, previousRoom, scrollTarget(direction));
    }

    public void startAdjacentIndoorScroll(ScrollController scrollController,
                                          int direction,
                                          int linkScreenX,
                                          int linkScreenY) {
        RoomRenderSnapshot previousRoom = renderSnapshot();
        int nextRoomId = adjacentIndoorRoomId(direction);
        loadIndoor(activeRoom.mapId(), nextRoomId, activeRoom.mapCategory());
        scrollController.start(direction, linkScreenX, linkScreenY, previousRoom, scrollTarget(direction));
    }

    public void loadOverworld(int roomId) {
        clearTransientRoomState();
        LoadedRoom room = roomLoader.loadOverworld(roomId, clearedEntitiesByRoom[roomId], null);
        gpu.loadAnimatedTilesGroup(romData, room.animatedTilesGroup());
        setActiveRoom(room);
        overworldCollision.setRoom(activeRoom.roomObjectsArea());
        overworldCollision.setGbcOverlay(activeRoom.gbcOverlay());
        overworldCollision.setPhysicsTable(RomTables.PHYSICS_TABLE_OVERWORLD);
    }

    public void loadIndoor(int mapId, int roomId) {
        loadIndoor(mapId, roomId, Warp.CATEGORY_INDOOR);
    }

    public void loadIndoor(int mapId, int roomId, int mapCategory) {
        clearTransientRoomState();
        gpu.loadIndoorTiles(romData, mapId, roomId);
        LoadedRoom room = roomLoader.loadIndoor(
            mapId, roomId, activeRoom == null ? null : activeRoom.palettes(), mapCategory,
            clearedEntitiesByRoom[roomId]);
        gpu.loadAnimatedTilesGroup(romData, room.animatedTilesGroup());
        setActiveRoom(room);
        overworldCollision.setRoom(activeRoom.roomObjectsArea());
        overworldCollision.setGbcOverlay(null);
        overworldCollision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
    }

    public ActiveRoom activeRoom() {
        return activeRoom;
    }

    public boolean hasActiveRoom() {
        return activeRoom != null;
    }

    public int currentRoomId() {
        return activeRoom.roomId();
    }

    public int mapCategory() {
        return activeRoom.mapCategory();
    }

    public int mapId() {
        return activeRoom.mapId();
    }

    public int[][] palettes() {
        return activeRoom == null ? null : activeRoom.palettes();
    }

    public RoomRenderSnapshot renderSnapshot() {
        return activeRoom == null ? null : activeRoom.renderSnapshot();
    }

    public void tickEntities(int frameCounter) {
        tickEntities(frameCounter, 0, 0);
    }

    public void tickEntities(int frameCounter, int linkPixelX, int linkPixelY) {
        if (activeRoom == null || entityRuntime == null) {
            return;
        }
        // rLY is not a meaningful value in the host renderer. Keep the
        // non-emulator policy explicit while preserving the ROM seed update.
        entityRandomByteSource.beginFrame(frameCounter & 0xFF, 0);
        entityRuntime.tick(frameCounter, linkPixelX, linkPixelY, entityRandomByteSource);
        activeRoom.replaceEntities(entityRuntime.snapshot());
    }

    public int clearEntity(int slot) {
        if (activeRoom == null || entityRuntime == null) {
            return 0;
        }
        int mask = entityRuntime.clearEntity(slot);
        if (mask != 0) {
            clearedEntitiesByRoom[activeRoom.roomId()] |= mask;
        }
        activeRoom.replaceEntities(entityRuntime.snapshot());
        return mask;
    }

    /**
     * Applies the ROM pickup collision cadence to the active room entities.
     * Item-specific effects are returned to the gameplay layer so room state
     * and persistent entity state stay separate.
     */
    public EntityPickupEvent collectEntityIfNeeded(int frameCounter,
                                                   int linkPixelX,
                                                   int linkPixelY,
                                                   boolean linkAirborne,
                                                   boolean linkInteractive) {
        if (activeRoom == null || entityRuntime == null) {
            return null;
        }
        EntityPickupEvent event = entityRuntime.collectIfNeeded(
            frameCounter, linkPixelX, linkPixelY, linkAirborne, linkInteractive);
        if (event == null) {
            return null;
        }
        if (event.persistentClearMask() != 0) {
            clearedEntitiesByRoom[activeRoom.roomId()] |= event.persistentClearMask();
        }
        activeRoom.replaceEntities(entityRuntime.snapshot());
        return event;
    }

    public RoomBoundaryState boundaryState(int linkX, int linkY) {
        return new RoomBoundaryState(
            activeRoom.mapCategory(),
            activeRoom.roomId(),
            activeRoom.indoorHasSouthEntrance(),
            activeRoom.hasWarps(),
            linkX,
            linkY
        );
    }

    private void setActiveRoom(LoadedRoom room) {
        RoomEntitySnapshot entities = room.entities();
        if (entities != null && entities.spriteSelection() != null) {
            var selection = entities.spriteSelection();
            if (selection.hasStandardSheets()) {
                gpu.loadEntitySpriteSheets(romData, selection.sheetValues());
                entities = entities.withSpriteTiles(gpu.snapshotEntityTiles());
            }
        }
        activeRoom = ActiveRoom.from(room, entities);
        entityRuntime = entities == null ? null : RoomEntityRuntime.from(
            entities, activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD,
            entityRandomByteSource);
        if (roomLoadListener != null) {
            roomLoadListener.roomLoaded(activeRoom);
        }
    }

    private void clearTransientRoomState() {
        if (transientVfxSystem != null) {
            transientVfxSystem.clear();
        }
        if (droppableRupeeSystem != null) {
            droppableRupeeSystem.clear();
        }
    }

    private void loadRoomSpecificTilesIfNeeded(int roomId) {
        int tilesetId = overworldTilesetTable.tilesetIdForRoom(roomId);
        if (OverworldTilesetTable.shouldLoadRoomSpecificTileset(roomId, tilesetId, currentOverworldTilesetId)) {
            gpu.loadRoomSpecificTiles(romData, roomId, tilesetId);
            currentOverworldTilesetId = tilesetId;
        }
    }

    private int adjacentOverworldRoomId(int direction) {
        int nextRoomId = activeRoom.roomId();
        switch (direction) {
            case ScrollController.UP:
                return nextRoomId - 16;
            case ScrollController.DOWN:
                return nextRoomId + 16;
            case ScrollController.LEFT:
                return nextRoomId - 1;
            case ScrollController.RIGHT:
                return nextRoomId + 1;
            default:
                return nextRoomId;
        }
    }

    private int adjacentIndoorRoomId(int direction) {
        int nextRoomId = activeRoom.roomId();
        switch (direction) {
            case ScrollController.UP:
                nextRoomId -= 8;
                break;
            case ScrollController.DOWN:
                nextRoomId += 8;
                break;
            case ScrollController.LEFT:
                nextRoomId -= 1;
                break;
            case ScrollController.RIGHT:
                nextRoomId += 1;
                break;
            default:
                break;
        }
        return nextRoomId & 0xFF;
    }

    private static int scrollTarget(int direction) {
        return (direction == ScrollController.LEFT || direction == ScrollController.RIGHT)
            ? ROOM_PIXEL_WIDTH
            : ROOM_PIXEL_HEIGHT;
    }
}
