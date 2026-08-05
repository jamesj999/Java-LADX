package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static linksawakening.world.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_WIDTH;

public final class RoomSession {
    private static final int W_TILESET_NO_UPDATE = 0xFF;
    private static final int ENTITY_WATER_TEKTITE = 0x99;
    private static final int ENTITY_OPT1_NO_GROUND_INTERACTION = 0x10;
    private static final int[] ENTITY_CONVEYOR_MOVEMENT_X = {0, 0, -1, 1, 1, -1, 1, -1};
    private static final int[] ENTITY_CONVEYOR_MOVEMENT_Y = {1, -1, 0, 0, 1, 1, -1, -1};

    private final byte[] romData;
    private final GPU gpu;
    private final RoomLoader roomLoader;
    private final OverworldTilesetTable overworldTilesetTable;
    private final OverworldCollision overworldCollision;
    private final TransientVfxSystem transientVfxSystem;
    private final DroppableRupeeSystem droppableRupeeSystem;
    private final RoomLoadListener roomLoadListener;
    private final RoomTilemapBuilder roomTilemapBuilder;
    private final RomRandomByteSource entityRandomByteSource = new RomRandomByteSource();
    private final EntitySpriteHandlerCatalog entitySpriteHandlerCatalog;
    private final FollowingNpcEntitySpawner followingNpcEntitySpawner;
    private final RomEnemyCombatTables enemyCombatTables;
    private final RomTables romTables;
    private final LinkPositionHistory followingLinkPositionHistory = new LinkPositionHistory();

    private ActiveRoom activeRoom;
    private RoomEntityRuntime entityRuntime;
    private final int[] clearedEntitiesByRoom = new int[0x100];
    private int currentOverworldTilesetId = W_TILESET_NO_UPDATE;
    private FollowingNpcState followingNpcState = FollowingNpcState.none();
    private int followingLinkX = 0x08;
    private int followingLinkY = 0x10;
    private int followingLinkZ;
    private int followingEntityYOffset;
    private int followingLinkDirection;
    private boolean followingNpcRoomNeedsSync;
    private GameplaySoundSink colorShellSoundSink = GameplaySoundSink.none();
    private final ColorShellWorld colorShellWorld = new ColorShellWorld() {
        @Override
        public int objectAt(RoomEntity entity, int relativeOffset) {
            return colorShellObjectAt(entity, relativeOffset);
        }

        @Override
        public void writeObject(RoomEntity entity, int objectId) {
            writeColorShellObject(entity, objectId);
        }

        @Override
        public void playJingle(int id) {
            if ((id & 0xFF) == 0x1D) {
                colorShellSoundSink.play(GameplaySoundEvent.WRONG_ANSWER);
            }
        }

        @Override
        public void playNoise(int id) {
            if ((id & 0xFF) == 0x04) {
                colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
            }
        }

        @Override
        public void spawnPoof(int x, int y) {
            if (transientVfxSystem != null) {
                transientVfxSystem.spawn(TransientVfxType.POOF, x, y);
            }
        }
    };

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
        this.roomTilemapBuilder = new RoomTilemapBuilder(romData);
        this.entitySpriteHandlerCatalog = new EntitySpriteHandlerCatalog(romData);
        this.followingNpcEntitySpawner = new FollowingNpcEntitySpawner(entitySpriteHandlerCatalog);
        this.enemyCombatTables = new RomEnemyCombatTables(romData);
        this.romTables = RomTables.loadFromRom(romData);
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

    /**
     * Applies the follower display-list selections used by
     * {@code CreateFollowingNpcEntity}. This low-level hook is retained for
     * callers that already own the dynamic entity state; new code should use
     * {@link #setFollowingNpcState(FollowingNpcState, int, int, int, int, int)}
     * so spawning and selection stay in one ROM-shaped path.
     */
    public void setFollowerSpriteOverrides(Map<Integer, EntitySpriteDefinition> overrides) {
        if (activeRoom == null || activeRoom.entities() == null
            || activeRoom.entities().spriteSelection() == null) {
            return;
        }
        Map<Integer, EntitySpriteDefinition> merged = new HashMap<>(
            activeRoom.entities().spriteSelection().spriteOverrides());
        if (overrides != null) {
            merged.putAll(overrides);
        }
        RoomEntitySnapshot updated = activeRoom.entities().withSpriteSelection(
            activeRoom.entities().spriteSelection().withSpriteOverrides(merged));
        activeRoom.replaceEntities(updated);
        if (entityRuntime != null) {
            entityRuntime.setSpriteSelection(updated.spriteSelection());
        }
    }

