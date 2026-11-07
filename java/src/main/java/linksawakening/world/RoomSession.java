package linksawakening.world;

import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.event.InstrumentCompletionEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkAttackStepRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.LinkSwordFinalPoseRequest;
import linksawakening.world.entity.event.LinkSwordSpinPoseRequest;
import linksawakening.world.entity.event.MadamMeowMeowFullHealRequest;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.event.RoomStatusPersistenceRequest;
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.entity.event.ThrownBackgroundCollisionEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.event.WarpDestination;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomBank;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomSymbols;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static linksawakening.world.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_WIDTH;

public final class RoomSession {
    final RoomTileRedraw roomTileRedraw = new RoomTileRedraw(this);
    final RoomCollisionQueries roomCollisionQueries = new RoomCollisionQueries(this);
    final RoomPlayerItemActions roomPlayerItemActions = new RoomPlayerItemActions(this);
    final RoomPuzzleController roomPuzzleController = new RoomPuzzleController(this);
    final RoomStoryState roomStoryState = new RoomStoryState(this);
    final RoomDoorInteractions roomDoorInteractions = new RoomDoorInteractions(this);
    final RoomObjectMutations roomObjectMutations = new RoomObjectMutations(this);
    final RoomTransitionController roomTransitionController = new RoomTransitionController(this);
    final RoomLoadCoordinator roomLoadCoordinator = new RoomLoadCoordinator(this);
    static long nextRoomSessionId;
    static final int ROOM_STATUS_TABLE_SIZE = 0x100;
    static final int COLOR_DUNGEON_SAVE_STATUS_SIZE = 0x20;
    static final int W_TILESET_NO_UPDATE = 0xFF;
    static final int ENTITY_OPT1_NO_GROUND_INTERACTION = 0x10;
    static final int ENTITY_OPT1_SPLASH_IN_WATER = 0x08;
    static final int ENTITY_FISH = 0xCC;
    static final int ENTITY_PEAHAT = 0xA0;
    static final int ENTITY_ROOSTER = 0xD5;
    static final int ENTITY_BOW_WOW = 0x6D;
    static final int ENTITY_MARIN_AT_THE_SHORE = 0xC1;
    static final int ENTITY_HEART_CONTAINER = 0x36;
    static final int ENTITY_DROPPABLE_SECRET_SEASHELL = 0x3D;
    static final int OBJECT_ROCKY_GROUND = 0x09;
    static final int OBJECT_ROCKY_CAVE_DOOR = 0xE1;
    static final int OBJECT_CLOSED_GATE = 0xC2;
    static final int OBJECT_CAVE_DOOR = 0xE3;
    static final int OBJECT_BOMBABLE_CAVE_DOOR = 0xBA;
    static final int OBJECT_GIANT_SKULL_TOP_LEFT = 0xBB;
    static final int OBJECT_GIANT_SKULL_BOTTOM_RIGHT = 0xBE;
    static final int OBJECT_BOMBABLE_BLOCK = 0xA9;
    static final int OBJECT_FLOOR_OD = 0x0D;
    static final int OBJECT_SHOVEL_HOLE = 0xCC;
    static final int OBJECT_SHOVEL_INDOOR_DIGGABLE = 0x05;
    static final int OBJECT_SHOVEL_BLOCKER_A = 0x0C;
    static final int OBJECT_SHOVEL_BLOCKER_B = 0x0D;
    static final int OBJECT_SHOVEL_BLOCKER_C = 0xB9;
    static final int SHOVEL_STATE_ACTIVE = 0x01;
    static final int SHOVEL_STATE_DUG = 0x02;
    static final int SHOVEL_DIG_TIMER = 0x10;
    static final int SHOVEL_FINISH_TIMER = 0x18;
    static final int SHOVEL_DIALOG_TABLE = 0;
    static final int SHOVEL_DIALOG_ID = 0x79;
    static final int OCARINA_NO_SONG_DIALOG_TABLE = 0;
    static final int OCARINA_NO_SONG_DIALOG_ID = 0x8E;
    static final int OCARINA_MARIN_DIALOG_TABLE = 2;
    static final int OCARINA_MARIN_DIALOG_ID = 0x77;
    static final int[] SHOVEL_TARGET_X = {0x14, 0xFC, 0x08, 0x08};
    static final int[] SHOVEL_TARGET_Y = {0x0A, 0x0A, 0xFC, 0x14};
    static final int OW_ROOM_STATUS_OPENED = 0x04;
    static final int OW_ROOM_STATUS_FLAG_CHANGED = 0x04;
    static final int ROOM_STATUS_CHEST_OPEN = 0x10;
    static final int ROOM_STATUS_EVENT_1 = 0x10;
    static final int ROOM_STATUS_EVENT_2 = 0x20;
    static final int ROOM_STATUS_DISCOVERED = 0x40;
    static final int ROOM_OW_TAIL_KEY_CHEST = 0x41;
    static final int ROOM_OW_TAIL_CAVE_ENTRANCE = 0xD3;
    static final int ROOM_OW_KEY_CAVERN_ENTRANCE = 0xB5;
    static final int ROOM_OW_MYSTERIOUS_WOODS_LOST = 0x63;
    static final int ENTITY_TARIN = TarinRaccoonMotion.ENTITY_TYPE;
    static final int ENTITY_OWL_EVENT = 0x41;
    static final int TAIL_CAVE_GATE_LOCATION = 0x16;
    static final int KEYHOLE_DIALOG_TABLE = 2;
    static final int TAIL_KEYHOLE_DIALOG_ID = 0x30;
    static final int SLIME_KEYHOLE_DIALOG_ID = 0x31;
    static final int LINK_COLLISION_TYPE_UP = 0x01;
    static final int TAIL_CAVE_RUMBLE_INITIAL_COUNTDOWN = 0xDF;
    static final int TAIL_CAVE_RUMBLE_GATE_FRAME = 0x08;
    static final int INDOOR_ROOM_STATUS_EVENT_3 = 0x40;
    static final int EVENT_TRIGGER_MASK = 0x1F;
    static final int EVENT_EFFECT_MASK = 0xE0;
    static final int EVENT_TRIGGER_KILL_ALL_ENEMIES = 0x01;
    static final int EVENT_TRIGGER_PUSH_SINGLE_BLOCK = 0x02;
    static final int EVENT_TRIGGER_STEP_ON_BUTTON = 0x03;
    static final int EVENT_TRIGGER_LIGHT_TORCHES = 0x05;
    static final int EVENT_TRIGGER_KILL_IN_ORDER = 0x06;
    static final int EVENT_TRIGGER_PUSH_BLOCKS = 0x07;
    static final int EVENT_TRIGGER_THROW_AT_DOOR = 0x0B;
    static final int EVENT_EFFECT_OPEN_LOCKED_DOORS = 0x20;
    static final int EVENT_EFFECT_REVEAL_CHEST = 0x60;
    static final int EVENT_EFFECT_DROP_KEY = 0x80;
    static final int EVENT_EFFECT_REVEAL_STAIRWAY = 0xA0;
    static final int EVENT_EFFECT_CLEAR_MIDBOSS = 0xC0;
    static final int EVENT_CLEAR_MIDBOSS = 0xC1;
    static final int OBJECT_SWITCH_BUTTON = 0xAA;
    static final int OBJECT_LIFTABLE_POT = 0x20;
    static final int OBJECT_POT_WITH_SWITCH = 0x8E;
    static final int INDOOR_POT_PULL_FRAMES = 0x08;
    static final int OBJECT_PUSHABLE_BLOCK = 0xA7;
    static final int OBJECT_KEYHOLE_BLOCK = 0xDE;
    static final int OBJECT_SETTLED_PUSHED_BLOCK = 0xA6;
    static final int OBJECT_STAIRS_DOWN = 0xBE;
    static final int STAIRCASE_NONE = 0;
    static final int STAIRCASE_INACTIVE = 1;
    static final int STAIRCASE_ACTIVE = 2;
    static final int EVENT_STAIRWAY_LOCATION = 0x18;
    static final int PUSH_BLOCK_CONTACT_TICKS = 0x40;
    static final int KEYHOLE_BLOCK_CONTACT_TICKS = 0x20;
    static final int PUSHED_BLOCK_MOTION_FRAMES = 0x21;
    static final int SWITCH_BUTTON_PRESS_FRAMES = 0x18;
    static final int SWITCH_BUTTON_PRESSED = 0x60;
    static final int ENTITY_OPTION_EXCLUDED_FROM_KILL_ALL = 0x02;
    static final int ROOM_EVENT_CHEST_REVEAL_COUNTDOWN = 0x0F;
    static final int ROOM_EVENT_CHEST_WRITE_COUNTDOWN = 0x04;
    static final int KEY_DOOR_PHYSICS_BASE = 0x90;
    static final int KEY_DOOR_ANIMATION_FRAMES = 0x08;
    static final int[] KEY_DOOR_CLOSED_OBJECTS = {
        0x2D, 0x2F, 0x31, 0x33
    };
    static final int[][] KEY_DOOR_OPEN_OBJECTS = {
        {0x43, 0x44}, {0x8C, 0x08}, {0x09, 0x0A}, {0x0B, 0x0C}
    };
    static final int[] KEY_DOOR_CURRENT_STATUS = {0x04, 0x08, 0x02, 0x01};
    static final int[] KEY_DOOR_ADJACENT_STATUS = {0x08, 0x04, 0x01, 0x02};
    static final int OBJECT_BOMBED_PASSAGE_VERTICAL = 0x3D;
    static final int OBJECT_BOMBED_PASSAGE_HORIZONTAL = 0x3E;
    static final int BOMBABLE_WALL_PHYSICS_BASE = 0x99;
    static final int INDOOR_MAP_LAYOUT_SIZE = 0x40;
    static final int LINK_MOTION_FALLING_DOWN = 0x06;
    static final int OBJECT_WELL = 0x61;
    static final int OBJECT_WATER_LADDER_SIDESCROLL = 0x67;
    static final int[] ENTITY_CONVEYOR_MOVEMENT_X = {0, 0, -1, 1, 1, -1, 1, -1};
    static final int[] ENTITY_CONVEYOR_MOVEMENT_Y = {1, -1, 0, 0, 1, 1, -1, -1};

