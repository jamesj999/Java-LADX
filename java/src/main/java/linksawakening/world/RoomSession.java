package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomBank;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static linksawakening.world.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_WIDTH;

public final class RoomSession {
    private static final int ROOM_STATUS_TABLE_SIZE = 0x100;
    private static final int COLOR_DUNGEON_SAVE_STATUS_SIZE = 0x20;
    private static final int W_TILESET_NO_UPDATE = 0xFF;
    private static final int ENTITY_OPT1_NO_GROUND_INTERACTION = 0x10;
    private static final int ENTITY_OPT1_SPLASH_IN_WATER = 0x08;
    private static final int ENTITY_FISH = 0xCC;
    private static final int ENTITY_PEAHAT = 0xA0;
    private static final int ENTITY_ROOSTER = 0xD5;
    private static final int ENTITY_BOW_WOW = 0x6D;
    private static final int ENTITY_MARIN_AT_THE_SHORE = 0xC1;
    private static final int ENTITY_HEART_CONTAINER = 0x36;
    private static final int OBJECT_ROCKY_GROUND = 0x09;
    private static final int OBJECT_ROCKY_CAVE_DOOR = 0xE1;
    private static final int OBJECT_BOMBABLE_CAVE_DOOR = 0xBA;
    private static final int OBJECT_GIANT_SKULL_TOP_LEFT = 0xBB;
    private static final int OBJECT_GIANT_SKULL_BOTTOM_RIGHT = 0xBE;
    private static final int OBJECT_BOMBABLE_BLOCK = 0xA9;
    private static final int OBJECT_FLOOR_OD = 0x0D;
    private static final int OW_ROOM_STATUS_OPENED = 0x04;
    private static final int ROOM_STATUS_CHEST_OPEN = 0x10;
    private static final int INDOOR_ROOM_STATUS_EVENT_3 = 0x40;
    private static final int OBJECT_BOMBED_PASSAGE_VERTICAL = 0x3D;
    private static final int OBJECT_BOMBED_PASSAGE_HORIZONTAL = 0x3E;
    private static final int BOMBABLE_WALL_PHYSICS_BASE = 0x99;
    private static final int BOMBED_CAVE_DOOR_TILES_BANK = 0x03;
    private static final int BOMBED_CAVE_DOOR_TILES_GBC_ADDR = 0x6751;
    private static final int INDOOR_MAP_LAYOUT_BANK = 0x14;
    private static final int INDOOR_MAP_LAYOUT_BASE_ADDR = 0x4220;
    private static final int COLOR_DUNGEON_MAP_LAYOUT_ADDR = 0x44E0;
    private static final int INDOOR_MAP_LAYOUT_SIZE = 0x40;
    static final int LINK_MOTION_FALLING_DOWN = 0x06;
    private static final int OBJECT_WELL = 0x61;
    private static final int OBJECT_WATER_LADDER_SIDESCROLL = 0x67;
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
    private final ChestContentsTable chestContentsTable;
    private final RomTables romTables;
    private final EnemyDropResolver enemyDropResolver;
    private final EntityCollisionPointProbe entityCollisionPointProbe;
    private final EntityBackgroundCollisionResolver entityBackgroundCollisionResolver;
    private final OverworldBushInteraction overworldBushInteraction;
    private final RoomEntityBackgroundInteraction entityBackgroundInteraction =
        new RoomEntityBackgroundInteraction() {
            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction,
                                                           int nextX, int nextY) {
                return entityBackgroundCollisionResult(entity, direction, nextX, nextY, 0);
            }

            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction,
                                                           int nextX, int nextY,
                                                           int ignoreHitsCountdown) {
                return entityBackgroundCollisionResult(
                    entity, direction, nextX, nextY, ignoreHitsCountdown);
            }

            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction,
                                                           int nextX, int nextY,
                                                           int ignoreHitsCountdown,
                                                           int frameCounter) {
                return entityBackgroundCollisionResult(
                    entity, direction, nextX, nextY, ignoreHitsCountdown, frameCounter);
            }
        };
    private final LinkPositionHistory followingLinkPositionHistory = new LinkPositionHistory();

    private ActiveRoom activeRoom;
    private RoomEntityRuntime entityRuntime;
    private final List<BombExplosionEvent> pendingBombExplosionEvents = new ArrayList<>();
    private final List<EntityCombatEvent> pendingRoomEntityEvents = new ArrayList<>();
    private final List<RoomEntityRuntime.ChestRewardEvent> pendingChestRewardEvents =
        new ArrayList<>();
    private final byte[] overworldRoomStatus = new byte[0x100];
    private final byte[] indoorARoomStatus = new byte[0x100];
    private final byte[] indoorBRoomStatus = new byte[0x100];
    private final byte[] colorDungeonRoomStatus = new byte[0x100];
    private final DungeonItemState dungeonItemState = new DungeonItemState();
    private final Map<Integer, RoomEntityRuntime.HookshotBridgeUpdate>
        hookshotBridgeTileOverrides = new HashMap<>();
    /** Immediate bomb-wall draw commands use a different tile order than the room object table. */
    private final Map<Integer, Integer> bombedWallTileOverrides = new HashMap<>();
    /** The basic A9 breakable path also draws floor tiles in source command order. */
    private final Set<Integer> bombedBlockTileOverrides = new HashSet<>();
    /** Bombed overworld cave doors use the dedicated GBC redraw tile order. */
    private final Set<Integer> bombedCaveDoorTileOverrides = new HashSet<>();
    private final int[] clearedEntitiesByRoom = new int[0x100];
    private int currentOverworldTilesetId = W_TILESET_NO_UPDATE;
    private FollowingNpcState followingNpcState = FollowingNpcState.none();
    private EnemyDropResolver.CounterState enemyDropCounters =
        new EnemyDropResolver.CounterState(0, 0);
    private int enemyDropMaxHearts = 3;
    private int enemyDropHealth = 6;
    private boolean enemyDropActivePowerUp;
    /** WRAM wSwitchBlocksState; the source reset value is zero. */
    private int switchBlocksState;
    /** WRAM wSwitchableObjectAnimationStage; zero means no update is active. */
    private int switchableObjectAnimationStage;
    private int followingLinkX = 0x08;
    private int followingLinkY = 0x10;
    private int followingLinkZ;
    private int followingEntityYOffset;
    private int followingLinkDirection;
    private boolean followingNpcRoomNeedsSync;
    private boolean actionButtonsHeld;
    private boolean powerBraceletButtonHeld;
    private boolean bombButtonHeld;
    private int ocarinaPlaybackCountdown;
    private int ocarinaSongFlags;
    private int selectedSongIndex;
    private int currentLinkMotionState = EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE;
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
        this.chestContentsTable = new ChestContentsTable(romData);
        this.romTables = RomTables.loadFromRom(romData);
        this.entityBackgroundCollisionResolver = new EntityBackgroundCollisionResolver(romTables);
        this.enemyDropResolver = new EnemyDropResolver(romData);
        this.entityCollisionPointProbe = new EntityCollisionPointProbe(romTables);
        this.overworldBushInteraction = new OverworldBushInteraction(romData, romTables);
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
        LoadedRoom room = roomLoader.loadOverworld(
            roomId, clearedEntitiesByRoom[roomId], overworldRoomStatus);
        gpu.loadAnimatedTilesGroup(romData, room.animatedTilesGroup());
        setActiveRoom(room);
        overworldCollision.setRoom(activeRoom.roomObjectsArea());
        overworldCollision.setGbcOverlay(activeRoom.gbcOverlay());
        overworldCollision.setPhysicsTable(RomTables.PHYSICS_TABLE_OVERWORLD);
        synchronizeSwitchBlockCollisionState();
    }

    public void loadIndoor(int mapId, int roomId) {
        loadIndoor(mapId, roomId, Warp.CATEGORY_INDOOR);
    }

    public void loadIndoor(int mapId, int roomId, int mapCategory) {
        clearTransientRoomState();
        dungeonItemState.loadForMap(mapId, true);
        gpu.loadIndoorTiles(romData, mapId, roomId);
        initializeSwitchBlockTiles();
        LoadedRoom room = roomLoader.loadIndoor(
            mapId, roomId, activeRoom == null ? null : activeRoom.palettes(), mapCategory,
            clearedEntitiesByRoom[roomId], indoorStatusTableForMap(mapId));
        gpu.loadAnimatedTilesGroup(romData, room.animatedTilesGroup());
        setActiveRoom(room);
        overworldCollision.setRoom(activeRoom.roomObjectsArea());
        overworldCollision.setGbcOverlay(null);
        overworldCollision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
        synchronizeSwitchBlockCollisionState();
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

    /**
     * Returns the source {@code wIndoorRoom} value for the active indoor map.
     * A negative result means this map has no ROM layout table, so the caller
     * should preserve the existing save byte rather than inventing one.
     */
    public int indoorRoomPositionForSave() {
        if (activeRoom == null || activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            return -1;
        }
        return indoorMapPosition(activeRoom.mapId(), activeRoom.roomId());
    }

    public byte[] overworldRoomStatusSnapshot() {
        return overworldRoomStatus.clone();
    }

    public byte[] indoorARoomStatusSnapshot() {
        return indoorARoomStatus.clone();
    }

    public byte[] indoorBRoomStatusSnapshot() {
        return indoorBRoomStatus.clone();
    }

    public byte[] colorDungeonRoomStatusSnapshot() {
        return Arrays.copyOf(colorDungeonRoomStatus, COLOR_DUNGEON_SAVE_STATUS_SIZE);
    }

    /** Restores the room-status WRAM tables loaded by the source save path. */
    public void restoreRoomStatuses(byte[] overworldStatus, byte[] indoorAStatus,
                                    byte[] indoorBStatus, byte[] colorDungeonStatus) {
        requireLength(overworldStatus, ROOM_STATUS_TABLE_SIZE, "overworldStatus");
        requireLength(indoorAStatus, ROOM_STATUS_TABLE_SIZE, "indoorAStatus");
        requireLength(indoorBStatus, ROOM_STATUS_TABLE_SIZE, "indoorBStatus");
        requireLength(colorDungeonStatus, COLOR_DUNGEON_SAVE_STATUS_SIZE,
            "colorDungeonStatus");
        System.arraycopy(overworldStatus, 0, overworldRoomStatus, 0, ROOM_STATUS_TABLE_SIZE);
        System.arraycopy(indoorAStatus, 0, indoorARoomStatus, 0, ROOM_STATUS_TABLE_SIZE);
        System.arraycopy(indoorBStatus, 0, indoorBRoomStatus, 0, ROOM_STATUS_TABLE_SIZE);
        Arrays.fill(colorDungeonRoomStatus, (byte) 0);
        System.arraycopy(colorDungeonStatus, 0, colorDungeonRoomStatus, 0,
            COLOR_DUNGEON_SAVE_STATUS_SIZE);
    }

    public byte[] dungeonItemFlagsSnapshot() {
        return dungeonItemState.dungeonItemFlagsSnapshot();
    }

    public byte[] colorDungeonItemFlagsSnapshot() {
        return dungeonItemState.colorDungeonItemFlagsSnapshot();
    }

    public byte[] currentDungeonItemFlagsSnapshot() {
        return dungeonItemState.currentFlagsSnapshot();
    }

    public void restoreDungeonItemFlags(byte[] dungeonFlags, byte[] colorFlags) {
        dungeonItemState.restore(dungeonFlags, colorFlags);
    }

    int overworldRoomStatusForTest(int roomId) {
        if (roomId < 0 || roomId >= overworldRoomStatus.length) {
            throw new IllegalArgumentException("Room id out of range: " + roomId);
        }
        return Byte.toUnsignedInt(overworldRoomStatus[roomId]);
    }

    int indoorRoomStatusForTest(int mapId, int roomId) {
        if (roomId < 0 || roomId >= 0x100) {
            throw new IllegalArgumentException("Room id out of range: " + roomId);
        }
        return Byte.toUnsignedInt(indoorStatusTableForMap(mapId)[roomId]);
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

    /** Supplies the held J_A|J_B state consumed by input-driven entity handlers. */
    public void setEntityActionButtonsHeld(boolean actionButtonsHeld) {
        this.actionButtonsHeld = actionButtonsHeld;
        if (entityRuntime != null) {
            entityRuntime.setActionButtonsHeld(actionButtonsHeld);
        }
    }

    /** Supplies the B/A inventory bytes consumed by Like Like's handler. */
    public void setEntityInventorySlots(int itemA, int itemB) {
        if ((itemA & ~0xFF) != 0 || (itemB & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity inventory slots must be unsigned bytes");
        }
        if (entityRuntime != null) {
            entityRuntime.setLikeLikeLinkInventory(itemA, itemB);
        }
    }

    /** Supplies the upgrade bytes consumed by the live chest dialog handler. */
    public void setChestPlayerLevels(int shieldLevel, int swordLevel,
                                     int powerBraceletLevel) {
        if (entityRuntime != null) {
            entityRuntime.setChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel);
        }
    }

    /** Supplies the held A/B state used by EntityGetLiftedUp. */
    public void setEntityPowerBraceletButtonHeld(boolean buttonHeld) {
        this.powerBraceletButtonHeld = buttonHeld;
        if (entityRuntime != null) {
            entityRuntime.setPowerBraceletButtonHeld(buttonHeld);
        }
    }

    /** Supplies the held A/B state for the equipped-bomb lift/throw path. */
    public void setEntityBombButtonHeld(boolean buttonHeld) {
        this.bombButtonHeld = buttonHeld;
        if (entityRuntime != null) {
            entityRuntime.setBombButtonHeld(buttonHeld);
        }
    }

    /** Starts the source UseOcarina countdown consumed by active entities. */
    public boolean startOcarina(int countdown, int songFlags, int selectedSong) {
        if (countdown < 0 || countdown > 0xFF) {
            throw new IllegalArgumentException("Ocarina countdown must be an unsigned byte: "
                + countdown);
        }
        if (songFlags < 0 || songFlags > 0xFF) {
            throw new IllegalArgumentException("Ocarina song flags must be an unsigned byte: "
                + songFlags);
        }
        if (selectedSong < 0 || selectedSong > 0xFF) {
            throw new IllegalArgumentException("Selected Ocarina song must be an unsigned byte: "
                + selectedSong);
        }
        if (activeRoom == null || entityRuntime == null || ocarinaPlaybackCountdown != 0) {
            return false;
        }
        ocarinaPlaybackCountdown = countdown;
        ocarinaSongFlags = songFlags & 0xFF;
        selectedSongIndex = selectedSong & 0xFF;
        return true;
    }

    /** Mirrors wLinkPlayingOcarinaCountdown for Link's motion gate. */
    public boolean ocarinaPlaying() {
        return ocarinaPlaybackCountdown != 0;
    }

    /** Supplies the player fields consumed by the ROM enemy-drop resolver. */
    public void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp) {
        if (maxHearts < 0 || maxHearts > 0xFF) {
            throw new IllegalArgumentException("Maximum hearts must be an unsigned byte: "
                + maxHearts);
        }
        if (health < 0 || health > 0xFF) {
            throw new IllegalArgumentException("Health must be an unsigned byte: " + health);
        }
        enemyDropMaxHearts = maxHearts;
        enemyDropHealth = health;
        enemyDropActivePowerUp = activePowerUp;
        configureEnemyDropRuntime();
    }

    public RoomEntityRuntime.LiftedEntityState liftedEntityState() {
        return entityRuntime == null
            ? RoomEntityRuntime.LiftedEntityState.none()
            : entityRuntime.liftedEntityState();
    }

    /** Throws the currently held object using Link's Java direction order. */
    public boolean throwLiftedEntity(int linkDirection) {
        if (entityRuntime == null) {
            return false;
        }
        boolean thrown = entityRuntime.throwLiftedEntity(
            romDirectionForProjectileCollision(linkDirection));
        if (thrown) {
            activeRoom.replaceEntities(entityRuntime.snapshot());
        }
        return thrown;
    }

    /**
     * Mirrors {@code FireHookshot}: the item path supplies Link's current ROM
     * coordinates and the room owns the player-projectile entity slot.
     */
    public boolean fireHookshot(int linkEntityX, int linkEntityY, int linkEntityZ,
                                int romDirection, boolean linkAirborne,
                                boolean linkPushing) {
        if (linkAirborne || linkPushing || activeRoom == null || entityRuntime == null) {
            return false;
        }
        int slot = entityRuntime.spawnHookshotChain(linkEntityX, linkEntityY, linkEntityZ,
            romDirection);
        if (slot < 0) {
            return false;
        }
        activeRoom.replaceEntities(entityRuntime.snapshot());
        return true;
    }

    /** Mirrors UseBoomerang's player-projectile spawn bridge. */
    public boolean fireBoomerang(int linkEntityX, int linkEntityY, int linkEntityZ,
                                 int romDirection, int pressedButtonsMask) {
        if (activeRoom == null || entityRuntime == null) {
            return false;
        }
        int slot = entityRuntime.spawnBoomerang(linkEntityX, linkEntityY, linkEntityZ,
            romDirection, pressedButtonsMask);
        if (slot < 0) {
            return false;
        }
        activeRoom.replaceEntities(entityRuntime.snapshot());
        return true;
    }

    /** Mirrors the level-two branch of UseSword's player-projectile spawn. */
    public boolean fireSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ,
                                 int romDirection) {
        if (activeRoom == null || entityRuntime == null) {
            return false;
        }
        int slot = entityRuntime.spawnSwordBeam(linkEntityX, linkEntityY, linkEntityZ,
            romDirection);
        if (slot < 0) {
            return false;
        }
        activeRoom.replaceEntities(entityRuntime.snapshot());
        return true;
    }

    /** Mirrors the delayed UseMagicRod player-projectile spawn bridge. */
    public boolean fireMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ,
                                        int romDirection) {
        if (activeRoom == null || entityRuntime == null) {
            return false;
        }
        int slot = entityRuntime.spawnMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ,
            romDirection);
        if (slot < 0) {
            return false;
        }
        activeRoom.replaceEntities(entityRuntime.snapshot());
        return true;
    }

    /** Mirrors the delayed UseMagicPowder sprinkle-entity spawn bridge. */
    public boolean sprinkleMagicPowder(int linkEntityX, int linkEntityY, int linkEntityZ,
                                       int romDirection) {
        if (activeRoom == null || entityRuntime == null) {
            return false;
        }
        int slot = entityRuntime.spawnMagicPowderSprinkle(
            linkEntityX, linkEntityY, linkEntityZ, romDirection);
        if (slot < 0) {
            return false;
        }
        activeRoom.replaceEntities(entityRuntime.snapshot());
        return true;
    }

    /**
     * Mirrors the ordinary player-bomb placement bridge. The item has already
     * applied PlaceBomb's inventory ordering; this method owns the room/runtime
     * allocation and immutable snapshot refresh.
     */
    public boolean placeBomb(int linkEntityX, int linkEntityY, int linkEntityZ,
                             int romDirection) {
        if (activeRoom == null || entityRuntime == null) {
            return false;
        }
        int slot = entityRuntime.spawnBomb(linkEntityX, linkEntityY, linkEntityZ, romDirection);
        if (slot < 0) {
            return false;
        }
        activeRoom.replaceEntities(entityRuntime.snapshot());
        return true;
    }

    /** Returns whether the last successful bomb placement queued the top-view bump jingle. */
    public boolean lastBombPlacementPlayedBump() {
        return entityRuntime != null && entityRuntime.lastBombPlacementPlayedBump();
    }

    /** Result of the ROM ShootArrow bridge, including its conditional whoosh. */
    public record ArrowShotResult(boolean spawned, boolean playWhoosh) {
    }

    /** Mirrors ShootArrow's player-projectile bridge and refreshes the room snapshot. */
    public boolean shootArrow(int linkEntityX, int linkEntityY, int linkEntityZ,
                              int romDirection) {
        return shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection, false)
            .spawned();
    }

    public ArrowShotResult shootArrowResult(int linkEntityX, int linkEntityY, int linkEntityZ,
                                            int romDirection) {
        return shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    /** ShootArrow's label_140F speed-table selector for Piece of Power. */
    public ArrowShotResult shootArrowResult(int linkEntityX, int linkEntityY, int linkEntityZ,
                                            int romDirection, boolean pieceOfPower) {
        if (activeRoom == null || entityRuntime == null) {
            return new ArrowShotResult(false, false);
        }
        int slot = entityRuntime.spawnArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection,
            pieceOfPower);
        if (slot < 0) {
            return new ArrowShotResult(false, false);
        }
        boolean playWhoosh = entityRuntime.lastArrowShotPlayedWhoosh();
        activeRoom.replaceEntities(entityRuntime.snapshot());
        return new ArrowShotResult(true, playWhoosh);
    }

    /** Returns the modeled player-projectile count used by the item gates. */
    public int activeProjectileCount() {
        return entityRuntime == null ? 0 : entityRuntime.activeProjectileCount();
    }

    /** Returns whether the live room currently owns an ordinary bomb entity. */
    public boolean bombActive() {
        return entityRuntime != null && entityRuntime.bombActive();
    }

    int bombDirectionForTest(int slot) {
        return entityRuntime == null ? 0xFF : entityRuntime.bombDirection(slot);
    }

    /** Returns whether the live room currently owns entity {@code $03}. */
    public boolean hookshotActive() {
        return entityRuntime != null && entityRuntime.hookshotActive();
    }

    /** Returns whether the live room currently owns the boomerang entity. */
    public boolean boomerangActive() {
        return entityRuntime != null && entityRuntime.boomerangActive();
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

    int entityGroundStatusForTest(int slot) {
        if (entityRuntime == null) {
            return 0;
        }
        return entityRuntime.groundStatus(slot);
    }

    void setEntityIgnoreHitsCountdownForTest(int slot, int value) {
        if (entityRuntime != null) {
            entityRuntime.setEnemyIgnoreHitsCountdownForTest(slot, value);
        }
    }

    void setEntityThrownDirectionForTest(int slot, int value) {
        if (entityRuntime != null) {
            entityRuntime.setThrownDirection(slot, value);
        }
    }

    void setEntityLedgeTimerForTest(int slot, int value) {
        if (entityRuntime != null) {
            entityRuntime.setLedgeTransitionTimer(slot, value);
        }
    }

    void setEntitySwitchBlocksStateForTest(int value) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(
                "Switch-block state must be an unsigned byte: " + value);
        }
        switchBlocksState = value;
        synchronizeSwitchBlockCollisionState();
    }

    void setSwitchableObjectAnimationStageForTest(int value) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(
                "Switch-block animation stage must be an unsigned byte: " + value);
        }
        switchableObjectAnimationStage = value;
    }

    int entitySwitchBlocksStateForTest() {
        return switchBlocksState & 0xFF;
    }

    int switchableObjectAnimationStageForTest() {
        return switchableObjectAnimationStage & 0xFF;
    }

    boolean linkCollisionPointBlockedForTest(int pixelX, int pixelY) {
        return overworldCollision.pointBlocked(pixelX, pixelY);
    }

    int entityLedgeTimerForTest(int slot) {
        return entityRuntime == null ? 0 : entityRuntime.ledgeTransitionTimer(slot);
    }

    int entityFallingTargetXForTest(int slot) {
        return entityRuntime == null ? 0 : entityRuntime.fallingTargetX(slot);
    }

    int entityTransitionCountdownForTest(int slot) {
        return entityRuntime == null ? 0 : entityRuntime.transitionCountdown(slot);
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
        return tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY,
            linkEntityZ, linkMotionState, linkDirection, 0, usingShield, shieldLevel,
            invincibilityCounter, swordCollisionActive, swordX, swordWidth, swordY,
            swordHeight);
    }

    /** Advances entities with Link's current ROM collision byte. */
    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
                             int frameCounter, int linkEntityX, int linkEntityY,
                             int linkEntityZ, int linkMotionState, int linkDirection,
                             int collisionType, boolean usingShield, int shieldLevel,
                             int invincibilityCounter, boolean swordCollisionActive,
                             int swordX, int swordWidth, int swordY, int swordHeight) {
        return tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY,
            linkEntityZ, linkMotionState, linkDirection, collisionType, usingShield,
            shieldLevel, invincibilityCounter, swordCollisionActive, swordX, swordWidth,
            swordY, swordHeight, 0, 0);
    }

    /** Advances entities with the current Link speed bytes for enemy-bomb recoil. */
    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
                             int frameCounter, int linkEntityX, int linkEntityY,
                             int linkEntityZ, int linkMotionState, int linkDirection,
                             int collisionType, boolean usingShield, int shieldLevel,
                             int invincibilityCounter, boolean swordCollisionActive,
                             int swordX, int swordWidth, int swordY, int swordHeight,
                             int linkSpeedX, int linkSpeedY) {
        followingLinkX = linkEntityX & 0xFF;
        followingLinkY = linkEntityY & 0xFF;
        followingLinkZ = linkEntityZ & 0xFF;
        followingLinkDirection = linkDirection & 0xFF;
        currentLinkMotionState = linkMotionState & 0xFF;
        if (activeRoom == null || entityRuntime == null) {
            return List.of();
        }
        entityRuntime.setSwitchBlockAnimationActive(
            SwitchBlockAnimation.isAnimating(switchableObjectAnimationStage));
        entityRuntime.setActionButtonsHeld(actionButtonsHeld);
        entityRuntime.setPowerBraceletButtonHeld(powerBraceletButtonHeld);
        entityRuntime.setBombButtonHeld(bombButtonHeld);
        entityRuntime.setLiftedLinkC13B(followingEntityYOffset);
        entityRuntime.setOcarinaPlayback(ocarinaPlaybackCountdown,
            ocarinaSongFlags, selectedSongIndex);
        // rLY is not a meaningful value in the host renderer. Keep the
        // non-emulator policy explicit while preserving the ROM seed update.
        entityRandomByteSource.beginFrame(frameCounter & 0xFF, 0);
        List<EntityProjectileEvent> events = entityRuntime.tickWithProjectileEvents(
            frameCounter, linkEntityX, linkEntityY, collisionType & 0xFF,
            entityRandomByteSource,
            this::entityBackgroundCollision, this::pairoddProjectileObjectCollision,
            followingLinkPositionHistory, followingLinkZ,
            followingLinkDirection, followingEntityYOffset,
            new EnemyProjectileCollision.LinkState(
                linkEntityX, linkEntityY, linkEntityZ, linkMotionState,
                romDirectionForProjectileCollision(linkDirection), usingShield, shieldLevel,
                invincibilityCounter), swordCollisionActive, swordX, swordWidth,
            swordY, swordHeight, linkSpeedX, linkSpeedY);
        if (ocarinaPlaybackCountdown > 0) {
            ocarinaPlaybackCountdown--;
        }
        List<BombExplosionEvent> bombExplosionEvents = entityRuntime.consumeBombExplosionEvents();
        pendingBombExplosionEvents.clear();
        pendingBombExplosionEvents.addAll(bombExplosionEvents);
        List<RoomEntityRuntime.ChestRewardEvent> chestRewards =
            entityRuntime.consumePendingChestRewardEvents();
        for (RoomEntityRuntime.ChestRewardEvent reward : chestRewards) {
            if (reward.itemType() >= ChestContentsTable.CHEST_MAP
                && reward.itemType() <= ChestContentsTable.CHEST_SMALL_KEY) {
                dungeonItemState.incrementCurrentFlag(reward.itemType());
            }
        }
        pendingChestRewardEvents.addAll(chestRewards);
        if (entityRuntime.consumePendingSwitchBlockAnimationRequest()
            && switchableObjectAnimationStage == 0) {
            switchableObjectAnimationStage = 0x01;
        }
        enemyDropCounters = entityRuntime.enemyDropCounters();
        if (transientVfxSystem != null) {
            for (RoomEntityRuntime.TransientVfxRequest request
                : entityRuntime.transientVfxRequests()) {
                transientVfxSystem.spawn(request.type(), request.worldX(), request.worldY(),
                    request.variant());
            }
        }
        int clearedMask = entityRuntime.consumePendingClearedEntityMask();
        if (clearedMask != 0) {
            clearedEntitiesByRoom[activeRoom.roomId()] |= clearedMask;
        }
        applyHookshotBridgeUpdates(entityRuntime.hookshotBridgeUpdates());
        activeRoom.replaceEntities(entityRuntime.snapshot());
        applyBombObjectInteractions(bombExplosionEvents);
        applyBoomerangObjectInteractions(entityRuntime.boomerangObjectRequests());
        applyMagicRodObjectInteractions(entityRuntime.magicRodObjectRequests());
        applyMagicPowderObjectInteractions(entityRuntime.magicPowderObjectRequests());
        activeRoom.replaceEntities(entityRuntime.snapshot());
        return events;
    }

    /** Returns and clears entity side-effect events emitted by the last tick. */
    public List<EntityCombatEvent> consumeEntityEvents() {
        List<EntityCombatEvent> events = new ArrayList<>();
        if (entityRuntime != null) {
            events.addAll(entityRuntime.consumePendingEntityEvents());
        }
        events.addAll(pendingRoomEntityEvents);
        pendingRoomEntityEvents.clear();
        return List.copyOf(events);
    }

    /** Returns and clears chest reward applications emitted by the last entity tick(s). */
    public List<RoomEntityRuntime.ChestRewardEvent> consumeChestRewardEvents() {
        List<RoomEntityRuntime.ChestRewardEvent> rewards = List.copyOf(pendingChestRewardEvents);
        pendingChestRewardEvents.clear();
        return rewards;
    }

    /** Returns the raw music-track request emitted by a chest, or {@code -1}. */
    public int consumePendingMusicTrack() {
        return entityRuntime == null ? -1 : entityRuntime.consumePendingMusicTrack();
    }

    /** Returns and clears Like Like capture/release requests from the last tick. */
    public List<RoomEntityRuntime.LikeLikeEvent> consumeLikeLikeEvents() {
        if (entityRuntime == null) {
            return List.of();
        }
        return entityRuntime.consumePendingLikeLikeEvents();
    }

    /** Returns and clears ROM dialog requests emitted by the last entity tick. */
    public List<RoomEntityRuntime.DialogRequest> consumeEntityDialogRequests() {
        if (entityRuntime == null) {
            return List.of();
        }
        return entityRuntime.consumePendingDialogRequests();
    }

    /** Returns and clears source-shaped bomb-explosion interaction requests from the last tick. */
    public List<BombExplosionEvent> consumeBombExplosionEvents() {
        List<BombExplosionEvent> pending = List.copyOf(pendingBombExplosionEvents);
        pendingBombExplosionEvents.clear();
        return pending;
    }

    /** Advances the ROM's VBlank tile path, including switch blocks. */
    public void tickGameplayVBlank() {
        if (SwitchBlockAnimation.isAnimating(switchableObjectAnimationStage)) {
            SwitchBlockAnimation.Step step = SwitchBlockAnimation.advance(
                switchableObjectAnimationStage, switchBlocksState);
            switchBlocksState = step.switchBlocksState();
            switchableObjectAnimationStage = step.nextStage();
            synchronizeSwitchBlockCollisionState();
            if (step.tileCopy() != null) {
                gpu.copySwitchBlockTiles(romData,
                    step.tileCopy().sourceOffset(), step.tileCopy().destinationTile());
            }
            // AnimateTiles returns after UpdateSwitchBlockTiles while the
            // switch animation is active, so ordinary animated BG tiles wait
            // until the next idle VBlank.
            return;
        }
        gpu.tickAnimatedTiles(romData);
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
            0, linkInteractive, swordCollisionActive, swordX, swordWidth, swordY, swordHeight,
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
        return resolveEntityCombat(frameCounter, linkEntityX, linkEntityY, linkAirborne,
            0, linkInteractive, swordCollisionActive, swordX, swordWidth, swordY, swordHeight,
            attackContext);
    }

    public List<EntityCombatEvent> resolveEntityCombat(int frameCounter,
                                                       int linkEntityX,
                                                       int linkEntityY,
                                                       boolean linkAirborne,
                                                       int linkVerticalVelocity,
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
            frameCounter, linkEntityX, linkEntityY, linkAirborne, linkVerticalVelocity,
            linkInteractive, swordCollisionActive, swordX, swordWidth, swordY, swordHeight,
            attackContext);
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
        return collectEntityIfNeeded(frameCounter, linkPixelX, linkPixelY, linkAirborne,
            linkInteractive, followingLinkDirection, followingLinkZ);
    }

    public EntityPickupEvent collectEntityIfNeeded(int frameCounter,
                                                   int linkPixelX,
                                                   int linkPixelY,
                                                   boolean linkAirborne,
                                                   boolean linkInteractive,
                                                   int linkDirection) {
        return collectEntityIfNeeded(frameCounter, linkPixelX, linkPixelY, linkAirborne,
            linkInteractive, linkDirection, followingLinkZ);
    }

    public EntityPickupEvent collectEntityIfNeeded(int frameCounter,
                                                   int linkPixelX,
                                                   int linkPixelY,
                                                   boolean linkAirborne,
                                                   boolean linkInteractive,
                                                   int linkDirection,
                                                   int linkZ) {
        if (activeRoom == null || entityRuntime == null) {
            return null;
        }
        followingLinkZ = linkZ & 0xFF;
        followingLinkDirection = linkDirection & 0xFF;
        EntityPickupEvent event = entityRuntime.collectIfNeeded(
            frameCounter, linkPixelX, linkPixelY, linkAirborne, linkInteractive,
            linkDirection, linkZ);
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
        if (entities != null) {
            entities = entities.withSideScrolling(room.mapCategory() == Warp.CATEGORY_SIDESCROLL);
        }
        activeRoom = ActiveRoom.from(room, entities);
        entityRuntime = entities == null ? null : RoomEntityRuntime.from(
            entities, activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD,
            entityRandomByteSource, entitySpriteHandlerCatalog, enemyCombatTables,
            chestContentsTable);
        if (entityRuntime != null) {
            entityRuntime.setColorShellWorld(colorShellWorld);
            entityRuntime.setFollowingNpcState(followingNpcState);
            entityRuntime.setEntityMapId(activeRoom.mapId());
            entityRuntime.setGroundInteraction(this::entityGroundInteraction);
            entityRuntime.setBackgroundInteraction(entityBackgroundInteraction);
            entityRuntime.setObjectQuery(this::entityObjectSample);
            entityRuntime.setObjectIntersectionQuery(this::entityObjectIntersectionSample);
            entityRuntime.setGroundInteractionSideScrolling(
                activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL);
            entityRuntime.setActionButtonsHeld(actionButtonsHeld);
            entityRuntime.setPowerBraceletButtonHeld(powerBraceletButtonHeld);
            entityRuntime.setBombButtonHeld(bombButtonHeld);
            entityRuntime.setLiftedLinkC13B(followingEntityYOffset);
            configureEnemyDropRuntime();
        }
        followingNpcRoomNeedsSync = true;
        synchronizeFollowingNpcEntitiesIfNeeded();
        if (roomLoadListener != null) {
            roomLoadListener.roomLoaded(activeRoom);
        }
    }

    private void initializeSwitchBlockTiles() {
        for (SwitchBlockAnimation.TileCopy copy
            : SwitchBlockAnimation.initialCopies(switchBlocksState)) {
            gpu.copySwitchBlockTiles(romData, copy.sourceOffset(), copy.destinationTile());
        }
    }

    private void synchronizeSwitchBlockCollisionState() {
        overworldCollision.setSwitchBlocksState(switchBlocksState);
    }

    private void synchronizeFollowingNpcEntitiesIfNeeded() {
        if (!followingNpcRoomNeedsSync || activeRoom == null || entityRuntime == null
            || activeRoom.entities() == null) {
            return;
        }
        FollowingNpcRoomContext room = new FollowingNpcRoomContext(
            activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD,
            activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL,
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
            entityRandomByteSource, entitySpriteHandlerCatalog, enemyCombatTables,
            chestContentsTable);
        entityRuntime.setColorShellWorld(colorShellWorld);
        entityRuntime.setFollowingNpcState(followingNpcState);
        entityRuntime.setEntityMapId(activeRoom.mapId());
        entityRuntime.setGroundInteraction(this::entityGroundInteraction);
        entityRuntime.setBackgroundInteraction(entityBackgroundInteraction);
        entityRuntime.setObjectQuery(this::entityObjectSample);
        entityRuntime.setObjectIntersectionQuery(this::entityObjectIntersectionSample);
        entityRuntime.setGroundInteractionSideScrolling(
            activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL);
        entityRuntime.setActionButtonsHeld(actionButtonsHeld);
        entityRuntime.setPowerBraceletButtonHeld(powerBraceletButtonHeld);
        entityRuntime.setBombButtonHeld(bombButtonHeld);
        entityRuntime.setLiftedLinkC13B(followingEntityYOffset);
        configureEnemyDropRuntime();
    }

    private void configureEnemyDropRuntime() {
        if (entityRuntime == null) {
            return;
        }
        entityRuntime.setEnemyDropResolver(enemyDropResolver);
        entityRuntime.setEnemyDropCounters(enemyDropCounters);
        entityRuntime.setEnemyDropPlayerState(
            enemyDropMaxHearts, enemyDropHealth, enemyDropActivePowerUp);
        entityRuntime.setEnemyDropBossBattle(roomHasBossBattle());
    }

    private boolean roomHasBossBattle() {
        if (activeRoom == null || activeRoom.entities() == null) {
            return false;
        }
        for (RoomEntity entity : activeRoom.entities().loadedEntities()) {
            if ((romTables.entityOptions1(entity.type()) & 0x80) != 0) {
                return true;
            }
        }
        return false;
    }

    private void clearTransientRoomState() {
        if (transientVfxSystem != null) {
            transientVfxSystem.clear();
        }
        if (droppableRupeeSystem != null) {
            droppableRupeeSystem.clear();
        }
        hookshotBridgeTileOverrides.clear();
        bombedWallTileOverrides.clear();
        bombedBlockTileOverrides.clear();
        bombedCaveDoorTileOverrides.clear();
        pendingBombExplosionEvents.clear();
        pendingRoomEntityEvents.clear();
        pendingChestRewardEvents.clear();
    }

    private void applyBombObjectInteractions(List<BombExplosionEvent> events) {
        if (activeRoom == null || activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL
            || events == null || events.isEmpty()) {
            return;
        }

        for (BombExplosionEvent event : events) {
            if (!event.targetsRoomObjects()) {
                continue;
            }
            RoomEntity bomb = event.bombSlot() < activeRoom.entities().slots().size()
                ? activeRoom.entities().slots().get(event.bombSlot()) : null;
            if (activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                BombObjectInteraction.Candidate candidate = BombObjectInteraction.basicCandidate(
                    event.bombX(), event.bombVisualY(), event.countdown());
                applyBasicBombObjectCandidate(candidate);
                if (bomb != null && bomb.loaded()) {
                    applyPuzzleBombObjectCandidate(BombObjectInteraction.puzzleCandidate(
                        event.bombX(), bomb.y(), event.countdown()), event.bombSlot(), bomb.z());
                }
            } else if (bomb != null && bomb.loaded()) {
                BombObjectInteraction.Candidate candidate = BombObjectInteraction.puzzleCandidate(
                    event.bombX(), bomb.y(), event.countdown());
                if (!applyIndoorBombableBlockCandidate(candidate)) {
                    applyIndoorBombableWallCandidate(candidate, event.bombSlot());
                }
            }
        }
    }

    private void applyBasicBombObjectCandidate(BombObjectInteraction.Candidate candidate) {
        if (candidate == null) {
            return;
        }
        int location = candidate.location();
        OverworldBushInteraction.CutResult result =
            overworldBushInteraction.revealObjectAtLocation(
                location,
                activeRoom.roomId(),
                true,
                activeRoom.roomObjectsArea(),
                activeRoom.renderValues(),
                activeRoom.gbcOverlay(),
                activeRoom.tileIds(),
                activeRoom.tileAttrs());
        if (!result.changed()) {
            return;
        }

        if (result.bushLeavesVisible() && entityRuntime != null) {
            int sourceSpriteVariant = result.originalObjectId() == 0x0A ? 0xFF : 0x01;
            entityRuntime.spawnLiftableRockSmash(
                overworldBushInteraction.effectOriginXForLocation(location),
                overworldBushInteraction.effectOriginYForLocation(location),
                sourceSpriteVariant);
        }
        refreshOverworldCollisionAfterObjectMutation();
    }

    private void applyBoomerangObjectInteractions(
            List<RoomEntityRuntime.BoomerangObjectRequest> requests) {
        if (activeRoom == null || activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
            || requests == null || requests.isEmpty()) {
            return;
        }

        for (RoomEntityRuntime.BoomerangObjectRequest request : requests) {
            OverworldBushInteraction.CutResult result =
                overworldBushInteraction.revealObjectAtLocation(
                    request.location(), activeRoom.roomId(), true,
                    activeRoom.roomObjectsArea(), activeRoom.renderValues(),
                    activeRoom.gbcOverlay(), activeRoom.tileIds(), activeRoom.tileAttrs());
            if (!result.changed()) {
                continue;
            }
            if (transientVfxSystem != null) {
                transientVfxSystem.spawn(TransientVfxType.SMOKE,
                    request.objectLeft() + 0x08, request.objectTop() + 0x10);
            }
            pendingRoomEntityEvents.add(new EntityCombatEvent(
                request.sourceSlot(), 0x01, 0, false,
                EntityCombatEvent.SoundChannel.NOISE, 0x13));
            refreshOverworldCollisionAfterObjectMutation();
        }
    }

    private void applyMagicRodObjectInteractions(
            List<RoomEntityRuntime.MagicRodObjectRequest> requests) {
        if (activeRoom == null || requests == null || requests.isEmpty()) {
            return;
        }

        for (RoomEntityRuntime.MagicRodObjectRequest request : requests) {
            boolean changed = false;
            if (activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                OverworldBushInteraction.CutResult result =
                    overworldBushInteraction.revealObjectAtLocation(
                        request.location(), activeRoom.roomId(), true,
                        activeRoom.roomObjectsArea(), activeRoom.renderValues(),
                        activeRoom.gbcOverlay(), activeRoom.tileIds(), activeRoom.tileAttrs());
                changed = result.changed();
            } else if (activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL
                && objectAtRoomLocation(request.location()) == 0x8A) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
                    + (request.location() & 0xF0) + (request.location() & 0x0F);
                int[] objects = activeRoom.roomObjectsArea();
                if (areaIndex >= 0 && areaIndex < objects.length) {
                    objects[areaIndex] = 0x04; // OBJECT_SIDE_VIEW_EMPTY
                    if (activeRoom.renderValues() != null
                        && areaIndex < activeRoom.renderValues().length) {
                        activeRoom.renderValues()[areaIndex] = 0x04;
                    }
                    refreshActiveRoomTilemap();
                    overworldCollision.setRoom(activeRoom.roomObjectsArea());
                    overworldCollision.setGbcOverlay(null);
                    changed = true;
                }
            }

            if (!changed) {
                continue;
            }
            if (transientVfxSystem != null) {
                transientVfxSystem.spawn(TransientVfxType.SMOKE,
                    request.objectLeft() + 0x08, request.objectTop() + 0x10);
            }
            pendingRoomEntityEvents.add(new EntityCombatEvent(
                request.sourceSlot(), 0x04, 0, false,
                EntityCombatEvent.SoundChannel.NOISE, 0x13));
            if (activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                refreshOverworldCollisionAfterObjectMutation();
            }
        }
    }

    private void applyMagicPowderObjectInteractions(
            List<RoomEntityRuntime.MagicPowderObjectRequest> requests) {
        if (activeRoom == null || requests == null || requests.isEmpty()) {
            return;
        }

        for (RoomEntityRuntime.MagicPowderObjectRequest request : requests) {
            boolean changed = switch (request.action()) {
                case REVEAL -> activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                    && overworldBushInteraction.revealObjectAtLocation(
                        request.location(), activeRoom.roomId(), true,
                        activeRoom.roomObjectsArea(), activeRoom.renderValues(),
                        activeRoom.gbcOverlay(), activeRoom.tileIds(), activeRoom.tileAttrs())
                        .changed();
                case IGNITE_TORCH -> writeMagicPowderTorchObject(
                    request.location(), 0xAB, 0xAC);
                case EXTINGUISH_TORCH -> writeMagicPowderTorchObject(
                    request.location(), 0xAC, 0xAB);
            };

            if (!changed) {
                continue;
            }
            if (activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                refreshOverworldCollisionAfterObjectMutation();
            } else {
                overworldCollision.setRoom(activeRoom.roomObjectsArea());
                overworldCollision.setGbcOverlay(null);
            }
        }
    }

    private boolean writeMagicPowderTorchObject(int location, int expectedObject,
                                                 int replacementObject) {
        if (objectAtRoomLocation(location) != expectedObject) {
            return false;
        }
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0)
            + (location & 0x0F);
        int[] objects = activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return false;
        }
        objects[areaIndex] = replacementObject & 0xFF;
        if (activeRoom.renderValues() != null
            && areaIndex < activeRoom.renderValues().length) {
            activeRoom.renderValues()[areaIndex] = replacementObject & 0xFF;
        }
        refreshActiveRoomTilemap();
        return true;
    }

    private void applyPuzzleBombObjectCandidate(BombObjectInteraction.Candidate candidate,
                                                int sourceSlot, int sourceZ) {
        if (candidate == null) {
            return;
        }
        int objectId = objectAtRoomLocation(candidate.location());
        if (objectId < OBJECT_GIANT_SKULL_TOP_LEFT
            || objectId > OBJECT_GIANT_SKULL_BOTTOM_RIGHT) {
            applyOutdoorBombableCaveDoorCandidate(candidate, sourceSlot);
            return;
        }

        int blockTop = candidate.objectTop() & 0xE0;
        int blockLeft = candidate.objectLeft() & 0xE0;
        int[] rubbleXOffsets = {0x18, 0x08, 0x18, 0x08};
        int[] rubbleYOffsets = {0x20, 0x20, 0x10, 0x10};
        if (entityRuntime != null) {
            for (int index = 0; index < rubbleXOffsets.length; index++) {
                entityRuntime.spawnLiftableRockRubble(
                    blockLeft + rubbleXOffsets[index],
                    blockTop + rubbleYOffsets[index] - sourceZ);
            }
        }
        queuePuzzleSolvedJingle(sourceSlot);

        int blockLocation = blockTop | (blockLeft >>> 4);
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                writeBombPuzzleObject(blockLocation
                    + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column,
                    OBJECT_ROCKY_GROUND);
            }
        }
        overworldRoomStatus[activeRoom.roomId()] |= (byte) OW_ROOM_STATUS_OPENED;
        refreshOverworldCollisionAfterObjectMutation();
    }

    private void applyOutdoorBombableCaveDoorCandidate(BombObjectInteraction.Candidate candidate,
                                                       int sourceSlot) {
        int objectId = objectAtRoomLocation(candidate.location());
        int doorIndex = romTables.objectPhysicsFlag(RomTables.PHYSICS_TABLE_OVERWORLD, objectId)
            - BOMBABLE_WALL_PHYSICS_BASE;
        if (objectId != OBJECT_BOMBABLE_CAVE_DOOR || doorIndex < 0 || doorIndex >= 4) {
            return;
        }

        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (candidate.objectTop() & 0xF0)
            + ((candidate.objectLeft() & 0xF0) >>> 4);
        int[] objects = activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return;
        }

        writeBombPuzzleObject(candidate.location(), OBJECT_ROCKY_CAVE_DOOR);
        bombedCaveDoorTileOverrides.add(areaIndex);
        overworldRoomStatus[activeRoom.roomId()] |= (byte) OW_ROOM_STATUS_OPENED;
        queuePuzzleSolvedJingle(sourceSlot);
        refreshActiveRoomTilemap();
        refreshOverworldCollisionAfterObjectMutation();
    }

    private int objectAtRoomLocation(int location) {
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0)
            + (location & 0x0F);
        int[] objects = activeRoom.roomObjectsArea();
        return areaIndex < 0 || areaIndex >= objects.length ? 0xFF : objects[areaIndex] & 0xFF;
    }

    /** Result of the source closed-chest action bridge. */
    public record ChestOpenResult(boolean opened, int itemType, int entitySlot, int location) {
    }

    /**
     * Mirrors the closed-$A0 branch in the source object interaction path.
     * Link's front-cell calculation intentionally follows the existing
     * overworld-dialog bridge so both interaction paths address the same
     * padded room-object buffer.
     */
    public ChestOpenResult tryOpenChest(int linkPixelX, int linkPixelY, int linkDirection,
                                        boolean actionPressed, int swordLevel) {
        if (!actionPressed || linkDirection != Link.DIRECTION_UP
            || activeRoom == null || entityRuntime == null) {
            return new ChestOpenResult(false, -1, -1, -1);
        }
        if (linkPixelX < 0 || linkPixelY < 0
            || linkPixelX >= ROOM_PIXEL_WIDTH || linkPixelY >= ROOM_PIXEL_HEIGHT) {
            return new ChestOpenResult(false, -1, -1, -1);
        }
        int column = Math.floorDiv(linkPixelX + 0x08, 0x10);
        int row = Math.floorDiv(linkPixelY - 0x01, 0x10);
        if (column < 0 || column >= RoomConstants.OBJECTS_PER_ROW
            || row < 0 || row >= RoomConstants.OBJECTS_PER_COLUMN) {
            return new ChestOpenResult(false, -1, -1, -1);
        }
        int location = (row << 4) | column;
        if (objectAtRoomLocation(location) != 0xA0) {
            return new ChestOpenResult(false, -1, -1, location);
        }
        int itemType = chestContentsTable.itemForSpawn(
            activeRoom.mapId(), activeRoom.roomId(), swordLevel);
        int slot = entityRuntime.spawnChestWithItem(column << 4, row << 4, itemType);
        if (slot < 0) {
            return new ChestOpenResult(false, -1, -1, location);
        }

        writeBombPuzzleObject(location, 0xA1);
        byte[] status = activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            ? overworldRoomStatus : indoorStatusTableForMap(activeRoom.mapId());
        status[activeRoom.roomId()] |= (byte) ROOM_STATUS_CHEST_OPEN;
        refreshActiveRoomTilemap();
        overworldCollision.setRoom(activeRoom.roomObjectsArea());
        overworldCollision.setGbcOverlay(activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            ? activeRoom.gbcOverlay() : null);
        activeRoom.replaceEntities(entityRuntime.snapshot());
        return new ChestOpenResult(true, itemType, slot, location);
    }

    private void writeBombPuzzleObject(int location, int objectId) {
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0)
            + (location & 0x0F);
        int[] objects = activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return;
        }
        objects[areaIndex] = objectId & 0xFF;
        if (activeRoom.renderValues() != null) {
            activeRoom.renderValues()[areaIndex] = objectId & 0xFF;
        }
        overworldBushInteraction.refreshRoomObjectCell(
            activeRoom.roomId(), location, objects, activeRoom.renderValues(),
            activeRoom.gbcOverlay(), activeRoom.tileIds(), activeRoom.tileAttrs());
    }

    private void refreshOverworldCollisionAfterObjectMutation() {
        overworldCollision.setRoom(activeRoom.roomObjectsArea());
        overworldCollision.setGbcOverlay(activeRoom.gbcOverlay());
    }

    private void applyIndoorBombableWallCandidate(BombObjectInteraction.Candidate candidate,
                                                  int sourceSlot) {
        if (candidate == null) {
            return;
        }
        int location = candidate.location();
        int objectId = objectAtRoomLocation(location);
        int physicsTable = activeRoom.mapId() == 0xFF
            ? RomTables.PHYSICS_TABLE_INDOORS2 : RomTables.PHYSICS_TABLE_INDOORS1;
        int wallIndex = romTables.objectPhysicsFlag(physicsTable, objectId)
            - BOMBABLE_WALL_PHYSICS_BASE;
        if (wallIndex < 0 || wallIndex >= 4) {
            return;
        }

        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0)
            + (location & 0x0F);
        int[] objects = activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return;
        }
        objects[areaIndex] = wallIndex < 2
            ? OBJECT_BOMBED_PASSAGE_VERTICAL : OBJECT_BOMBED_PASSAGE_HORIZONTAL;

        byte[] status = indoorStatusTableForMap(activeRoom.mapId());
        int currentStatus = switch (wallIndex) {
            case 0 -> 0x04;
            case 1 -> 0x08;
            case 2 -> 0x02;
            case 3 -> 0x01;
            default -> throw new AssertionError(wallIndex);
        };
        int adjacentStatus = switch (wallIndex) {
            case 0 -> 0x08;
            case 1 -> 0x04;
            case 2 -> 0x01;
            case 3 -> 0x02;
            default -> throw new AssertionError(wallIndex);
        };
        int adjacentRoom = adjacentIndoorRoomIdForBombWall(wallIndex);
        status[activeRoom.roomId()] |= (byte) currentStatus;
        status[adjacentRoom] |= (byte) adjacentStatus;
        bombedWallTileOverrides.put(areaIndex, wallIndex < 2 ? 0 : 1);
        queuePuzzleSolvedJingle(sourceSlot);

        refreshActiveRoomTilemap();
        overworldCollision.setRoom(activeRoom.roomObjectsArea());
        overworldCollision.setGbcOverlay(null);
    }

    private void queuePuzzleSolvedJingle(int sourceSlot) {
        pendingRoomEntityEvents.add(new EntityCombatEvent(
            sourceSlot, 0x02, 0, false,
            EntityCombatEvent.SoundChannel.JINGLE, 0x02));
    }

    private boolean applyIndoorBombableBlockCandidate(BombObjectInteraction.Candidate candidate) {
        if (candidate == null) {
            return false;
        }
        int location = candidate.location();
        if (objectAtRoomLocation(location) != OBJECT_BOMBABLE_BLOCK) {
            return false;
        }
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0)
            + (location & 0x0F);
        int[] objects = activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return false;
        }

        objects[areaIndex] = OBJECT_FLOOR_OD;
        indoorStatusTableForMap(activeRoom.mapId())[activeRoom.roomId()]
            |= (byte) INDOOR_ROOM_STATUS_EVENT_3;
        bombedBlockTileOverrides.add(areaIndex);

        if (entityRuntime != null) {
            entityRuntime.spawnLiftableRockSmash(
                candidate.objectLeft() + 0x08, candidate.objectTop() + 0x10, 0x00);
        }

        refreshActiveRoomTilemap();
        overworldCollision.setRoom(activeRoom.roomObjectsArea());
        overworldCollision.setGbcOverlay(null);
        return true;
    }

    private int adjacentIndoorRoomIdForBombWall(int wallIndex) {
        int mapPosition = indoorMapPosition(activeRoom.mapId(), activeRoom.roomId());
        int mapPositionDelta = switch (wallIndex) {
            case 0 -> -0x08;
            case 1 -> 0x08;
            case 2 -> -0x01;
            case 3 -> 0x01;
            default -> throw new AssertionError(wallIndex);
        };
        if (mapPosition >= 0) {
            int adjacentPosition = mapPosition + mapPositionDelta;
            if (adjacentPosition >= 0 && adjacentPosition < INDOOR_MAP_LAYOUT_SIZE) {
                int layoutOffset = indoorMapLayoutOffset(activeRoom.mapId());
                if (layoutOffset >= 0 && layoutOffset + adjacentPosition < romData.length) {
                    int roomId = Byte.toUnsignedInt(romData[layoutOffset + adjacentPosition]);
                    if (roomId != 0) {
                        return roomId;
                    }
                }
            }
        }

        // Some small cave/house maps have no spatial layout table. Preserve the
        // source's byte-grid fallback for those rooms.
        return (activeRoom.roomId() + mapPositionDelta) & 0xFF;
    }

    private int indoorMapPosition(int mapId, int roomId) {
        int layoutOffset = indoorMapLayoutOffset(mapId);
        if (layoutOffset < 0 || layoutOffset + INDOOR_MAP_LAYOUT_SIZE > romData.length) {
            return -1;
        }
        for (int position = 0; position < INDOOR_MAP_LAYOUT_SIZE; position++) {
            if (Byte.toUnsignedInt(romData[layoutOffset + position]) == (roomId & 0xFF)) {
                return position;
            }
        }
        return -1;
    }

    private int indoorMapLayoutOffset(int mapId) {
        if (mapId == 0xFF) {
            return RomBank.romOffset(INDOOR_MAP_LAYOUT_BANK, COLOR_DUNGEON_MAP_LAYOUT_ADDR);
        }
        if (mapId < 0 || mapId >= 0x0B) {
            return -1;
        }
        return RomBank.romOffset(INDOOR_MAP_LAYOUT_BANK,
            INDOOR_MAP_LAYOUT_BASE_ADDR + mapId * INDOOR_MAP_LAYOUT_SIZE);
    }

    private byte[] indoorStatusTableForMap(int mapId) {
        if (mapId == 0xFF) {
            return colorDungeonRoomStatus;
        }
        return mapId >= 0x06 && mapId < 0x1A
            ? indoorBRoomStatus : indoorARoomStatus;
    }

    private static void requireLength(byte[] values, int expectedLength, String label) {
        if (values == null) {
            throw new NullPointerException(label);
        }
        if (values.length != expectedLength) {
            throw new IllegalArgumentException(label + " must contain " + expectedLength
                + " bytes, got " + values.length);
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
        applyHookshotBridgeTileOverrides();
        applyBombedWallTileOverrides();
        applyBombedBlockTileOverrides();
        applyBombedCaveDoorTileOverrides();
    }

    private RoomEntityObjectSample entityObjectSample(RoomEntity entity) {
        OverworldCollision.GroundInteractionSample sample =
            overworldCollision.groundInteractionSample(entity.x(), entity.y());
        return new RoomEntityObjectSample(sample.objectId(), sample.physicsFlag(),
            sample.objectLeft(), sample.objectTop());
    }

    private RoomEntityObjectSample entityObjectIntersectionSample(RoomEntity entity) {
        OverworldCollision.GroundInteractionSample sample =
            overworldCollision.entityInteractionSample(entity.x(), entity.y());
        return new RoomEntityObjectSample(sample.objectId(), sample.physicsFlag(),
            sample.objectLeft(), sample.objectTop());
    }

    private void applyHookshotBridgeUpdates(
            List<RoomEntityRuntime.HookshotBridgeUpdate> updates) {
        if (activeRoom == null || updates == null || updates.isEmpty()) {
            return;
        }
        int[] objects = activeRoom.roomObjectsArea();
        boolean changed = false;
        for (RoomEntityRuntime.HookshotBridgeUpdate update : updates) {
            int areaIndex = hookshotObjectAreaIndex(update.objectLeft(), update.objectTop());
            if (areaIndex < 0 || areaIndex >= objects.length) {
                continue;
            }
            RoomEntityRuntime.HookshotBridgeUpdate previous =
                hookshotBridgeTileOverrides.put(areaIndex, update);
            if (objects[areaIndex] != 0x9D || !update.equals(previous)) {
                objects[areaIndex] = 0x9D;
                changed = true;
            }
        }
        if (changed) {
            refreshActiveRoomTilemap();
        }
    }

    private void applyHookshotBridgeTileOverrides() {
        if (activeRoom == null || hookshotBridgeTileOverrides.isEmpty()) {
            return;
        }
        int[] tileIds = activeRoom.tileIds();
        for (RoomEntityRuntime.HookshotBridgeUpdate update
            : hookshotBridgeTileOverrides.values()) {
            int tileX = (update.objectLeft() & 0xFF) >>> 3;
            int tileY = (update.objectTop() & 0xFF) >>> 3;
            if (tileX < 0 || tileX + 1 >= RoomConstants.ROOM_TILE_WIDTH
                || tileY < 0 || tileY + 1 >= RoomConstants.ROOM_TILE_HEIGHT) {
                continue;
            }
            int topLeft = tileY * RoomConstants.ROOM_TILE_WIDTH + tileX;
            int bottomLeft = topLeft + RoomConstants.ROOM_TILE_WIDTH;
            if (update.direction() == HookshotBridgeMotion.PULL_DOWN_DIRECTION) {
                tileIds[topLeft] = 0x04;
                tileIds[topLeft + 1] = 0x05;
                tileIds[bottomLeft] = update.active() ? 0x08 : 0x04;
                tileIds[bottomLeft + 1] = update.active() ? 0x09 : 0x05;
            } else {
                tileIds[topLeft] = update.active() ? 0x0A : 0x04;
                tileIds[topLeft + 1] = update.active() ? 0x0B : 0x05;
                tileIds[bottomLeft] = 0x04;
                tileIds[bottomLeft + 1] = 0x05;
            }
        }
    }

    private void applyBombedWallTileOverrides() {
        if (activeRoom == null || bombedWallTileOverrides.isEmpty()) {
            return;
        }
        int[] tileIds = activeRoom.tileIds();
        for (Map.Entry<Integer, Integer> entry : bombedWallTileOverrides.entrySet()) {
            int areaOffset = entry.getKey() - RoomConstants.ROOM_OBJECTS_BASE;
            int objectRow = (areaOffset >>> 4) & 0x0F;
            int objectColumn = areaOffset & 0x0F;
            int tileX = objectColumn * 2;
            int tileY = objectRow * 2;
            if (objectRow >= RoomConstants.OBJECTS_PER_COLUMN
                || objectColumn >= RoomConstants.OBJECTS_PER_ROW
                || tileX + 1 >= RoomConstants.ROOM_TILE_WIDTH
                || tileY + 1 >= RoomConstants.ROOM_TILE_HEIGHT) {
                continue;
            }

            int[] replacement = entry.getValue() == 0
                ? new int[] { 0x72, 0x72, 0x73, 0x73 }
                : new int[] { 0x69, 0x79, 0x69, 0x79 };
            int topLeft = tileY * RoomConstants.ROOM_TILE_WIDTH + tileX;
            tileIds[topLeft] = replacement[0];
            tileIds[topLeft + 1] = replacement[1];
            tileIds[topLeft + RoomConstants.ROOM_TILE_WIDTH] = replacement[2];
            tileIds[topLeft + RoomConstants.ROOM_TILE_WIDTH + 1] = replacement[3];
        }
    }

    private void applyBombedBlockTileOverrides() {
        if (activeRoom == null || bombedBlockTileOverrides.isEmpty()) {
            return;
        }
        int[] tileIds = activeRoom.tileIds();
        for (int areaIndex : bombedBlockTileOverrides) {
            int areaOffset = areaIndex - RoomConstants.ROOM_OBJECTS_BASE;
            int objectRow = (areaOffset >>> 4) & 0x0F;
            int objectColumn = areaOffset & 0x0F;
            int tileX = objectColumn * 2;
            int tileY = objectRow * 2;
            if (objectRow >= RoomConstants.OBJECTS_PER_COLUMN
                || objectColumn >= RoomConstants.OBJECTS_PER_ROW
                || tileX + 1 >= RoomConstants.ROOM_TILE_WIDTH
                || tileY + 1 >= RoomConstants.ROOM_TILE_HEIGHT) {
                continue;
            }

            int topLeft = tileY * RoomConstants.ROOM_TILE_WIDTH + tileX;
            tileIds[topLeft] = 0x10;
            tileIds[topLeft + 1] = 0x12;
            tileIds[topLeft + RoomConstants.ROOM_TILE_WIDTH] = 0x11;
            tileIds[topLeft + RoomConstants.ROOM_TILE_WIDTH + 1] = 0x13;
        }
    }

    private void applyBombedCaveDoorTileOverrides() {
        if (activeRoom == null || bombedCaveDoorTileOverrides.isEmpty()) {
            return;
        }
        int[] tileIds = activeRoom.tileIds();
        for (int areaIndex : bombedCaveDoorTileOverrides) {
            int areaOffset = areaIndex - RoomConstants.ROOM_OBJECTS_BASE;
            int objectRow = (areaOffset >>> 4) & 0x0F;
            int objectColumn = areaOffset & 0x0F;
            int tileX = objectColumn * 2;
            int tileY = objectRow * 2;
            if (objectRow >= RoomConstants.OBJECTS_PER_COLUMN
                || objectColumn >= RoomConstants.OBJECTS_PER_ROW
                || tileX + 1 >= RoomConstants.ROOM_TILE_WIDTH
                || tileY + 1 >= RoomConstants.ROOM_TILE_HEIGHT) {
                continue;
            }

            int tileTableOffset = RomBank.romOffset(
                BOMBED_CAVE_DOOR_TILES_BANK, BOMBED_CAVE_DOOR_TILES_GBC_ADDR);
            int topLeft = tileY * RoomConstants.ROOM_TILE_WIDTH + tileX;
            tileIds[topLeft] = Byte.toUnsignedInt(romData[tileTableOffset]);
            tileIds[topLeft + 1] = Byte.toUnsignedInt(romData[tileTableOffset + 1]);
            tileIds[topLeft + RoomConstants.ROOM_TILE_WIDTH] =
                Byte.toUnsignedInt(romData[tileTableOffset + 2]);
            tileIds[topLeft + RoomConstants.ROOM_TILE_WIDTH + 1] =
                Byte.toUnsignedInt(romData[tileTableOffset + 3]);
        }
    }

    private static int hookshotObjectAreaIndex(int objectLeft, int objectTop) {
        return RoomConstants.ROOM_OBJECTS_BASE + (objectTop & 0xF0)
            + ((objectLeft & 0xF0) >>> 4);
    }

    private static int colorShellObjectIndex(RoomEntity entity) {
        int x = (entity.x() - 0x01) & 0xFF;
        int y = (entity.y() - 0x07) & 0xFF;
        return RoomConstants.ROOM_OBJECTS_BASE + (y & 0xF0) + ((x & 0xF0) >>> 4);
    }

    /**
     * Mirrors the entity collision point selected by bank $03's
     * ApplyEntityCollisionWithObject. The shared room physics query supplies
     * the active overworld/indoor table.
     */
    private boolean entityBackgroundCollision(RoomEntity entity, int direction,
                                              int nextX, int nextY) {
        return entityBackgroundCollisionResult(entity, direction, nextX, nextY).blocked();
    }

    private EntityBackgroundCollisionResult entityBackgroundCollisionResult(
            RoomEntity entity, int direction, int nextX, int nextY) {
        int ignoreHitsCountdown = entityRuntime == null
            ? 0 : entityRuntime.enemyIgnoreHitsCountdown(entity.slot());
        return entityBackgroundCollisionResult(
            entity, direction, nextX, nextY, ignoreHitsCountdown, 0);
    }

    private EntityBackgroundCollisionResult entityBackgroundCollisionResult(
            RoomEntity entity, int direction, int nextX, int nextY,
            int ignoreHitsCountdown) {
        return entityBackgroundCollisionResult(entity, direction, nextX, nextY,
            ignoreHitsCountdown, 0);
    }

    private EntityBackgroundCollisionResult entityBackgroundCollisionResult(
            RoomEntity entity, int direction, int nextX, int nextY,
            int ignoreHitsCountdown, int frameCounter) {
        EntityCollisionPointProbe.Sample sample = entityCollisionPointProbe.sample(
            entity, direction, nextX, nextY);
        int pointX = sample.x();
        int pointY = sample.y();
        int objectId = overworldCollision.objectIdAtPoint(pointX, pointY);
        int physicsFlag = overworldCollision.objectPhysicsFlagAtPoint(pointX, pointY);
        int ledgeTimer = entityRuntime == null
            ? 0 : entityRuntime.ledgeTransitionTimer(entity.slot());
        if ((entity.z() & 0x80) != 0) {
            ledgeTimer = 0;
        }
        int thrownDirection = entityRuntime == null
            ? 0xFF : entityRuntime.thrownDirection(entity.slot());
        EntityBackgroundCollisionState state = new EntityBackgroundCollisionState(
            frameCounter, activeRoom != null
                && activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD,
            thrownDirection, ledgeTimer, switchBlocksState,
            overworldCollision.linkStandingOnSwitchBlock());
        EntityBackgroundCollisionResolution resolution =
            entityBackgroundCollisionResolver.resolveWithState(
                entity, direction, sample, objectId, physicsFlag,
                ignoreHitsCountdown, state);
        if (entityRuntime != null) {
            entityRuntime.setLedgeTransitionTimer(entity.slot(), resolution.nextLedgeTimer());
        }
        return resolution.result();
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResultForTest(
            RoomEntity entity, int direction, int nextX, int nextY) {
        return entityBackgroundCollisionResult(entity, direction, nextX, nextY);
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResultForTest(
            RoomEntity entity, int direction, int nextX, int nextY, int frameCounter) {
        int ignoreHitsCountdown = entityRuntime == null
            ? 0 : entityRuntime.enemyIgnoreHitsCountdown(entity.slot());
        return entityBackgroundCollisionResult(entity, direction, nextX, nextY,
            ignoreHitsCountdown, frameCounter);
    }

    /**
     * Mirrors the terrain/status and conveyor portion of bank-$03's
     * ApplyEntityInteractionWithBackground. Terrain physics and the entity
     * options byte stay ROM-backed; the runtime invokes this after the
     * entity-family handler has completed its movement for the frame.
     */
    private RoomEntityGroundInteraction.Result entityGroundInteraction(
            RoomEntity entity, int frameCounter, int previousGroundStatus,
            int speedZ, boolean sideScrolling) {
        int options = romTables.entityOptions1(entity.type());
        if ((options & ENTITY_OPT1_NO_GROUND_INTERACTION) != 0) {
            return RoomEntityGroundInteraction.Result.unchanged(entity, 0);
        }

        // The ROM skips ground interaction while an entity is moving upward;
        // a negative Z byte denotes a falling entity and continues through
        // the shared helper.
        int z = entity.z() & 0xFF;
        if (z != 0 && (z & 0x80) == 0) {
            return RoomEntityGroundInteraction.Result.unchanged(entity, 0);
        }

        OverworldCollision.GroundInteractionSample sample =
            overworldCollision.groundInteractionSample(entity.x(), entity.y());
        int physicsFlag = sample.physicsFlag();
        int groundStatus = groundStatusFor(sample);
        boolean deepWaterOrLava = physicsFlag == PhysicsFlags.DEEP_WATER
            || physicsFlag == PhysicsFlags.LAVA;
        boolean retainsDeepWaterStatus = entity.type() == ENTITY_FISH
            || entity.type() == ENTITY_PEAHAT
            || entity.type() == ENTITY_ROOSTER
            || entity.type() == ENTITY_BOW_WOW
            || entity.type() == ENTITY_MARIN_AT_THE_SHORE;
        if (deepWaterOrLava && !retainsDeepWaterStatus) {
            // The source unloads ordinary entities before jumping directly to
            // .createWaterSplash, so this splash is not gated by options or
            // the status-transition speed test.
            return RoomEntityGroundInteraction.Result.unloaded(entity, 0x00, true);
        }

        boolean splash = splashAllowed(options, previousGroundStatus, groundStatus,
            speedZ, sideScrolling);
        int movementIndex = physicsFlag - PhysicsFlags.CAT_CONVEYOR;
        RoomEntity updated = entity;
        if (movementIndex >= 0 && movementIndex < ENTITY_CONVEYOR_MOVEMENT_X.length
            && (frameCounter & 0x03) == 0) {
            updated = withEntityPosition(entity,
                entity.x() + ENTITY_CONVEYOR_MOVEMENT_X[movementIndex],
                entity.y() + ENTITY_CONVEYOR_MOVEMENT_Y[movementIndex]);
        }

        RoomEntityGroundInteraction.PitTransition pit = pitTransitionFor(
            entity, sample, currentLinkMotionState);
        return new RoomEntityGroundInteraction.Result(updated, groundStatus, pit, false, splash);
    }

    static RoomEntityGroundInteraction.PitTransition pitTransitionFor(
            RoomEntity entity, OverworldCollision.GroundInteractionSample sample,
            int linkMotionState) {
        boolean pit = sample.objectId() == OBJECT_WELL
            || sample.physicsFlag() == PhysicsFlags.NORMAL_PIT
            || sample.physicsFlag() == PhysicsFlags.PIT_WARP;
        if (!pit || entity.type() == ENTITY_BOW_WOW || entity.type() == ENTITY_ROOSTER
            || entity.type() == ENTITY_HEART_CONTAINER) {
            return null;
        }
        if (entity.type() == ENTITY_MARIN_AT_THE_SHORE
            && (linkMotionState != LINK_MOTION_FALLING_DOWN
                || sample.objectId() != OBJECT_WELL)) {
            return null;
        }
        return new RoomEntityGroundInteraction.PitTransition(
            sample.objectLeft() + 0x08, sample.objectTop() + 0x10);
    }

    private static int groundStatusFor(OverworldCollision.GroundInteractionSample sample) {
        int physicsFlag = sample.physicsFlag();
        if (physicsFlag == PhysicsFlags.DEEP_WATER || physicsFlag == PhysicsFlags.LAVA) {
            return 0x02;
        }
        if (sample.objectId() == OBJECT_WATER_LADDER_SIDESCROLL
            || physicsFlag == PhysicsFlags.WATER_SIDESCROLL) {
            return 0x01;
        }
        if (physicsFlag == PhysicsFlags.NONE) {
            return 0x00;
        }
        if (physicsFlag == PhysicsFlags.SHALLOW_WATER) {
            return 0x02;
        }
        if (physicsFlag == PhysicsFlags.GRASS) {
            return 0x03;
        }
        return 0x01;
    }

    private static boolean splashAllowed(int options, int previousGroundStatus,
                                         int currentGroundStatus, int speedZ,
                                         boolean sideScrolling) {
        if ((options & ENTITY_OPT1_SPLASH_IN_WATER) == 0
            || previousGroundStatus == currentGroundStatus
            || previousGroundStatus == 0x03
            || currentGroundStatus == 0x03) {
            return false;
        }
        if (sideScrolling) {
            // The side-scroll branch uses shared speed-X/Y tables, which are
            // not yet part of the Java entity snapshot. Keep this path
            // source-safe until that table is ported.
            return false;
        }
        int unsignedSpeedZ = speedZ & 0xFF;
        return (unsignedSpeedZ & 0x80) != 0 && unsignedSpeedZ < 0xE7;
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