    /**
     * Binds the current WRAM-like follower flags and Link coordinates to the
     * active room. The operation is applied once per room load or follower
     * state change, matching the room-transition caller in room_transition.asm.
     * Coordinates are hLinkPositionX/Y/Z and entityYOffset is wC13B.
     */
    public void setFollowingNpcState(FollowingNpcState state,
                                     int linkX,
                                     int linkY,
                                     int linkZ,
                                     int entityYOffset,
                                     int linkDirection) {
        if (state == null) {
            throw new IllegalArgumentException("Follower state cannot be null");
        }
        followingLinkX = linkX & 0xFF;
        followingLinkY = linkY & 0xFF;
        followingLinkZ = linkZ & 0xFF;
        followingEntityYOffset = entityYOffset & 0xFF;
        followingLinkDirection = linkDirection & 0xFF;
        if (!state.equals(followingNpcState)) {
            followingNpcRoomNeedsSync = true;
        }
        followingNpcState = state;
        synchronizeFollowingNpcEntitiesIfNeeded();
    }

    public FollowingNpcState followingNpcState() {
        return followingNpcState;
    }

    public void setColorShellSoundSink(GameplaySoundSink soundSink) {
        colorShellSoundSink = soundSink == null ? GameplaySoundSink.none() : soundSink;
    }

    void setColorShellStateForTest(int slot, int state, int transitionCountdown,
                                   int direction, int speedX, int speedY) {
        if (entityRuntime != null) {
            entityRuntime.setColorShellStateForTest(slot, state, transitionCountdown,
                direction, speedX, speedY);
        }
    }

    public void tickEntities(int frameCounter) {
        tickEntities(frameCounter, 0, 0);
    }

    public void tickEntities(int frameCounter, int linkEntityX, int linkEntityY) {
        tickEntities(frameCounter, linkEntityX, linkEntityY,
            followingLinkZ, followingLinkDirection);
    }