    final byte[] romData;
    final GPU gpu;
    final RoomLoader roomLoader;
    final RoomPaletteLoader roomPaletteLoader;
    final OverworldTilesetTable overworldTilesetTable;
    final ManboWarpResolver manboWarpResolver;
    final OwlEventDialogResolver owlEventDialogResolver;
    final OwlStatueDialogResolver owlStatueDialogResolver;
    final OverworldCollision overworldCollision;
    final TransientVfxSystem transientVfxSystem;
    final DroppableRupeeSystem droppableRupeeSystem;
    final RoomLoadListener roomLoadListener;
    final RoomTilemapBuilder roomTilemapBuilder;
    final RomRandomByteSource entityRandomByteSource = new RomRandomByteSource();
    final EntitySpriteHandlerCatalog entitySpriteHandlerCatalog;
    final FollowingNpcEntitySpawner followingNpcEntitySpawner;
    final RomEnemyCombatTables enemyCombatTables;
    final ChestContentsTable chestContentsTable;
    final DungeonRoomEventTable dungeonRoomEventTable;
    final RomTables romTables;
    final EnemyDropResolver enemyDropResolver;
    final EntityCollisionPointProbe entityCollisionPointProbe;
    final EntityBackgroundCollisionResolver entityBackgroundCollisionResolver;
    final OverworldBushInteraction overworldBushInteraction;
    final IndoorTorchPaletteEffect indoorTorchPaletteEffect;
    final RoomEntityBackgroundInteraction entityBackgroundInteraction =
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
    final LinkPositionHistory followingLinkPositionHistory = new LinkPositionHistory();

    final long roomSessionId = ++nextRoomSessionId;
    ActiveRoom activeRoom;
    /** Increments on every ROM room load, including a reload of the same room. */
    long roomLoadSerial;
    /** ROM wIndoorRoom: the current position in the active map's 8x8 layout. */
    int indoorMapPosition = -1;
    final RoomWorldState roomState;
    final RoomScrollNavigation scrollNavigation;
    final RoomEntityBridge entityBridge = new RoomEntityBridge(this);
    final RoomEntityInputs entityInputs =
        new RoomEntityInputs(() -> entityBridge.entityRuntime);

    Warp pendingDungeonWarp;

    PendingShovelDrop pendingShovelDrop;
    int shovelUseState;

    final Map<Integer, HookshotBridgeUpdate>
        hookshotBridgeTileOverrides = new HashMap<>();
    /** Immediate bomb-wall draw commands use a different tile order than the room object table. */
    final Map<Integer, Integer> bombedWallTileOverrides = new HashMap<>();
    /** The basic A9 breakable path also draws floor tiles in source command order. */
    final Set<Integer> bombedBlockTileOverrides = new HashSet<>();
    /** Bombed overworld cave doors use the dedicated GBC redraw tile order. */
    final Set<Integer> bombedCaveDoorTileOverrides = new HashSet<>();
    // A torch handler may change WRAM but return before emitting its BG draw command.
    final Map<Integer, int[]> retainedTorchTiles = new HashMap<>();
    final RecentRoomEntityClears recentRoomEntityClears =
        new RecentRoomEntityClears();
    int currentOverworldTilesetId = W_TILESET_NO_UPDATE;
    FollowingNpcState followingNpcState = FollowingNpcState.none();
    EnemyDropResolver.CounterState enemyDropCounters =
        new EnemyDropResolver.CounterState(0, 0);
    int boomerangSoundCounter;
    boolean boomerangSoundClockProvided;
    int enemyDropMaxHearts = 3;
    int enemyDropHealth = 6;
    boolean enemyDropActivePowerUp;
    /** WRAM wSwitchBlocksState; the source reset value is zero. */
    int switchBlocksState;
    /** WRAM wSwitchableObjectAnimationStage; zero means no update is active. */
    int switchableObjectAnimationStage;
    int followingLinkX = 0x08;
    int followingLinkY = 0x10;
    int followingLinkZ;
    int followingEntityYOffset;
    int followingLinkDirection;
    boolean followingNpcRoomNeedsSync;

    boolean shouldGetLostInMysteriousWoods;

    int ocarinaPlaybackCountdown;
    int ocarinaSongFlags;
    int selectedSongIndex;
    int ocarinaAnimationCounter;
    int ocarinaAnimationPhase;
    boolean pendingManboTransition;
    boolean pendingInstrumentTransition;
    boolean hasBirdKey;
    boolean hasTailKey;
    int bowWowState;

    int richardSpokenFlag;
    /** WRAM wTarinFlag; persisted NPC/world progression, not player inventory state. */
    int tarinFlag;
    /** WRAM wShieldLevel retained for room-load-time palette selection. */
    int playerShieldLevel;
    int currentLinkMotionState = EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE;
    /** WRAM wC1A2; ResetRoomVariables clears the room trigger counter. */
    int roomTriggerCount;
    int activeRoomEvent;
    /** wHasInstrument1..: bit 0 records each dungeon miniboss defeat. */

    boolean roomEventEffectExecuted;
    boolean thrownAtDoorTriggerResolved;
    int roomEventChestRevealCountdown;
    int roomEventStairRevealCountdown;
    int staircaseState;
    int staircaseX;
    int staircaseY;
    int switchButtonPressCounter;
    int switchButtonPressed;
    int indoorKeyDoorAnimationCountdown;
    int indoorKeyDoorDirection = -1;
    int indoorKeyDoorLocation = -1;
    boolean indoorBossDoorOpening;
    /** wC191 for OBJECT_PUSHABLE_BLOCK's sustained collision branch. */
    int pushBlockContactTicks;
    int indoorPotPullFrames;
    int pushedBlockMotionFrames;
    int pushedBlockSourceLocation = -1;
    int pushedBlockDestinationLocation = -1;
    int pushedBlockDirection = -1;
    int pushedBlockEntitySlot = -1;
    /** Defers trigger-$07's common effect until the frame after settlement. */
    boolean pushedBlocksTriggerEffectPending;
    boolean worldLinkMotionBlockPending;
    int pendingShutterCloseMask, closedShutterMask;
    int shutterCloseDirection = -1, shutterCloseTicks, shutterCloseLocation = -1;
    boolean shutterCloseStarted;
    final java.util.Map<Integer, Integer> shutterTileOverrides = new java.util.HashMap<>();
    int dungeonTitleMessageCountdown;
    int tailCaveKeyholeCountdown;
    boolean tailCaveFinalMotionBlockPending;
    int nextWorldMusicTrackCountdown;
    int tailCaveResumeMusicTrack = 0x05;

    int entityDefaultMusicTrack = 0x05;

    GameplaySoundSink colorShellSoundSink = GameplaySoundSink.none();
    final ColorShellWorld colorShellWorld = new ColorShellWorld() {
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
    }

    public void loadInitialOverworld(int roomId) { roomLoadCoordinator.loadInitialOverworld(roomId); }

    public void loadWarpDestination(Warp warp) { roomTransitionController.loadWarpDestination(warp); }

    /** $00:0F53 countdown and $01:61EE dungeon-title dialog gates. */
    public int tickDungeonTitleMessage(boolean dialogActive, boolean inventoryClosed, int linkMotionState, boolean freeMovement) { return roomTransitionController.tickDungeonTitleMessage(dialogActive, inventoryClosed, linkMotionState, freeMovement); }

    public void startAdjacentOverworldScroll(ScrollController scrollController, int direction, int linkScreenX, int linkScreenY) { roomTransitionController.startAdjacentOverworldScroll(scrollController, direction, linkScreenX, linkScreenY); }

    void startMysteriousWoodsLostScroll(ScrollController scrollController, int linkScreenX, int linkScreenY) { roomTransitionController.startMysteriousWoodsLostScroll(scrollController, linkScreenX, linkScreenY); }

    public void startAdjacentIndoorScroll(ScrollController scrollController, int direction, int linkScreenX, int linkScreenY) { roomTransitionController.startAdjacentIndoorScroll(scrollController, direction, linkScreenX, linkScreenY); }

    public void loadOverworld(int roomId) { roomLoadCoordinator.loadOverworld(roomId); }

    public void loadIndoor(int mapId, int roomId) { roomLoadCoordinator.loadIndoor(mapId, roomId); }

    /** Restores hMapRoom and the independently saved source wIndoorRoom byte. */
    public void loadIndoorFromSavedPosition(int mapId, int roomId, int savedIndoorMapPosition) { roomLoadCoordinator.loadIndoorFromSavedPosition(mapId, roomId, savedIndoorMapPosition); }

    public void loadIndoor(int mapId, int roomId, int mapCategory) { roomLoadCoordinator.loadIndoor(mapId, roomId, mapCategory); }

    void loadIndoor(int mapId, int roomId, int mapCategory, int transitionDirection) { roomLoadCoordinator.loadIndoor(mapId, roomId, mapCategory, transitionDirection); }

    /** Current ROM health byte for diagnostic captures. */
    public int entityHealth(int slot) { return entityBridge.entityRuntime.enemyHealth(slot); }

    public ActiveRoom activeRoom() {
        return activeRoom;
    }

    /** Identifies the current load instance for Link room-entry state. */
    public long roomLoadSerial() {
        return roomLoadSerial;
    }

    /** Distinguishes same-room loads performed by different live sessions. */
    public long roomLoadToken() {
        return (roomSessionId << 32) ^ roomLoadSerial;
    }

    public boolean hasActiveRoom() {
        return activeRoom != null;
    }