    public void tickEntities(int frameCounter, int linkEntityX, int linkEntityY,
                             int linkEntityZ, int linkDirection) {
        tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY,
            linkEntityZ, EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE,
            linkDirection, false);
    }

    /**
     * Advances entities and returns the ROM projectile interactions generated
     * during this frame.  The older tick overloads route through this method
     * with a non-interactive Link state, so callers that do not provide player
     * interaction state retain their previous behavior.
     */
    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
                             int frameCounter, int linkEntityX, int linkEntityY,
                             int linkEntityZ, int linkMotionState, int linkDirection,
                             boolean usingShield) {
        return tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY,
            linkEntityZ, linkMotionState, linkDirection, usingShield, 1, 0);
    }

    /** Advances entities with the additional ROM shield and invincibility fields. */
    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
                             int frameCounter, int linkEntityX, int linkEntityY,
                             int linkEntityZ, int linkMotionState, int linkDirection,
                             boolean usingShield, int shieldLevel,
                             int invincibilityCounter) {
        return tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY,
            linkEntityZ, linkMotionState, linkDirection, usingShield, shieldLevel,
            invincibilityCounter, false, 0, 0, 0, 0);
    }

    /** Advances entities with Link's optional current sword collision rectangle. */
    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
                             int frameCounter, int linkEntityX, int linkEntityY,
                             int linkEntityZ, int linkMotionState, int linkDirection,
                             boolean usingShield, int shieldLevel,
                             int invincibilityCounter, boolean swordCollisionActive,
                             int swordX, int swordWidth, int swordY, int swordHeight) {
        followingLinkX = linkEntityX & 0xFF;
        followingLinkY = linkEntityY & 0xFF;
        followingLinkZ = linkEntityZ & 0xFF;
        followingLinkDirection = linkDirection & 0xFF;
        if (activeRoom == null || entityRuntime == null) {
            return List.of();
        }
        // rLY is not a meaningful value in the host renderer. Keep the
        // non-emulator policy explicit while preserving the ROM seed update.
        entityRandomByteSource.beginFrame(frameCounter & 0xFF, 0);
        List<EntityProjectileEvent> events = entityRuntime.tickWithProjectileEvents(
            frameCounter, linkEntityX, linkEntityY, entityRandomByteSource,
            this::entityBackgroundCollision, this::pairoddProjectileObjectCollision,
            followingLinkPositionHistory, followingLinkZ,
            followingLinkDirection, followingEntityYOffset,
            new EnemyProjectileCollision.LinkState(
                linkEntityX, linkEntityY, linkEntityZ, linkMotionState,
                romDirectionForProjectileCollision(linkDirection), usingShield, shieldLevel,
                invincibilityCounter), swordCollisionActive, swordX, swordWidth,
            swordY, swordHeight);
        if (transientVfxSystem != null) {
            for (RoomEntityRuntime.TransientVfxRequest request
                : entityRuntime.transientVfxRequests()) {
                transientVfxSystem.spawn(request.type(), request.worldX(), request.worldY());
            }
        }
        int clearedMask = entityRuntime.consumePendingClearedEntityMask();
        if (clearedMask != 0) {
            clearedEntitiesByRoom[activeRoom.roomId()] |= clearedMask;
        }
        activeRoom.replaceEntities(entityRuntime.snapshot());
        return events;
    }

    /** Returns and clears entity side-effect events emitted by the last tick. */
    public List<EntityCombatEvent> consumeEntityEvents() {
        if (entityRuntime == null) {
            return List.of();
        }
        return entityRuntime.consumePendingEntityEvents();
    }

    /** Link uses Java's down/up/left/right order; projectile ROM tables use right/left/up/down. */
    private static int romDirectionForProjectileCollision(int linkDirection) {
        if (linkDirection < 0 || linkDirection > 3) {
            throw new IllegalArgumentException("Link direction out of range: " + linkDirection);
        }
        return switch (linkDirection) {
            case 0 -> 3; // Java down -> ROM down
            case 1 -> 2; // Java up -> ROM up
            case 2 -> 1; // Java left -> ROM left
            case 3 -> 0; // Java right -> ROM right
            default -> throw new AssertionError(linkDirection);
        };
    }

    /** Applies the active room's ROM enemy/sword collision pass. */
    public List<EntityCombatEvent> resolveEntityCombat(int frameCounter,
                                                       int linkEntityX,
                                                       int linkEntityY,
                                                       boolean linkAirborne,
                                                       boolean linkInteractive,
                                                       boolean swordCollisionActive,
                                                       int swordX,
                                                       int swordWidth,
                                                       int swordY,
                                                       int swordHeight) {
        return resolveEntityCombat(frameCounter, linkEntityX, linkEntityY, linkAirborne,
            linkInteractive, swordCollisionActive, swordX, swordWidth, swordY, swordHeight,
            EnemyAttackContext.standard());
    }

    public List<EntityCombatEvent> resolveEntityCombat(int frameCounter,
                                                       int linkEntityX,
                                                       int linkEntityY,
                                                       boolean linkAirborne,
                                                       boolean linkInteractive,
                                                       boolean swordCollisionActive,
                                                       int swordX,
                                                       int swordWidth,
                                                       int swordY,
                                                       int swordHeight,
                                                       EnemyAttackContext attackContext) {
        if (activeRoom == null || entityRuntime == null) {
            return List.of();
        }
        List<EntityCombatEvent> events = entityRuntime.resolveCombat(
            frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive,
            swordCollisionActive, swordX, swordWidth, swordY, swordHeight, attackContext);
        activeRoom.replaceEntities(entityRuntime.snapshot());
        return events;
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
            } else if (selection.roomTable() == EntityRoomLoader.RoomTable.COLOR_DUNGEON) {
                gpu.loadColorDungeonEntitySheets(romData, selection.roomId());
                entities = entities.withSpriteTiles(gpu.snapshotEntityTiles());
            }
        }
        activeRoom = ActiveRoom.from(room, entities);
        entityRuntime = entities == null ? null : RoomEntityRuntime.from(
            entities, activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD,
            entityRandomByteSource, entitySpriteHandlerCatalog, enemyCombatTables);
        if (entityRuntime != null) {
            entityRuntime.setColorShellWorld(colorShellWorld);
            entityRuntime.setFollowingNpcState(followingNpcState);
            entityRuntime.setGroundInteraction(this::entityGroundInteraction);
        }
        followingNpcRoomNeedsSync = true;
        synchronizeFollowingNpcEntitiesIfNeeded();
        if (roomLoadListener != null) {
            roomLoadListener.roomLoaded(activeRoom);
        }
    }

    private void synchronizeFollowingNpcEntitiesIfNeeded() {
        if (!followingNpcRoomNeedsSync || activeRoom == null || entityRuntime == null
            || activeRoom.entities() == null) {
            return;
        }
        FollowingNpcRoomContext room = new FollowingNpcRoomContext(
            activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD,
            false,
            activeRoom.mapId(),
            activeRoom.roomId());
        FollowingNpcEntitySpawner.Result result = followingNpcEntitySpawner.synchronize(
            activeRoom.entities(), room, followingNpcState,
            followingLinkX, followingLinkY, followingLinkZ,
            followingEntityYOffset, followingLinkDirection, followingLinkPositionHistory);
        followingNpcState = result.state();
        followingNpcRoomNeedsSync = false;
        activeRoom.replaceEntities(result.snapshot());
        entityRuntime = RoomEntityRuntime.from(
            result.snapshot(), activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD,
            entityRandomByteSource, entitySpriteHandlerCatalog, enemyCombatTables);
        entityRuntime.setColorShellWorld(colorShellWorld);
        entityRuntime.setFollowingNpcState(followingNpcState);
        entityRuntime.setGroundInteraction(this::entityGroundInteraction);
    }

    private void clearTransientRoomState() {
        if (transientVfxSystem != null) {
            transientVfxSystem.clear();
        }
        if (droppableRupeeSystem != null) {
            droppableRupeeSystem.clear();
        }
    }

    private int colorShellObjectAt(RoomEntity entity, int relativeOffset) {
        if (activeRoom == null || entity == null) {
            return 0xFF;
        }
        int index = colorShellObjectIndex(entity) + relativeOffset;
        int[] objects = activeRoom.roomObjectsArea();
        return index >= 0 && index < objects.length ? objects[index] : 0xFF;
    }

    private void writeColorShellObject(RoomEntity entity, int objectId) {
        if (activeRoom == null || entity == null) {
            return;
        }
        int index = colorShellObjectIndex(entity);
        int[] objects = activeRoom.roomObjectsArea();
        if (index < 0 || index >= objects.length) {
            return;
        }
        objects[index] = objectId & 0xFF;
        refreshActiveRoomTilemap();
    }

    private void refreshActiveRoomTilemap() {
        RoomTilemap tilemap = activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            ? roomTilemapBuilder.buildOverworld(activeRoom.roomId(), activeRoom.roomObjectsArea())
            : roomTilemapBuilder.buildIndoor(activeRoom.mapId(), activeRoom.roomId(),
                activeRoom.roomObjectsArea());
        activeRoom.replaceTilemap(tilemap.tileIds(), tilemap.tileAttrs());
    }

    private static int colorShellObjectIndex(RoomEntity entity) {
        int x = (entity.x() - 0x01) & 0xFF;
        int y = (entity.y() - 0x07) & 0xFF;
        return RoomConstants.ROOM_OBJECTS_BASE + (y & 0xF0) + ((x & 0xF0) >>> 4);
    }

    /**
     * Mirrors the ordinary entity collision point used by bank $03's
     * ApplyEntityCollisionWithObject for normal collision boxes. The shared
     * room physics query supplies the active overworld/indoor table.
     */
    private boolean entityBackgroundCollision(RoomEntity entity, int direction,
                                              int nextX, int nextY) {
        boolean spark = entity.type() == 0x16 || entity.type() == 0x17;
        int pointX = switch (direction) {
            case 0 -> spark ? nextX + 8 : nextX + 5;
            case 1 -> spark ? nextX - 9 : nextX - 6;
            default -> nextX;     // vertical movement: x - 8 + 8
        };
        int pointY = switch (direction) {
            case 2 -> spark ? nextY - 17 : nextY - 14;
            case 3 -> spark ? nextY : nextY - 3;
            default -> nextY - 8; // horizontal movement: y - 16 + 8
        };
        if (entity.type() == ENTITY_WATER_TEKTITE) {
            int physicsFlag = overworldCollision.objectPhysicsFlagAtPoint(pointX, pointY);
            if (physicsFlag == PhysicsFlags.SHALLOW_WATER
                || physicsFlag == PhysicsFlags.DEEP_WATER) {
                return false;
            }
        }
        return overworldCollision.pointBlocked(pointX, pointY);
    }

    /**
     * Mirrors the conveyor portion of bank-$03's
     * ApplyEntityInteractionWithBackground. Terrain physics and the entity
     * options byte stay ROM-backed; the runtime invokes this after the
     * entity-family handler has completed its movement for the frame.
     */
    private RoomEntity entityGroundInteraction(RoomEntity entity, int frameCounter) {
        int options = romTables.entityOptions1(entity.type());
        if ((options & ENTITY_OPT1_NO_GROUND_INTERACTION) != 0) {
            return entity;
        }

        // The ROM skips ground interaction while an entity is moving upward;
        // a negative Z byte denotes a falling entity and continues through
        // the shared helper.
        int z = entity.z() & 0xFF;
        if (z != 0 && (z & 0x80) == 0) {
            return entity;
        }
        if ((frameCounter & 0x03) != 0) {
            return entity;
        }

        int physicsFlag = overworldCollision.objectPhysicsFlagAtGroundInteraction(
            entity.x(), entity.y());
        int movementIndex = physicsFlag - PhysicsFlags.CAT_CONVEYOR;
        if (movementIndex < 0 || movementIndex >= ENTITY_CONVEYOR_MOVEMENT_X.length) {
            return entity;
        }

        return withEntityPosition(entity,
            entity.x() + ENTITY_CONVEYOR_MOVEMENT_X[movementIndex],
            entity.y() + ENTITY_CONVEYOR_MOVEMENT_Y[movementIndex]);
    }

    private static RoomEntity withEntityPosition(RoomEntity entity, int x, int y) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            entity.z());
    }

    private boolean pairoddProjectileObjectCollision(RoomEntity entity) {
        int physicsFlag = overworldCollision.objectPhysicsFlagAtEntityPosition(
            entity.x(), entity.y());
        return PairoddProjectileObjectCollision.collidesWithPhysicsFlag(physicsFlag);
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