    public boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) { return roomStoryState.applyMarinWakeUpPresentation(x, y, spriteVariant); }

    /** Tarin's indoor handler updates his facing every 32 frames, even during the bed scene. */
    public void updateHouseTarinPresentation(int frameCounter, int linkX, int linkY) { roomStoryState.updateHouseTarinPresentation(frameCounter, linkX, linkY); }

    /** Supplies the patched EntityInitKeyDropPoint inventory byte. */
    public void setBirdKeyOwned(boolean owned) { roomStoryState.setBirdKeyOwned(owned); }

    /** Supplies wHasTailKey to the active owl-event handler. */
    public void setTailKeyOwned(boolean owned) { roomStoryState.setTailKeyOwned(owned); }

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
        return indoorMapPosition;
    }

    /** Room-status bytes in the active dungeon's 8x8 ROM map order. */
    public byte[] dungeonMinimapRoomStatusSnapshot() { return roomState.dungeonMinimapRoomStatusSnapshot(activeRoom); }

    public byte[] overworldRoomStatusSnapshot() { return roomState.overworldRoomStatusSnapshot(); }

    public byte[] indoorARoomStatusSnapshot() { return roomState.indoorARoomStatusSnapshot(); }

    public byte[] indoorBRoomStatusSnapshot() { return roomState.indoorBRoomStatusSnapshot(); }

    public byte[] colorDungeonRoomStatusSnapshot() { return roomState.colorDungeonRoomStatusSnapshot(); }

    /**
     * Clears the persistent WRAM-shaped world state created for a fresh save.
     * The source new-file path starts with zeroed room-status, entity-clear,
     * dungeon-item, and dungeon-progress regions; retaining any of these from
     * a previously played file can incorrectly unload opening entities such
     * as the beach sword and owl.
     */
    public void initializeNewGameWorldState() { roomStoryState.initializeNewGameWorldState(); }

    /** Restores the room-status WRAM tables loaded by the source save path. */
    public void restoreRoomStatuses(byte[] overworldStatus, byte[] indoorAStatus, byte[] indoorBStatus, byte[] colorDungeonStatus) { roomStoryState.restoreRoomStatuses(overworldStatus, indoorAStatus, indoorBStatus, colorDungeonStatus); }

    public byte[] dungeonItemFlagsSnapshot() { return roomState.dungeonItemFlagsSnapshot(); }

    public byte[] colorDungeonItemFlagsSnapshot() { return roomState.colorDungeonItemFlagsSnapshot(); }

    public byte[] currentDungeonItemFlagsSnapshot() { return roomState.currentDungeonItemFlagsSnapshot(); }

    public void restoreDungeonItemFlags(byte[] dungeonFlags, byte[] colorFlags) { roomState.restoreDungeonItemFlags(dungeonFlags, colorFlags); }

    /** Snapshots wHasInstrument1..wHasInstrument8 (DB65..DB6C). */
    public byte[] dungeonProgressFlagsSnapshot() { return roomState.dungeonProgressFlagsSnapshot(); }

    public void restoreDungeonProgressFlags(byte[] flags) { roomState.restoreDungeonProgressFlags(flags); }

    int overworldRoomStatusForTest(int roomId) { return roomState.overworldRoomStatusForTest(roomId); }

    int indoorRoomStatusForTest(int mapId, int roomId) { return roomState.indoorRoomStatusForTest(mapId, roomId); }

    void setEnemyDropCountersForTest(int droppedItemCount, int droppedBombArrowCount) {
        enemyDropCounters = new EnemyDropResolver.CounterState(
            droppedItemCount, droppedBombArrowCount);
    }

    EnemyDropResolver.CounterState enemyDropCountersForTest() {
        return enemyDropCounters;
    }

    void setSwitchButtonPressedForTest(int value) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException("Switch-button state must be an unsigned byte");
        }
        switchButtonPressed = value;
    }

    int switchButtonPressedForTest() {
        return switchButtonPressed;
    }

    public int[][] palettes() {
        return activeRoom == null ? null : activeRoom.palettes();
    }

    public boolean heartPiecePickupActive() {
        return entityBridge.entityRuntime != null && entityBridge.entityRuntime.heartPiecePickupActive();
    }

    public void setHeartPiecesCount(int count) {
        if (entityBridge.entityRuntime != null) entityBridge.entityRuntime.setHeartPiecesCount(count);
    }
    public List<HeartPiecePickupMotion.Update> consumeHeartPieceUpdates() {
        if (entityBridge.entityRuntime == null) return List.of();
        var updates = entityBridge.entityRuntime.consumeHeartPieceUpdates();
        if (updates.stream().anyMatch(HeartPiecePickupMotion.Update::complete)) markActiveRoomCompleted();
        return updates;
    }

    public String instrumentTrace() { return entityBridge.entityRuntime == null ? "" : entityBridge.entityRuntime.instrumentTrace(); }
    public String moblinKingTrace() { return entityBridge.entityRuntime == null ? "-1" : entityBridge.entityRuntime.moblinKingTrace(); }

    public RoomRenderSnapshot renderSnapshot() {
        if (activeRoom == null) return null;
        RoomRenderSnapshot snapshot = activeRoom.renderSnapshot();
        return new RoomRenderSnapshot(snapshot.tileIds(), snapshot.tileAttrs(), snapshot.palettes(),
            snapshot.entities(), entityBridge.entityRuntime == null ? null : entityBridge.entityRuntime.instrumentWarpVisual(),
            entityBridge.entityRuntime == null ? null : entityBridge.entityRuntime.heartPieceVisual());
    }

    /**
     * Applies the follower display-list selections used by
     * {@code CreateFollowingNpcEntity}. This low-level hook is retained for
     * callers that already own the dynamic entity state; new code should use
     * {@link #setFollowingNpcState(FollowingNpcState, int, int, int, int, int)}
     * so spawning and selection stay in one ROM-shaped path.
     */
    public void setFollowerSpriteOverrides(Map<Integer, EntitySpriteDefinition> overrides) { roomStoryState.setFollowerSpriteOverrides(overrides); }

    /**
     * Binds the current WRAM-like follower flags and Link coordinates to the
     * active room. The operation is applied once per room load or follower
     * state change, matching the room-transition caller in room_transition.asm.
     * Coordinates are hLinkPositionX/Y/Z and entityYOffset is wC13B.
     */
    public void setFollowingNpcState(FollowingNpcState state, int linkX, int linkY, int linkZ, int entityYOffset, int linkDirection) { roomStoryState.setFollowingNpcState(state, linkX, linkY, linkZ, entityYOffset, linkDirection); }

    public FollowingNpcState followingNpcState() { return roomStoryState.followingNpcState(); }

    /** Mirrors wIsBowWowFollowingLink, including BOW_WOW_KIDNAPPED=$80. */
    public int bowWowState() { return roomStoryState.bowWowState(); }

    public void setBowWowState(int state) { roomStoryState.setBowWowState(state); }

    public int richardSpokenFlag() { return roomStoryState.richardSpokenFlag(); }

    public void setRichardSpokenFlag(int flag) { roomStoryState.setRichardSpokenFlag(flag); }

    /** Mirrors the unsigned-byte world progression flag at wTarinFlag. */
    public int tarinFlag() { return roomStoryState.tarinFlag(); }

    public void setTarinFlag(int flag) { roomStoryState.setTarinFlag(flag); }

    /** Supplies the held J_A|J_B state consumed by input-driven entity handlers. */
    public void setEntityActionButtonsHeld(boolean actionButtonsHeld) { entityInputs.setEntityActionButtonsHeld(actionButtonsHeld); }

    /** The cling flag produced by the previous entity-handler pass. */
    public boolean gelClingingToLink() {
        return entityBridge.entityRuntime != null && entityBridge.entityRuntime.gelClingingToLink();
    }

    /** Supplies wDialogState's active gate to the room entity handlers. */
    public void setEntityLinkVelocityZ(int velocity) { entityInputs.setEntityLinkVelocityZ(velocity); }

    public void setEntityDialogActive(boolean active) { entityInputs.setEntityDialogActive(active); }

    /** Supplies the remaining WRAM gates read by ShouldLinkTalkToEntity_05. */
    public void setEntityTalkState(boolean inventoryAppearing, int dialogCooldown,
                                   int windowY) { entityInputs.setEntityTalkState(inventoryAppearing, dialogCooldown, windowY); }

    public void setEntityDialogAskSelectionIndex(int selectionIndex) { entityInputs.setEntityDialogAskSelectionIndex(selectionIndex); }

    /** Supplies the source wActiveMusicIndex gate to entity state machines. */
    public void setEntityMusicActive(boolean active) { entityInputs.setEntityMusicActive(active); }

    /** Supplies the distinct held A/B states consumed by ROM input handlers. */
    public void setEntityActionButtonsHeld(boolean actionButtonAHeld, boolean actionButtonBHeld) { entityInputs.setEntityActionButtonsHeld(actionButtonAHeld, actionButtonBHeld); }

    /** Supplies whether any held Game Boy joypad bit is active. */
    public void setEntityJoypadHeld(boolean joypadHeld) { entityInputs.setEntityJoypadHeld(joypadHeld); }

    /** Supplies hPressedButtonsMask's currently-held D-pad bits to entities. */
    public void setEntityPressedButtonsMask(int pressedButtonsMask) { entityInputs.setEntityPressedButtonsMask(pressedButtonsMask); }

    /** Supplies the B/A inventory bytes consumed by Like Like's handler. */
    public void setEntityInventorySlots(int itemA, int itemB) { entityInputs.setEntityInventorySlots(itemA, itemB); }

    /** Supplies the upgrade bytes consumed by the live chest dialog handler. */
    public void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) { roomStoryState.setChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel); }

    /** Supplies the two WRAM values checked by SleepyToadstoolEntityHandler. */
    public void setToadstoolPlayerState(boolean hasToadstool, int magicPowderCount) { entityInputs.setToadstoolPlayerState(hasToadstool, magicPowderCount); }

    public void setEntityDefaultMusicTrack(int trackId) { roomStoryState.setEntityDefaultMusicTrack(trackId); }

    void updateWitchEnvironment() { roomStoryState.updateWitchEnvironment(); }

    /** Supplies wGoldenLeavesCount to the live Hiding Slime Key handler. */
    public void setEntityGoldenLeavesCount(int goldenLeavesCount) { entityInputs.setEntityGoldenLeavesCount(goldenLeavesCount); }

    /** Supplies the live HRAM shake/collision values used by tree shells. */
    public void setSecretSeashellPegasusCollisionState(boolean screenShakeActive,
                                                        boolean collisionActive,
                                                        int collisionX, int collisionY) { entityInputs.setSecretSeashellPegasusCollisionState(screenShakeActive, collisionActive, collisionX, collisionY); }

    /** Supplies the held A/B state used by EntityGetLiftedUp. */
    public void setEntityPowerBraceletButtonHeld(boolean buttonHeld) { entityInputs.setEntityPowerBraceletButtonHeld(buttonHeld); }

    /** Supplies the held A/B state for the equipped-bomb lift/throw path. */
    public void setEntityBombButtonHeld(boolean buttonHeld) { entityInputs.setEntityBombButtonHeld(buttonHeld); }

    /** Supplies wLinkAttackStepAnimationCountdown to input-driven entity handlers. */
    public void setEntityAttackStepAnimationCountdown(int countdown) { entityInputs.setEntityAttackStepAnimationCountdown(countdown); }

    /** Supplies hLinkFinalPositionX captured before the current Link update. */
    public void setEntityFinalPositionX(int positionX) { entityInputs.setEntityFinalPositionX(positionX); }

    /** Returns wShouldGetLostInMysteriousWoods from the latest entity pass. */
    public boolean shouldGetLostInMysteriousWoods() { return roomStoryState.shouldGetLostInMysteriousWoods(); }

    /** Starts the source UseOcarina countdown consumed by active entities. */
    public boolean startOcarina(int countdown, int songFlags, int selectedSong) { return roomTransitionController.startOcarina(countdown, songFlags, selectedSong); }

    /** Mirrors wLinkPlayingOcarinaCountdown for Link's motion gate. */
    public boolean ocarinaPlaying() { return roomTransitionController.ocarinaPlaying(); }

    /** Mirrors hLinkAnimationState $75/$76 while LinkPlayingOcarinaHandler runs. */
    public int ocarinaAnimationState() { return roomTransitionController.ocarinaAnimationState(); }

    /**
     * Returns the ROM destination prepared by the completed Mambo branch.
     * The caller starts the non-interactive transition and applies this warp
     * only after the source's {@code $C0}-frame effect has finished.
     */
    public Warp consumeManboPondTransitionRequest() { return roomTransitionController.consumeManboPondTransitionRequest(); }

    /** Returns the room's prepared warp after the instrument effect completes. */
    public Warp consumeInstrumentTransitionRequest() { return roomTransitionController.consumeInstrumentTransitionRequest(); }

    /** Returns the completed entity-$61 request emitted by the ROM handler. */
    Warp consumeClearedDungeonWarp() { return roomTransitionController.consumeClearedDungeonWarp(); }

    /** Supplies the player fields consumed by the ROM enemy-drop resolver. */
    public void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp) { entityBridge.setEnemyDropPlayerState(maxHearts, health, activePowerUp); }

    public RoomEntityRuntime.LiftedEntityState liftedEntityState() {
        return entityBridge.entityRuntime == null
            ? RoomEntityRuntime.LiftedEntityState.none()
            : entityBridge.entityRuntime.liftedEntityState();
    }

    /** Throws the currently held object using Link's Java direction order. */
    public boolean throwLiftedEntity(int linkDirection) { return roomPlayerItemActions.throwLiftedEntity(linkDirection); }

    /**
     * Mirrors {@code FireHookshot}: the item path supplies Link's current ROM
     * coordinates and the room owns the player-projectile entity slot.
     */
    public boolean fireHookshot(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean linkAirborne, boolean linkPushing) { return roomPlayerItemActions.fireHookshot(linkEntityX, linkEntityY, linkEntityZ, romDirection, linkAirborne, linkPushing); }

    /** Mirrors UseBoomerang's player-projectile spawn bridge. */
    public boolean fireBoomerang(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask) { return roomPlayerItemActions.fireBoomerang(linkEntityX, linkEntityY, linkEntityZ, romDirection, pressedButtonsMask); }

    /** Mirrors the level-two branch of UseSword's player-projectile spawn. */
    public boolean fireSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return roomPlayerItemActions.fireSwordBeam(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    /** Mirrors the delayed UseMagicRod player-projectile spawn bridge. */
    public boolean fireMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return roomPlayerItemActions.fireMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    public boolean fireMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) { return roomPlayerItemActions.fireMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower); }

    /** Mirrors the delayed UseMagicPowder sprinkle-entity spawn bridge. */
    public boolean sprinkleMagicPowder(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return roomPlayerItemActions.sprinkleMagicPowder(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    /**
     * Mirrors {@code UseShovel}: the item always enters its 24-frame pose
     * window when Link is grounded, while the adjacent-cell probe determines
     * whether the sound is a poke or a normal dig.
     *
     * <p>The coordinates are the ROM's {@code hLinkPositionX/Y} values. The
     * caller supplies ROM direction order: RIGHT, LEFT, UP, DOWN.</p>
     */
    public ShovelStartResult startShovel(int linkEntityX, int linkEntityY, int romDirection, boolean linkAirborne) { return roomPlayerItemActions.startShovel(linkEntityX, linkEntityY, romDirection, linkAirborne); }

    /** Applies the ROM shovel timer's delayed room mutation and finish path. */
    public void advanceShovel(int linkEntityX, int linkEntityY, int romDirection, int timer) { roomPlayerItemActions.advanceShovel(linkEntityX, linkEntityY, romDirection, timer); }

    /** Resolves the ROM shovel animation table through Java's Link direction order. */
    public int shovelAnimationState(int javaDirection, int timer) { return roomPlayerItemActions.shovelAnimationState(javaDirection, timer); }

    ShovelProbe probeShovel(int linkEntityX, int linkEntityY, int romDirection) { return roomPlayerItemActions.probeShovel(linkEntityX, linkEntityY, romDirection); }

    void writeShovelHole(ShovelProbe probe) { roomPlayerItemActions.writeShovelHole(probe); }

    /**
     * Mirrors the ordinary player-bomb placement bridge. The item has already
     * applied PlaceBomb's inventory ordering; this method owns the room/runtime
     * allocation and immutable snapshot refresh.
     */
    public boolean placeBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return roomPlayerItemActions.placeBomb(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    /** Returns whether the last successful bomb placement queued the top-view bump jingle. */
    public boolean lastBombPlacementPlayedBump() { return roomPlayerItemActions.lastBombPlacementPlayedBump(); }

    /** Result of the ROM ShootArrow bridge, including its conditional whoosh. */
    public record ArrowShotResult(boolean spawned, boolean playWhoosh) {
    }

    /** Mirrors ShootArrow's player-projectile bridge and refreshes the room snapshot. */
    public boolean shootArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return roomPlayerItemActions.shootArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    public ArrowShotResult shootArrowResult(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return roomPlayerItemActions.shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    /** ShootArrow's label_140F speed-table selector for Piece of Power. */
    public ArrowShotResult shootArrowResult(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) { return roomPlayerItemActions.shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower); }

    /** Returns the modeled player-projectile count used by the item gates. */
    public int activeProjectileCount() { return roomPlayerItemActions.activeProjectileCount(); }

    /** Returns whether the live room currently owns an ordinary bomb entity. */
    public boolean bombActive() { return roomPlayerItemActions.bombActive(); }

    int bombDirectionForTest(int slot) {
        return entityBridge.entityRuntime == null ? 0xFF : entityBridge.entityRuntime.bombDirection(slot);
    }

    /** Returns whether the live room currently owns entity {@code $03}. */
    public boolean hookshotActive() { return roomPlayerItemActions.hookshotActive(); }

    /** Returns whether the live room currently owns the boomerang entity. */
    public boolean boomerangActive() { return roomPlayerItemActions.boomerangActive(); }

    public void setColorShellSoundSink(GameplaySoundSink soundSink) {
        colorShellSoundSink = soundSink == null ? GameplaySoundSink.none() : soundSink;
    }

    void setColorShellStateForTest(int slot, int state, int transitionCountdown,
                                   int direction, int speedX, int speedY) {
        if (entityBridge.entityRuntime != null) {
            entityBridge.entityRuntime.setColorShellStateForTest(slot, state, transitionCountdown,
                direction, speedX, speedY);
        }
    }

    int entityGroundStatusForTest(int slot) {
        if (entityBridge.entityRuntime == null) {
            return 0;
        }
        return entityBridge.entityRuntime.groundStatus(slot);
    }

    void setEntityIgnoreHitsCountdownForTest(int slot, int value) {
        if (entityBridge.entityRuntime != null) {
            entityBridge.entityRuntime.setEnemyIgnoreHitsCountdownForTest(slot, value);
        }
    }

    void setEntityThrownDirectionForTest(int slot, int value) {
        if (entityBridge.entityRuntime != null) {
            entityBridge.entityRuntime.setThrownDirection(slot, value);
        }
    }

    void setEntityLedgeTimerForTest(int slot, int value) {
        if (entityBridge.entityRuntime != null) {
            entityBridge.entityRuntime.setLedgeTransitionTimer(slot, value);
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
        return entityBridge.entityRuntime == null ? 0 : entityBridge.entityRuntime.ledgeTransitionTimer(slot);
    }

    int entityFallingTargetXForTest(int slot) {
        return entityBridge.entityRuntime == null ? 0 : entityBridge.entityRuntime.fallingTargetX(slot);
    }

    int entityTransitionCountdownForTest(int slot) {
        if (entityBridge.entityRuntime == null) {
            return 0;
        }
        RoomEntity entity = activeRoom == null ? null : activeRoom.entities().slots().get(slot);
        return entity != null && entity.type() == WarpMotion.ENTITY_TYPE
            ? entityBridge.entityRuntime.warpTransitionCountdownForTest(slot)
            : entityBridge.entityRuntime.transitionCountdown(slot);
    }

    int roomTriggerCountForTest() {
        return roomTriggerCount & 0xFF;
    }

    int activeRoomEventForTest() {
        return activeRoomEvent & 0xFF;
    }

    boolean hasDungeonInstrumentForTest(int mapId) {
        return mapId >= 0 && mapId < roomState.dungeonProgressFlags.length
            && (roomState.dungeonProgressFlags[mapId] & 0x02) != 0;
    }

    int indoorTorchPaletteEffectAddressForTest() {
        return indoorTorchPaletteEffect.effectAddress();
    }

    void igniteIndoorTorchPaletteForTest() {
        indoorTorchPaletteEffect.igniteTorch();
    }

    void setRoomTriggerCountForTest(int value) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException("Room trigger count must be an unsigned byte");
        }
        roomTriggerCount = value;
    }

    int entityDropSpeedXForTest(int slot) {
        return entityBridge.entityRuntime == null ? 0 : entityBridge.entityRuntime.dropSpeedX(slot);
    }

    int entityDropSpeedZForTest(int slot) {
        return entityBridge.entityRuntime == null ? 0 : entityBridge.entityRuntime.dropSpeedZ(slot);
    }

    void replaceEntityRuntimeForTest(RoomEntityRuntime runtime) {
        entityBridge.entityRuntime = runtime;
    }

    /** WorldHandler's shared sound clock persists across entity and room loads. */
    public void advanceBoomerangSoundClock(boolean worldGameplay) { entityBridge.advanceBoomerangSoundClock(worldGameplay); }

    public void tickEntities(int frameCounter) { entityBridge.tickEntities(frameCounter); }

    public void tickEntities(int frameCounter, int linkEntityX, int linkEntityY) { entityBridge.tickEntities(frameCounter, linkEntityX, linkEntityY); }

    public void tickEntities(int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkDirection) { entityBridge.tickEntities(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkDirection); }

    public void tickEntities(int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, boolean linkAirborne, int linkDirection) { entityBridge.tickEntities(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkAirborne, linkDirection); }

    /**
     * Advances entities and returns the ROM projectile interactions generated
     * during this frame.  The older tick overloads route through this method
     * with a non-interactive Link state, so callers that do not provide player
     * interaction state retain their previous behavior.
     */
    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkMotionState, int linkDirection, boolean usingShield) { return entityBridge.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkMotionState, linkDirection, usingShield); }

    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, boolean linkAirborne, int linkMotionState, int linkDirection, boolean usingShield) { return entityBridge.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkAirborne, linkMotionState, linkDirection, usingShield); }

    /** Advances entities with the additional ROM shield and invincibility fields. */
    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkMotionState, int linkDirection, boolean usingShield, int shieldLevel, int invincibilityCounter) { return entityBridge.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkMotionState, linkDirection, usingShield, shieldLevel, invincibilityCounter); }

    /** Advances entities with Link's optional current sword collision rectangle. */
    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkMotionState, int linkDirection, boolean usingShield, int shieldLevel, int invincibilityCounter, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight) { return entityBridge.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkMotionState, linkDirection, usingShield, shieldLevel, invincibilityCounter, swordCollisionActive, swordX, swordWidth, swordY, swordHeight); }

    /** Advances entities with Link's current ROM collision byte. */
    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkMotionState, int linkDirection, int collisionType, boolean usingShield, int shieldLevel, int invincibilityCounter, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight) { return entityBridge.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkMotionState, linkDirection, collisionType, usingShield, shieldLevel, invincibilityCounter, swordCollisionActive, swordX, swordWidth, swordY, swordHeight); }

    /** Advances entities with the current Link speed bytes for enemy-bomb recoil. */
    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkMotionState, int linkDirection, int collisionType, boolean usingShield, int shieldLevel, int invincibilityCounter, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight, int linkSpeedX, int linkSpeedY) { return entityBridge.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkMotionState, linkDirection, collisionType, usingShield, shieldLevel, invincibilityCounter, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, linkSpeedX, linkSpeedY); }

    /** Rich live path with Link's explicit airborne state. */
    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, boolean linkAirborne, int linkMotionState, int linkDirection, int collisionType, boolean usingShield, int shieldLevel, int invincibilityCounter, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight, int linkSpeedX, int linkSpeedY) { return entityBridge.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkAirborne, linkMotionState, linkDirection, collisionType, usingShield, shieldLevel, invincibilityCounter, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, linkSpeedX, linkSpeedY); }

    /** Mirrors the countdown-zero branches of LinkPlayingOcarinaHandler. */
    void finishOcarinaPlayback() { roomTransitionController.finishOcarinaPlayback(); }

    void tickOcarinaAnimationHandler() { roomTransitionController.tickOcarinaAnimationHandler(); }

    /** Returns and clears entity side-effect events emitted by the last tick. */
    public List<EntityCombatEvent> consumeEntityEvents() { return entityBridge.consumeEntityEvents(); }

    /** Returns and clears handlers that restore Link's pre-entity final position. */
    public List<LinkFinalPositionRequest>
            consumeLinkFinalPositionRequests() { return entityBridge.consumeLinkFinalPositionRequests(); }

    /** Returns and clears Rooster's Link HRAM writes from the last entity tick. */
    public List<RoosterLinkStateRequest>
            consumeRoosterLinkStateRequests() { return entityBridge.consumeRoosterLinkStateRequests(); }

    /** Returns and clears handlers that block Link's next interactive motion frame. */
    public List<LinkMotionBlockRequest>
            consumeLinkMotionBlockRequests() { return entityBridge.consumeLinkMotionBlockRequests(); }

    /** Returns and clears side-view platform Link writes from the last entity tick. */
    public List<SideViewPlatformLinkRequest>
            consumeSideViewPlatformLinkRequests() { return entityBridge.consumeSideViewPlatformLinkRequests(); }

    /** Returns state-0 side-view pot Link writes from the last entity tick. */
    public List<SideViewPotLinkRequest>
            consumeSideViewPotLinkRequests() { return entityBridge.consumeSideViewPotLinkRequests(); }

    /** Returns entity-$61 WarpState3 Link writes and clears supported session actions. */
    public List<WarpLinkStateRequest>
    consumeWarpLinkStateRequests() { return entityBridge.consumeWarpLinkStateRequests(); }

    /** Returns and clears Hinox grab/throw writes for the last entity tick. */
    public List<HinoxLinkEffectRequest>
            consumeHinoxLinkEffectRequests() { return entityBridge.consumeHinoxLinkEffectRequests(); }

    /** Returns and clears explicit ROM Link fallen-pose requests. */
    public List<LinkFallenPoseRequest>
            consumeLinkFallenPoseRequests() { return entityBridge.consumeLinkFallenPoseRequests(); }

    /** Returns and clears a motion block emitted by a world handler rather than an entity. */
    public void setFreeMovementMode(boolean enabled) { entityBridge.setFreeMovementMode(enabled); }

    public boolean consumeWorldLinkMotionBlockRequest() { return entityBridge.consumeWorldLinkMotionBlockRequest(); }

    public List<LinkFacingRequest> consumeLinkFacingRequests() { return entityBridge.consumeLinkFacingRequests(); }

    public List<LinkAttackClearRequest>
            consumeLinkAttackClearRequests() { return entityBridge.consumeLinkAttackClearRequests(); }

    /** Returns lifted-throw attack-step writes for Main's post-entity phase. */
    public List<LinkAttackStepRequest>
            consumeLinkAttackStepRequests() { return entityBridge.consumeLinkAttackStepRequests(); }

    public List<LinkHeldItemPoseRequest>
            consumeLinkHeldItemPoseRequests() { return entityBridge.consumeLinkHeldItemPoseRequests(); }

    public boolean gotItemPresentationActive() { return entityBridge.gotItemPresentationActive(); }

    public boolean powerUpPickupSequenceActive() { return entityBridge.powerUpPickupSequenceActive(); }

    public boolean swordPickupSequenceActive() { return entityBridge.swordPickupSequenceActive(); }

    public List<LinkSwordSpinPoseRequest>
            consumeLinkSwordSpinPoseRequests() { return entityBridge.consumeLinkSwordSpinPoseRequests(); }

    public List<LinkSwordFinalPoseRequest>
            consumeLinkSwordFinalPoseRequests() { return entityBridge.consumeLinkSwordFinalPoseRequests(); }

    /** Returns and clears ROM screen-shake requests emitted by the last entity tick. */
    public List<ScreenShakeRequest> consumeScreenShakeRequests() { return entityBridge.consumeScreenShakeRequests(); }

    /** Returns and clears chest reward applications emitted by the last entity tick(s). */
    public List<ChestRewardEvent> consumeChestRewardEvents() { return entityBridge.consumeChestRewardEvents(); }

    /** Returns and clears key or hookshot rewards emitted by key-drop entities. */
    public List<KeyRewardEvent> consumeKeyRewardEvents() { return entityBridge.consumeKeyRewardEvents(); }

    /** Returns and clears Hiding Slime Key leaf-count updates emitted by entity handlers. */
    public List<SlimeKeyRewardEvent> consumeSlimeKeyRewardEvents() { return entityBridge.consumeSlimeKeyRewardEvents(); }

    /** Returns and clears completed boss heart-container rewards. */
    public List<HeartContainerRewardEvent>
            consumeHeartContainerRewards() { return entityBridge.consumeHeartContainerRewards(); }

    public List<MadamMeowMeowFullHealRequest>
            consumeMadamMeowMeowFullHealRequests() { return entityBridge.consumeMadamMeowMeowFullHealRequests(); }

    public List<RichardProgressEvent> consumeRichardProgressEvents() { return entityBridge.consumeRichardProgressEvents(); }

    public List<SwordPickupRewardEvent> consumeSwordPickupRewards() { return entityBridge.consumeSwordPickupRewards(); }

    public List<ToadstoolRewardEvent> consumeToadstoolRewards() { return entityBridge.consumeToadstoolRewards(); }

    public List<WitchExchangeEvent> consumeWitchExchangeEvents() { return entityBridge.consumeWitchExchangeEvents(); }

    public List<WitchRewardEvent> consumeWitchRewardEvents() { return entityBridge.consumeWitchRewardEvents(); }

    /** Returns the raw music-track request emitted by a chest, or {@code -1}. */
    public int consumePendingMusicTrack() { return entityBridge.consumePendingMusicTrack(); }

    public int consumePendingMusicFadeOutCountdown() { return entityBridge.consumePendingMusicFadeOutCountdown(); }

    /** Returns and clears Like Like capture/release requests from the last tick. */
    public List<LikeLikeEvent> consumeLikeLikeEvents() { return entityBridge.consumeLikeLikeEvents(); }

    /** Returns and clears ROM dialog requests emitted by the last entity tick. */
    public List<DialogRequest> consumeEntityDialogRequests() { return entityBridge.consumeEntityDialogRequests(); }

    /** Returns and clears source-shaped bomb-explosion interaction requests from the last tick. */
    public List<BombExplosionEvent> consumeBombExplosionEvents() { return entityBridge.consumeBombExplosionEvents(); }

    /** Advances the ROM's VBlank tile path, including switch blocks. */
    public void tickGameplayVBlank() { entityBridge.tickGameplayVBlank(); }

    /** Link uses Java's down/up/left/right order; projectile ROM tables use right/left/up/down. */
    static int romDirectionForProjectileCollision(int linkDirection) {
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
    public List<EntityCombatEvent> resolveEntityCombat(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight) { return entityBridge.resolveEntityCombat(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, swordCollisionActive, swordX, swordWidth, swordY, swordHeight); }

    public List<EntityCombatEvent> resolveEntityCombat(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight, EnemyAttackContext attackContext) { return entityBridge.resolveEntityCombat(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, attackContext); }

    public List<EntityCombatEvent> resolveEntityCombat(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, int linkVerticalVelocity, boolean linkInteractive, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight, EnemyAttackContext attackContext) { return entityBridge.resolveEntityCombat(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkVerticalVelocity, linkInteractive, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, attackContext); }

    public int clearEntity(int slot) { return entityBridge.clearEntity(slot); }

    /**
     * Applies the ROM pickup collision cadence to the active room entities.
     * Item-specific effects are returned to the gameplay layer so room state
     * and persistent entity state stay separate.
     */
    /** Link coordinates use ROM entity origins, not sprite top-left; direction uses Link constants. */
    public EntityPickupEvent collectEntityIfNeeded(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive) { return entityBridge.collectEntityIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive); }

    public EntityPickupEvent collectEntityIfNeeded(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive, int linkDirection) { return entityBridge.collectEntityIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection); }

    public EntityPickupEvent collectEntityIfNeeded(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive, int linkDirection, int linkZ) { return entityBridge.collectEntityIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection, linkZ); }

    public RoomBoundaryState boundaryState(int linkX, int linkY) { return roomCollisionQueries.boundaryState(linkX, linkY); }

    public RoomBoundaryState boundaryState(int linkX, int linkY, boolean linkAirborne) { return roomCollisionQueries.boundaryState(linkX, linkY, linkAirborne); }

    public RoomBoundaryState boundaryState(int linkX, int linkY, boolean linkAirborne, int linkPhysicsModifier) { return roomCollisionQueries.boundaryState(linkX, linkY, linkAirborne, linkPhysicsModifier); }

    void setActiveRoom(LoadedRoom room) { roomLoadCoordinator.setActiveRoom(room); }

    void setActiveRoom(LoadedRoom room, int[][] objectPalettes) { roomLoadCoordinator.setActiveRoom(room, objectPalettes); }

    void initializeSwitchBlockTiles() { roomLoadCoordinator.initializeSwitchBlockTiles(); }

    void synchronizeSwitchBlockCollisionState() { roomLoadCoordinator.synchronizeSwitchBlockCollisionState(); }

    void synchronizeFollowingNpcEntitiesIfNeeded() { roomStoryState.synchronizeFollowingNpcEntitiesIfNeeded(); }

    void configureEnemyDropRuntime() { roomLoadCoordinator.configureEnemyDropRuntime(); }

    boolean roomHasBossBattle() { return roomLoadCoordinator.roomHasBossBattle(); }

    void clearTransientRoomState() {
        pendingShutterCloseMask = closedShutterMask = shutterCloseTicks = 0;
        shutterCloseDirection = shutterCloseLocation = -1;
        shutterCloseStarted = false;
        shutterTileOverrides.clear();
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
        retainedTorchTiles.clear();
        entityBridge.pendingBombExplosionEvents.clear();
        pendingDungeonWarp = null;
        entityBridge.pendingRoomEntityEvents.clear();
        entityBridge.pendingChestRewardEvents.clear();
        entityBridge.pendingKeyRewardEvents.clear();
        entityBridge.pendingSlimeKeyRewardEvents.clear();
        entityBridge.pendingHeartContainerRewards.clear();
        entityBridge.pendingMadamMeowMeowFullHealRequests.clear();
        entityBridge.pendingRichardProgressEvents.clear();
        entityBridge.pendingSwordPickupRewards.clear();
        entityBridge.pendingToadstoolRewards.clear();
        entityBridge.pendingWitchExchangeEvents.clear();
        entityBridge.pendingWitchRewardEvents.clear();
        entityBridge.pendingRoomDialogRequests.clear();
        pendingManboTransition = false;
        pendingInstrumentTransition = false;
        pendingShovelDrop = null;
        shovelUseState = 0;
        roomTriggerCount = 0;
        activeRoomEvent = 0;
        roomEventEffectExecuted = false;
        thrownAtDoorTriggerResolved = false;
        roomEventChestRevealCountdown = 0;
        roomEventStairRevealCountdown = 0;
        staircaseState = STAIRCASE_NONE;
        staircaseX = 0;
        staircaseY = 0;
        switchButtonPressCounter = 0;
        indoorKeyDoorAnimationCountdown = 0;
        indoorKeyDoorDirection = -1;
        indoorKeyDoorLocation = -1;
        indoorBossDoorOpening = false;
        pushBlockContactTicks = 0;
        indoorPotPullFrames = 0;
        pushedBlockMotionFrames = 0;
        pushedBlockSourceLocation = -1;
        pushedBlockDestinationLocation = -1;
        pushedBlockDirection = -1;
        pushedBlockEntitySlot = -1;
        pushedBlocksTriggerEffectPending = false;
        worldLinkMotionBlockPending = false;
        tailCaveKeyholeCountdown = 0;
        tailCaveFinalMotionBlockPending = false;
        nextWorldMusicTrackCountdown = 0;
        entityBridge.pendingRoomMusicTrack = -1;
    }

    void applyBombObjectInteractions(List<BombExplosionEvent> events) { roomObjectMutations.applyBombObjectInteractions(events); }

    void applyBasicBombObjectCandidate(BombObjectInteraction.Candidate candidate) { roomObjectMutations.applyBasicBombObjectCandidate(candidate); }

    void applyBoomerangObjectInteractions( List<BoomerangObjectRequest> requests) { roomObjectMutations.applyBoomerangObjectInteractions(requests); }

    void applyMagicRodObjectInteractions( List<MagicRodObjectRequest> requests) { roomObjectMutations.applyMagicRodObjectInteractions(requests); }

    void applyMagicPowderObjectInteractions( List<MagicPowderObjectRequest> requests) { roomObjectMutations.applyMagicPowderObjectInteractions(requests); }

    boolean writeMagicPowderTorchObject(int location, int expectedObject, int replacementObject, boolean redraw) { return roomObjectMutations.writeMagicPowderTorchObject(location, expectedObject, replacementObject, redraw); }

    void retainTorchTiles(int areaIndex) { roomObjectMutations.retainTorchTiles(areaIndex); }

    void applyRetainedTorchTiles() { roomObjectMutations.applyRetainedTorchTiles(); }

    int countUnlitTorches() { return roomObjectMutations.countUnlitTorches(); }

    void copyIndoorTorchPalettesToActiveRoom() { roomObjectMutations.copyIndoorTorchPalettesToActiveRoom(); }

    void applyPuzzleBombObjectCandidate(BombObjectInteraction.Candidate candidate, int sourceSlot, int sourceZ) { roomObjectMutations.applyPuzzleBombObjectCandidate(candidate, sourceSlot, sourceZ); }

    void applyOutdoorBombableCaveDoorCandidate(BombObjectInteraction.Candidate candidate, int sourceSlot) { roomObjectMutations.applyOutdoorBombableCaveDoorCandidate(candidate, sourceSlot); }

    int objectAtRoomLocation(int location) {
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0)
            + (location & 0x0F);
        int[] objects = activeRoom.roomObjectsArea();
        return areaIndex < 0 || areaIndex >= objects.length ? 0xFF : objects[areaIndex] & 0xFF;
    }

    /** Result of the ROM shovel probe made when Link begins using the item. */
    public record ShovelStartResult(boolean started, boolean poking, int location) {
    }

    record ShovelProbe(boolean valid, int location, int objectLeft, int objectTop) {
        static ShovelProbe invalid() {
            return new ShovelProbe(false, -1, -1, -1);
        }
    }

    record PendingShovelDrop(int objectLeft, int objectTop,
                                     int linkEntityX, int linkEntityY,
                                     boolean allowDrop) {
    }

    /** Result of the source closed-chest action bridge. */
    public record ChestOpenResult(boolean opened, int itemType, int entitySlot, int location) {
    }

    /**
     * Mirrors the closed-$A0 branch in the source object interaction path.
     * The source at $00:1F80 samples SwordAreaX/YForDirection relative to
     * Link's sprite top-left (ROM entity coordinates minus 8/16).
     */
    public ChestOpenResult tryOpenChest(int linkPixelX, int linkPixelY, int linkDirection, boolean actionPressed, int swordLevel) { return roomDoorInteractions.tryOpenChest(linkPixelX, linkPixelY, linkDirection, actionPressed, swordLevel); }

    /** Mirrors bank 2's locked-door branch for physics categories $90-$93. */
    public boolean tryUnlockIndoorKeyDoor(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) { return roomDoorInteractions.tryUnlockIndoorKeyDoor(linkPixelX, linkPixelY, linkDirection, collisionType); }

    /** $02:70F2-$710F checks ROM Link coordinates against the authored door origin. */
    static boolean alignedWithIndoorKeyDoor(int location, int direction,
                                                    int linkPixelX, int linkPixelY) {
        int offset = direction < 2
            ? linkPixelX + 8 - ((location & 15) << 4) - 0x0C
            : linkPixelY + 16 - (location & 0xF0) - 0x14;
        return (offset & 0xFF) < 8;
    }

    /** Mirrors bank 2's $A7 collision counter and entity-$06 spawn branch. */
    public boolean tryPushIndoorBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) { return roomDoorInteractions.tryPushIndoorBlock(linkPixelX, linkPixelY, linkDirection, collisionType); }

    /** Mirrors bank 0's sustained Power Bracelet pull on indoor object $20/$8E. */
    public boolean tryLiftIndoorObject(int linkPixelX, int linkPixelY, int linkDirection, boolean braceletButtonHeld, int pressedButtonsMask) { return roomDoorInteractions.tryLiftIndoorObject(linkPixelX, linkPixelY, linkDirection, braceletButtonHeld, pressedButtonsMask); }

    /** Includes the source's shortened pull while Piece of Power is active. */
    public boolean tryLiftIndoorObject(int linkPixelX, int linkPixelY, int linkDirection, boolean braceletButtonHeld, int pressedButtonsMask, boolean pieceOfPowerActive) { return roomDoorInteractions.tryLiftIndoorObject(linkPixelX, linkPixelY, linkDirection, braceletButtonHeld, pressedButtonsMask, pieceOfPowerActive); }

    int entityPhysicsFlagsForTest(int slot) {
        return entityBridge.entityRuntime == null ? 0 : entityBridge.entityRuntime.physicsFlags(slot);
    }

    /** Dispatches bank 2's shared $A7/$DE interactive-block collision branch. */
    public boolean tryInteractWithIndoorBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) { return roomDoorInteractions.tryInteractWithIndoorBlock(linkPixelX, linkPixelY, linkDirection, collisionType); }

    /** Mirrors bank 2's $DE interactive-block branch and TryOpenLockedDoor. */
    public boolean tryUnlockIndoorKeyholeBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) { return roomDoorInteractions.tryUnlockIndoorKeyholeBlock(linkPixelX, linkPixelY, linkDirection, collisionType); }

    static int keyDoorDirectionIndex(int linkDirection) {
        return switch (linkDirection) {
            case Link.DIRECTION_UP -> 0;
            case Link.DIRECTION_DOWN -> 1;
            case Link.DIRECTION_LEFT -> 2;
            case Link.DIRECTION_RIGHT -> 3;
            default -> -1;
        };
    }

    static int keyDoorCollisionBit(int direction) {
        return switch (direction) {
            case 0 -> 0x01;
            case 1 -> 0x02;
            case 2 -> 0x04;
            case 3 -> 0x08;
            default -> 0;
        };
    }

    static int[][] keyDoorCollisionPoints(int x, int y, int direction) {
        return switch (direction) {
            case 0 -> new int[][] {{x + 0x06, y + 0x06}, {x + 0x09, y + 0x06}};
            case 1 -> new int[][] {{x + 0x06, y + 0x0F}, {x + 0x09, y + 0x0F}};
            case 2 -> new int[][] {{x + 0x04, y + 0x09}, {x + 0x04, y + 0x0C}};
            case 3 -> new int[][] {{x + 0x0B, y + 0x09}, {x + 0x0B, y + 0x0C}};
            default -> new int[0][0];
        };
    }

    /**
     * Mirrors bank 2's Tail Key branch in ApplyCollisionWithObject. The
     * keyhole is automatic: walking upward into physics category $C0 either
     * opens Dialog230 or schedules the ROM's $DF-frame gate-opening rumble.
     */
    public boolean tryUnlockTailCaveKeyhole(int linkPixelX, int linkPixelY, int linkDirection, int collisionType, boolean hasTailKey) { return roomDoorInteractions.tryUnlockTailCaveKeyhole(linkPixelX, linkPixelY, linkDirection, collisionType, hasTailKey); }

    /** Mirrors bank 2's room-$B5 Slime Key branch of the shared keyhole handler. */
    public boolean tryUnlockKeyCavernKeyhole(int linkPixelX, int linkPixelY, int linkDirection, int collisionType, int goldenLeavesCount) { return roomDoorInteractions.tryUnlockKeyCavernKeyhole(linkPixelX, linkPixelY, linkDirection, collisionType, goldenLeavesCount); }

    boolean tryUnlockOverworldDungeonKeyhole( int linkPixelX, int linkPixelY, int linkDirection, int collisionType, int roomId, boolean hasKey, int dialogLowId) { return roomDoorInteractions.tryUnlockOverworldDungeonKeyhole(linkPixelX, linkPixelY, linkDirection, collisionType, roomId, hasKey, dialogLowId); }

    /** One RenderTranscientRumble step, called once per unpaused gameplay frame. */
    public int tickTailCaveKeyholeSequence() { return roomDoorInteractions.tickTailCaveKeyholeSequence(); }

    public boolean tailCaveKeyholeSequenceActive() { return roomDoorInteractions.tailCaveKeyholeSequenceActive(); }

    int tailCaveKeyholeCountdownForTest() { return roomDoorInteractions.tailCaveKeyholeCountdownForTest(); }

    int nextWorldMusicTrackCountdownForTest() { return roomDoorInteractions.nextWorldMusicTrackCountdownForTest(); }

    void writeTailCaveGateAnimationFrame(int countdown) { roomDoorInteractions.writeTailCaveGateAnimationFrame(countdown); }

    void tickNextWorldMusicTrackCountdown() { roomDoorInteractions.tickNextWorldMusicTrackCountdown(); }

    void writeBombPuzzleObject(int location, int objectId) { roomObjectMutations.writeBombPuzzleObject(location, objectId); }

    void refreshOverworldCollisionAfterObjectMutation() { roomObjectMutations.refreshOverworldCollisionAfterObjectMutation(); }

    void applyIndoorBombableWallCandidate(BombObjectInteraction.Candidate candidate, int sourceSlot) { roomObjectMutations.applyIndoorBombableWallCandidate(candidate, sourceSlot); }

    void queuePuzzleSolvedJingle(int sourceSlot) { roomObjectMutations.queuePuzzleSolvedJingle(sourceSlot); }

    boolean applyIndoorBombableBlockCandidate(BombObjectInteraction.Candidate candidate) { return roomObjectMutations.applyIndoorBombableBlockCandidate(candidate); }

    int adjacentIndoorRoomIdForBombWall(int wallIndex) { return scrollNavigation.adjacentIndoorRoomIdForBombWall(activeRoom, indoorMapPosition, wallIndex); }

    int findIndoorMapPosition(int mapId, int roomId) { return roomState.findIndoorMapPosition(mapId, roomId); }

    int indoorMapLayoutOffset(int mapId) { return roomState.indoorMapLayoutOffset(mapId); }

    byte[] indoorStatusTableForMap(int mapId) { return roomState.indoorStatusTableForMap(mapId); }

    int activeRoomStatusFlags() { return roomState.activeRoomStatusFlags(activeRoom); }

    void harvestKeyQuicksandEvents() {
        if (entityBridge.entityRuntime == null
            || entityBridge.entityRuntime.consumePendingKeyQuicksandEvents().isEmpty()) {
            return;
        }
        roomState.overworldRoomStatus[0xCE] |= (byte) OW_ROOM_STATUS_FLAG_CHANGED;
        roomState.indoorARoomStatus[0xF8] |= (byte) 0x20;
    }

    void harvestKeyRewardEvents() {
        if (entityBridge.entityRuntime == null) {
            return;
        }
        List<KeyRewardEvent> rewards =
            entityBridge.entityRuntime.consumePendingKeyRewardEvents();
        for (KeyRewardEvent reward : rewards) {
            if (reward.itemType() >= ChestContentsTable.CHEST_MAP
                && reward.itemType() <= ChestContentsTable.CHEST_SMALL_KEY) {
                roomState.dungeonItemState.incrementCurrentFlag(reward.itemType());
            }
            markActiveRoomCompleted();
        }
        entityBridge.pendingKeyRewardEvents.addAll(rewards);
    }

    void harvestHeartContainerRewards() {
        if (entityBridge.entityRuntime == null) {
            return;
        }
        List<HeartContainerRewardEvent> rewards =
            entityBridge.entityRuntime.consumePendingHeartContainerRewards();
        if (rewards.isEmpty()) {
            return;
        }
        markHeartContainerCollected();
        entityBridge.pendingHeartContainerRewards.addAll(rewards);
    }

    void harvestSwordPickupRewards() {
        if (entityBridge.entityRuntime == null) {
            return;
        }
        List<SwordPickupRewardEvent> rewards =
            entityBridge.entityRuntime.consumePendingSwordPickupRewards();
        if (rewards.isEmpty()) {
            return;
        }
        markActiveRoomCompleted();
        entityBridge.pendingSwordPickupRewards.addAll(rewards);
    }

    void harvestToadstoolRewards() {
        if (entityBridge.entityRuntime != null) {
            entityBridge.pendingToadstoolRewards.addAll(entityBridge.entityRuntime.consumePendingToadstoolRewards());
        }
    }

    void harvestInstrumentRewards() {
        if (entityBridge.entityRuntime == null || activeRoom == null
            || entityBridge.entityRuntime.consumePendingInstrumentRewards().isEmpty()
            || activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            return;
        }
        indoorStatusTableForMap(activeRoom.mapId())[activeRoom.roomId()]
            |= (byte) ROOM_STATUS_EVENT_1;
        if (activeRoom.mapId() >= 0 && activeRoom.mapId() < roomState.dungeonProgressFlags.length) {
            roomState.dungeonProgressFlags[activeRoom.mapId()] |= 0x02;
        }
        entityBridge.entityRuntime.setEntityRoomStatus(activeRoomStatusFlags());
    }

    void harvestInstrumentCompletions() {
        if (entityBridge.entityRuntime == null) {
            return;
        }
        for (InstrumentCompletionEvent completion
            : entityBridge.entityRuntime.consumePendingInstrumentCompletions()) {
            pendingInstrumentTransition = true;
            if (completion.mapId() == 0x00) {
                bowWowState = 0x80;
            } else if (completion.mapId() == 0x01) {
                tarinFlag = 0x02;
            }
        }
    }

    void harvestRichardProgressEvents() {
        if (entityBridge.entityRuntime == null) {
            return;
        }
        List<RichardProgressEvent> events =
            entityBridge.entityRuntime.consumePendingRichardProgressEvents();
        for (RichardProgressEvent event : events) {
            if (event.kind() == RichardProgressEvent.Kind.START_QUEST) {
                roomState.indoorBRoomStatus[0xC7] |= (byte) ROOM_STATUS_EVENT_1;
                richardSpokenFlag = Math.max(richardSpokenFlag, 0x02);
            } else {
                entityInputs.entityGoldenLeavesCount = event.goldenLeavesCount();
            }
        }
        entityBridge.pendingRichardProgressEvents.addAll(events);
    }

    void harvestWitchEvents() {
        if (entityBridge.entityRuntime != null) {
            entityBridge.pendingWitchExchangeEvents.addAll(
                entityBridge.entityRuntime.consumePendingWitchExchangeEvents());
            entityBridge.pendingWitchRewardEvents.addAll(
                entityBridge.entityRuntime.consumePendingWitchRewardEvents());
        }
    }

    void harvestOwlEventCompletions() {
        if (entityBridge.entityRuntime == null || activeRoom == null) {
            return;
        }
        if (entityBridge.entityRuntime.consumePendingOwlEventCompletions().isEmpty()) {
            return;
        }
        roomState.overworldRoomStatus[activeRoom.roomId()] |= (byte) ROOM_STATUS_EVENT_2;
    }

    void markHeartContainerCollected() { roomStoryState.markHeartContainerCollected(); }

    void harvestSlimeKeyRewardEvents() {
        if (entityBridge.entityRuntime == null) {
            return;
        }
        List<SlimeKeyRewardEvent> rewards =
            entityBridge.entityRuntime.consumePendingSlimeKeyRewardEvents();
        for (SlimeKeyRewardEvent reward : rewards) {
            entityInputs.entityGoldenLeavesCount = reward.goldenLeavesCount();
            markActiveRoomCompleted();
        }
        entityBridge.pendingSlimeKeyRewardEvents.addAll(rewards);
    }

    void markActiveRoomCompleted() { roomStoryState.markActiveRoomCompleted(); }

    /** Mirrors EntityCheckThrowAtTriggers' $35..$3C shutter-object test. */
    void harvestThrownRoomEventCollisions() { roomPuzzleController.harvestThrownRoomEventCollisions(); }

    /** Executes the source's first common dungeon-event path used by Tail Cave. */
    void tickRoomEvent(int linkEntityX, int linkEntityY) { roomPuzzleController.tickRoomEvent(linkEntityX, linkEntityY); }

    void tickPushedBlockMotion(int linkEntityX, int linkEntityY) { roomPuzzleController.tickPushedBlockMotion(linkEntityX, linkEntityY); }

    boolean pushedBlocksTriggerResolved() { return roomPuzzleController.pushedBlocksTriggerResolved(); }

    void tickIndoorKeyDoorAnimation() { roomPuzzleController.tickIndoorKeyDoorAnimation(); }

    void replaceKeyDoorObjects(int direction, int location) { roomPuzzleController.replaceKeyDoorObjects(direction, location); }

    void replaceBossDoorObjects(int location) { roomPuzzleController.replaceBossDoorObjects(location); }

    boolean killAllEnemiesTriggerResolved() { return roomPuzzleController.killAllEnemiesTriggerResolved(); }

    void tickSwitchButton(int linkEntityX, int linkEntityY) { roomPuzzleController.tickSwitchButton(linkEntityX, linkEntityY); }

    void tickRoomEventChestReveal(int linkEntityX, int linkEntityY) { roomPuzzleController.tickRoomEventChestReveal(linkEntityX, linkEntityY); }

    /** Type-$04 poof calls func_002_5F5C when its countdown reaches four. */
    void tickRoomEventStairReveal() { roomPuzzleController.tickRoomEventStairReveal(); }

    /** Mirrors renderTranscientVFXs' hStaircase leave-and-return state machine. */
    public Warp pollStaircaseWarp(int linkEntityX, int linkEntityY, int linkZ, boolean carryingObject) { return roomTransitionController.pollStaircaseWarp(linkEntityX, linkEntityY, linkZ, carryingObject); }

    void configureStaircase(int location) { roomTransitionController.configureStaircase(location); }

    static boolean withinUnsignedByteRange(int value, int center,
                                                   int bias, int size) {
        return ((value - center + bias) & 0xFF) < size;
    }

    static int roomEventChestTop(int linkEntityX, int linkEntityY) {
        boolean linkNearUpperChest = ((linkEntityY - 0x30 + 0x08) & 0xFF) < 0x10
            && ((linkEntityX - 0x88 + 0x10) & 0xFF) < 0x20;
        return linkNearUpperChest ? 0x30 : 0x20;
    }

    static void requireLength(byte[] values, int expectedLength, String label) {
        if (values == null) {
            throw new NullPointerException(label);
        }
        if (values.length != expectedLength) {
            throw new IllegalArgumentException(label + " must contain " + expectedLength
                + " bytes, got " + values.length);
        }
    }

    int colorShellObjectAt(RoomEntity entity, int relativeOffset) { return roomObjectMutations.colorShellObjectAt(entity, relativeOffset); }

    void writeColorShellObject(RoomEntity entity, int objectId) { roomObjectMutations.writeColorShellObject(entity, objectId); }

    void refreshActiveRoomTilemap() { roomTileRedraw.refreshActiveRoomTilemap(); }

    /** $02:599D selects each shutter; $02:5C04 closes it over eight frames. */
    boolean tickShutterClosing() { return roomPuzzleController.tickShutterClosing(); }

    void openActiveRoomShutterDoors() { roomPuzzleController.openActiveRoomShutterDoors(); }

    void openActiveRoomShutterDoors(boolean persistDirectionalStatus) { roomPuzzleController.openActiveRoomShutterDoors(persistDirectionalStatus); }

    RoomEntityObjectSample entityObjectSample(RoomEntity entity) { return roomCollisionQueries.entityObjectSample(entity); }

    RoomEntityObjectSample entityObjectIntersectionSample(RoomEntity entity) { return roomCollisionQueries.entityObjectIntersectionSample(entity); }

    void applyHookshotBridgeUpdates( List<HookshotBridgeUpdate> updates) { roomTileRedraw.applyHookshotBridgeUpdates(updates); }

    void applyHookshotBridgeTileOverrides() { roomTileRedraw.applyHookshotBridgeTileOverrides(); }

    void applyBombedWallTileOverrides() { roomTileRedraw.applyBombedWallTileOverrides(); }

    void applyBombedBlockTileOverrides() { roomTileRedraw.applyBombedBlockTileOverrides(); }

    void applyBombedCaveDoorTileOverrides() { roomTileRedraw.applyBombedCaveDoorTileOverrides(); }

    static int hookshotObjectAreaIndex(int objectLeft, int objectTop) {
        return RoomConstants.ROOM_OBJECTS_BASE + (objectTop & 0xF0)
            + ((objectLeft & 0xF0) >>> 4);
    }

    static int colorShellObjectIndex(RoomEntity entity) {
        int x = (entity.x() - 0x01) & 0xFF;
        int y = (entity.y() - 0x07) & 0xFF;
        return RoomConstants.ROOM_OBJECTS_BASE + (y & 0xF0) + ((x & 0xF0) >>> 4);
    }

    /**
     * Mirrors the entity collision point selected by bank $03's
     * ApplyEntityCollisionWithObject. The shared room physics query supplies
     * the active overworld/indoor table.
     */
    boolean entityBackgroundCollision(RoomEntity entity, int direction, int nextX, int nextY) { return roomCollisionQueries.entityBackgroundCollision(entity, direction, nextX, nextY); }

    EntityBackgroundCollisionResult entityBackgroundCollisionResult( RoomEntity entity, int direction, int nextX, int nextY) { return roomCollisionQueries.entityBackgroundCollisionResult(entity, direction, nextX, nextY); }

    EntityBackgroundCollisionResult entityBackgroundCollisionResult( RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown) { return roomCollisionQueries.entityBackgroundCollisionResult(entity, direction, nextX, nextY, ignoreHitsCountdown); }

    EntityBackgroundCollisionResult entityBackgroundCollisionResult( RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown, int frameCounter) { return roomCollisionQueries.entityBackgroundCollisionResult(entity, direction, nextX, nextY, ignoreHitsCountdown, frameCounter); }

    EntityBackgroundCollisionResult entityBackgroundCollisionResultForTest( RoomEntity entity, int direction, int nextX, int nextY) { return roomCollisionQueries.entityBackgroundCollisionResultForTest(entity, direction, nextX, nextY); }

    EntityBackgroundCollisionResult entityBackgroundCollisionResultForTest( RoomEntity entity, int direction, int nextX, int nextY, int frameCounter) { return roomCollisionQueries.entityBackgroundCollisionResultForTest(entity, direction, nextX, nextY, frameCounter); }

    /**
     * Mirrors the terrain/status and conveyor portion of bank-$03's
     * ApplyEntityInteractionWithBackground. Terrain physics and the entity
     * options byte stay ROM-backed; the runtime invokes this after the
     * entity-family handler has completed its movement for the frame.
     */
    RoomEntityGroundInteraction.Result entityGroundInteraction( RoomEntity entity, int frameCounter, int previousGroundStatus, int speedZ, boolean sideScrolling) { return roomCollisionQueries.entityGroundInteraction(entity, frameCounter, previousGroundStatus, speedZ, sideScrolling); }

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

    static int groundStatusFor(OverworldCollision.GroundInteractionSample sample) {
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

    static boolean splashAllowed(int options, int previousGroundStatus,
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

    static RoomEntity withEntityPosition(RoomEntity entity, int x, int y) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            entity.z(), -1, false);
    }

    boolean pairoddProjectileObjectCollision(RoomEntity entity) { return roomCollisionQueries.pairoddProjectileObjectCollision(entity); }

    void loadRoomSpecificTilesIfNeeded(int roomId) { roomLoadCoordinator.loadRoomSpecificTilesIfNeeded(roomId); }

    int adjacentOverworldRoomId(int direction) { return roomTransitionController.adjacentOverworldRoomId(direction); }

    RoomScrollNavigation.IndoorRoomDestination adjacentIndoorRoom(int direction) { return roomTransitionController.adjacentIndoorRoom(direction); }

    static int scrollTarget(int direction) { return RoomScrollNavigation.scrollTarget(direction); }
}
