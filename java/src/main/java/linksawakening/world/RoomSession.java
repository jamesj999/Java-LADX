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
    final RoomEntityBridge entityBridge = new RoomEntityBridge();
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

    public boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) {
        if (activeRoom == null || entityBridge.entityRuntime == null
            || !entityBridge.entityRuntime.applyMarinWakeUpPresentation(x, y, spriteVariant)) {
            return false;
        }
        activeRoom.replaceEntities(entityBridge.entityRuntime.snapshot());
        return true;
    }

    /** Tarin's indoor handler updates his facing every 32 frames, even during the bed scene. */
    public void updateHouseTarinPresentation(int frameCounter, int linkX, int linkY) {
        if (activeRoom != null && activeRoom.mapId() == 0x10 && activeRoom.roomId() == 0xA3
            && tarinFlag != 1 && tarinFlag != 2
            && entityBridge.entityRuntime != null && (frameCounter & 0x1F) == 0) {
            entityBridge.entityRuntime.updateHouseTarinPresentation(linkX, linkY);
            activeRoom.replaceEntities(entityBridge.entityRuntime.snapshot());
        }
    }

    /** Supplies the patched EntityInitKeyDropPoint inventory byte. */
    public void setBirdKeyOwned(boolean owned) {
        hasBirdKey = owned;
    }

    /** Supplies wHasTailKey to the active owl-event handler. */
    public void setTailKeyOwned(boolean owned) {
        hasTailKey = owned;
        if (entityBridge.entityRuntime != null) {
            entityBridge.entityRuntime.setTailKeyOwned(owned);
        }
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
    public void initializeNewGameWorldState() {
        Arrays.fill(roomState.overworldRoomStatus, (byte) 0);
        Arrays.fill(roomState.indoorARoomStatus, (byte) 0);
        Arrays.fill(roomState.indoorBRoomStatus, (byte) 0);
        Arrays.fill(roomState.colorDungeonRoomStatus, (byte) 0);
        shouldGetLostInMysteriousWoods = false;
        recentRoomEntityClears.reset();
        Arrays.fill(roomState.dungeonProgressFlags, (byte) 0);
        roomState.dungeonItemState.reset();
        followingNpcState = FollowingNpcState.none();
        followingNpcRoomNeedsSync = true;
        followingLinkPositionHistory.fill(0x08, 0x10, 0, 0);
        hasBirdKey = false;
        hasTailKey = false;
        bowWowState = 0;
        richardSpokenFlag = 0;
        tarinFlag = 0;
        playerShieldLevel = 0;
        entityInputs.entityGoldenLeavesCount = 0;
        enemyDropCounters = new EnemyDropResolver.CounterState(0, 0);
        boomerangSoundCounter = 0;
        boomerangSoundClockProvided = false;
        dungeonTitleMessageCountdown = 0;
        switchBlocksState = 0;
        switchableObjectAnimationStage = 0;
        switchButtonPressed = 0;
        currentOverworldTilesetId = W_TILESET_NO_UPDATE;
        clearTransientRoomState();
    }

    /** Restores the room-status WRAM tables loaded by the source save path. */
    public void restoreRoomStatuses(byte[] overworldStatus, byte[] indoorAStatus,
                                    byte[] indoorBStatus, byte[] colorDungeonStatus) {
        roomState.restoreRoomStatuses(overworldStatus, indoorAStatus, indoorBStatus, colorDungeonStatus);
        if (entityBridge.entityRuntime != null && activeRoom != null) {
            entityBridge.entityRuntime.setEntityRoomStatus(activeRoomStatusFlags());
            entityBridge.entityRuntime.setTailKeyOwned(hasTailKey);
        }
    }

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
        if (entityBridge.entityRuntime != null) {
            entityBridge.entityRuntime.setSpriteSelection(updated.spriteSelection());
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

    /** Mirrors wIsBowWowFollowingLink, including BOW_WOW_KIDNAPPED=$80. */
    public int bowWowState() {
        return bowWowState & 0xFF;
    }

    public void setBowWowState(int state) {
        if ((state & ~0xFF) != 0) {
            throw new IllegalArgumentException("BowWow state must be an unsigned byte");
        }
        bowWowState = state;
        if (entityBridge.entityRuntime != null) {
            entityBridge.entityRuntime.setBowWowState(state);
        }
    }

    public int richardSpokenFlag() {
        return richardSpokenFlag & 0xFF;
    }

    public void setRichardSpokenFlag(int flag) {
        if ((flag & ~0xFF) != 0) {
            throw new IllegalArgumentException("Richard spoken flag must be an unsigned byte");
        }
        richardSpokenFlag = flag;
    }

    /** Mirrors the unsigned-byte world progression flag at wTarinFlag. */
    public int tarinFlag() {
        return tarinFlag & 0xFF;
    }

    public void setTarinFlag(int flag) {
        if ((flag & ~0xFF) != 0) {
            throw new IllegalArgumentException("Tarin flag must be an unsigned byte");
        }
        tarinFlag = flag;
    }

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
    public void setChestPlayerLevels(int shieldLevel, int swordLevel,
                                     int powerBraceletLevel) {
        if ((shieldLevel & ~0xFF) != 0 || (swordLevel & ~0xFF) != 0
            || (powerBraceletLevel & ~0xFF) != 0) {
            throw new IllegalArgumentException("Player upgrade levels must be unsigned bytes");
        }
        playerShieldLevel = shieldLevel;
        if (entityBridge.entityRuntime != null) {
            entityBridge.entityRuntime.setChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel);
            if (activeRoom != null) {
                activeRoom.replaceEntities(entityBridge.entityRuntime.snapshot());
            }
        }
    }

    /** Supplies the two WRAM values checked by SleepyToadstoolEntityHandler. */
    public void setToadstoolPlayerState(boolean hasToadstool, int magicPowderCount) { entityInputs.setToadstoolPlayerState(hasToadstool, magicPowderCount); }

    public void setEntityDefaultMusicTrack(int trackId) {
        if ((trackId & ~0xFF) != 0) {
            throw new IllegalArgumentException("Default music track must be an unsigned byte");
        }
        entityDefaultMusicTrack = trackId;
        updateWitchEnvironment();
    }

    void updateWitchEnvironment() {
        if (entityBridge.entityRuntime != null) {
            entityBridge.entityRuntime.setWitchEnvironment(indoorTorchPaletteEffect.effectAddress(),
                indoorTorchPaletteEffect.paletteDataFlags(), entityDefaultMusicTrack);
        }
    }

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
    public boolean shouldGetLostInMysteriousWoods() {
        return shouldGetLostInMysteriousWoods;
    }

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
        return entityBridge.entityRuntime == null
            ? RoomEntityRuntime.LiftedEntityState.none()
            : entityBridge.entityRuntime.liftedEntityState();
    }

    /** Throws the currently held object using Link's Java direction order. */
    public boolean throwLiftedEntity(int linkDirection) {
        if (entityBridge.entityRuntime == null) {
            return false;
        }
        boolean thrown = entityBridge.entityRuntime.throwLiftedEntity(
            romDirectionForProjectileCollision(linkDirection));
        if (thrown) {
            activeRoom.replaceEntities(entityBridge.entityRuntime.snapshot());
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
        if (linkAirborne || linkPushing || activeRoom == null || entityBridge.entityRuntime == null) {
            return false;
        }
        int slot = entityBridge.entityRuntime.spawnHookshotChain(linkEntityX, linkEntityY, linkEntityZ,
            romDirection);
        if (slot < 0) {
            return false;
        }
        activeRoom.replaceEntities(entityBridge.entityRuntime.snapshot());
        return true;
    }

    /** Mirrors UseBoomerang's player-projectile spawn bridge. */
    public boolean fireBoomerang(int linkEntityX, int linkEntityY, int linkEntityZ,
                                 int romDirection, int pressedButtonsMask) {
        if (activeRoom == null || entityBridge.entityRuntime == null) {
            return false;
        }
        int slot = entityBridge.entityRuntime.spawnBoomerang(linkEntityX, linkEntityY, linkEntityZ,
            romDirection, pressedButtonsMask);
        if (slot < 0) {
            return false;
        }
        activeRoom.replaceEntities(entityBridge.entityRuntime.snapshot());
        return true;
    }

    /** Mirrors the level-two branch of UseSword's player-projectile spawn. */
    public boolean fireSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ,
                                 int romDirection) {
        if (activeRoom == null || entityBridge.entityRuntime == null) {
            return false;
        }
        int slot = entityBridge.entityRuntime.spawnSwordBeam(linkEntityX, linkEntityY, linkEntityZ,
            romDirection);
        if (slot < 0) {
            return false;
        }
        activeRoom.replaceEntities(entityBridge.entityRuntime.snapshot());
        return true;
    }

    /** Mirrors the delayed UseMagicRod player-projectile spawn bridge. */
    public boolean fireMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ,
                                        int romDirection) {
        return fireMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    public boolean fireMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ,
                                        int romDirection, boolean pieceOfPower) {
        if (activeRoom == null || entityBridge.entityRuntime == null) {
            return false;
        }
        int slot = entityBridge.entityRuntime.spawnMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ,
            romDirection, pieceOfPower);
        if (slot < 0) {
            return false;
        }
        activeRoom.replaceEntities(entityBridge.entityRuntime.snapshot());
        return true;
    }

    /** Mirrors the delayed UseMagicPowder sprinkle-entity spawn bridge. */
    public boolean sprinkleMagicPowder(int linkEntityX, int linkEntityY, int linkEntityZ,
                                       int romDirection) {
        if (activeRoom == null || entityBridge.entityRuntime == null) {
            return false;
        }
        int slot = entityBridge.entityRuntime.spawnMagicPowderSprinkle(
            linkEntityX, linkEntityY, linkEntityZ, romDirection);
        if (slot < 0) {
            return false;
        }
        activeRoom.replaceEntities(entityBridge.entityRuntime.snapshot());
        return true;
    }

    /**
     * Mirrors {@code UseShovel}: the item always enters its 24-frame pose
     * window when Link is grounded, while the adjacent-cell probe determines
     * whether the sound is a poke or a normal dig.
     *
     * <p>The coordinates are the ROM's {@code hLinkPositionX/Y} values. The
     * caller supplies ROM direction order: RIGHT, LEFT, UP, DOWN.</p>
     */
    public ShovelStartResult startShovel(int linkEntityX, int linkEntityY,
                                         int romDirection, boolean linkAirborne) {
        if (romDirection < 0 || romDirection > 3) {
            throw new IllegalArgumentException("ROM shovel direction out of range: "
                + romDirection);
        }
        if (activeRoom == null || linkAirborne || shovelUseState != 0) {
            return new ShovelStartResult(false, true, -1);
        }
        ShovelProbe probe = probeShovel(linkEntityX, linkEntityY, romDirection);
        shovelUseState = SHOVEL_STATE_ACTIVE;
        return new ShovelStartResult(true, !probe.valid(), probe.location());
    }

    /** Applies the ROM shovel timer's delayed room mutation and finish path. */
    public void advanceShovel(int linkEntityX, int linkEntityY, int romDirection, int timer) {
        if (activeRoom == null || shovelUseState == 0) {
            return;
        }
        int normalizedTimer = timer & 0xFF;
        if (normalizedTimer == SHOVEL_DIG_TIMER) {
            ShovelProbe probe = probeShovel(linkEntityX, linkEntityY, romDirection);
            if (!probe.valid()) {
                return;
            }
            shovelUseState = SHOVEL_STATE_DUG;
            writeShovelHole(probe);
            pendingShovelDrop = new PendingShovelDrop(
                probe.objectLeft(), probe.objectTop(), linkEntityX, linkEntityY,
                activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
                    || activeRoom.roomId() != 0x0E);
        } else if (normalizedTimer == SHOVEL_FINISH_TIMER) {
            if (shovelUseState == SHOVEL_STATE_DUG && followingNpcState.marinFollowing()) {
                entityBridge.pendingRoomDialogRequests.add(new DialogRequest(
                    SHOVEL_DIALOG_TABLE, SHOVEL_DIALOG_ID));
            }
            shovelUseState = 0;
        }
    }

    /** Resolves the ROM shovel animation table through Java's Link direction order. */
    public int shovelAnimationState(int javaDirection, int timer) {
        return romTables.shovelAnimationState(timer,
            romDirectionForProjectileCollision(javaDirection));
    }

    ShovelProbe probeShovel(int linkEntityX, int linkEntityY, int romDirection) {
        if (activeRoom == null || activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL) {
            return ShovelProbe.invalid();
        }

        // func_002_4D20 subtracts Link's sprite-center/bottom origin before
        // masking to the room-object grid. These are hLinkPositionX/Y, not
        // Java's top-left pixel coordinates.
        int objectLeft = (linkEntityX + SHOVEL_TARGET_X[romDirection] - 0x08) & 0xF0;
        int objectTop = (linkEntityY + SHOVEL_TARGET_Y[romDirection] - 0x10) & 0xF0;
        int cellX = objectLeft >>> 4;
        int cellY = objectTop >>> 4;
        if (cellX < 0 || cellX >= RoomConstants.OBJECTS_PER_ROW
            || cellY < 0 || cellY >= RoomConstants.OBJECTS_PER_COLUMN) {
            return ShovelProbe.invalid();
        }

        int location = objectTop | cellX;
        int objectId = objectAtRoomLocation(location);
        int physicsTable = activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            ? RomTables.PHYSICS_TABLE_OVERWORLD
            : activeRoom.mapId() == 0xFF
                ? RomTables.PHYSICS_TABLE_INDOORS2
                : RomTables.PHYSICS_TABLE_INDOORS1;
        boolean valid = romTables.objectPhysicsFlag(physicsTable, objectId) == 0;
        if (activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            valid &= objectId != OBJECT_SHOVEL_BLOCKER_A
                && objectId != OBJECT_SHOVEL_BLOCKER_B
                && objectId != OBJECT_SHOVEL_BLOCKER_C;
        } else {
            valid &= objectId == OBJECT_SHOVEL_INDOOR_DIGGABLE;
        }
        return new ShovelProbe(valid, location, objectLeft, objectTop);
    }

    void writeShovelHole(ShovelProbe probe) {
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + probe.location();
        int[] objects = activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return;
        }
        objects[areaIndex] = OBJECT_SHOVEL_HOLE;
        if (activeRoom.renderValues() != null && areaIndex < activeRoom.renderValues().length) {
            activeRoom.renderValues()[areaIndex] = OBJECT_SHOVEL_HOLE;
        }
        if (activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            // On GBC the mutable WRAM2 overlay is the render source for both
            // tile ids and attributes; rebuilding from the immutable ROM
            // overlay here would immediately undo the hole.
            overworldBushInteraction.refreshRoomObjectCell(
                activeRoom.roomId(), probe.location(), objects, activeRoom.renderValues(),
                activeRoom.gbcOverlay(), activeRoom.tileIds(), activeRoom.tileAttrs());
            refreshOverworldCollisionAfterObjectMutation();
        } else {
            refreshActiveRoomTilemap();
            overworldCollision.setRoom(activeRoom.roomObjectsArea());
        }
    }

    /**
     * Mirrors the ordinary player-bomb placement bridge. The item has already
     * applied PlaceBomb's inventory ordering; this method owns the room/runtime
     * allocation and immutable snapshot refresh.
     */
    public boolean placeBomb(int linkEntityX, int linkEntityY, int linkEntityZ,
                             int romDirection) {
        if (activeRoom == null || entityBridge.entityRuntime == null) {
            return false;
        }
        int slot = entityBridge.entityRuntime.spawnBomb(linkEntityX, linkEntityY, linkEntityZ, romDirection);
        if (slot < 0) {
            return false;
        }
        activeRoom.replaceEntities(entityBridge.entityRuntime.snapshot());
        return true;
    }

    /** Returns whether the last successful bomb placement queued the top-view bump jingle. */
    public boolean lastBombPlacementPlayedBump() {
        return entityBridge.entityRuntime != null && entityBridge.entityRuntime.lastBombPlacementPlayedBump();
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
        if (activeRoom == null || entityBridge.entityRuntime == null) {
            return new ArrowShotResult(false, false);
        }
        int slot = entityBridge.entityRuntime.spawnArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection,
            pieceOfPower);
        if (slot < 0) {
            return new ArrowShotResult(false, false);
        }
        boolean playWhoosh = entityBridge.entityRuntime.lastArrowShotPlayedWhoosh();
        activeRoom.replaceEntities(entityBridge.entityRuntime.snapshot());
        return new ArrowShotResult(true, playWhoosh);
    }

    /** Returns the modeled player-projectile count used by the item gates. */
    public int activeProjectileCount() {
        return entityBridge.entityRuntime == null ? 0 : entityBridge.entityRuntime.activeProjectileCount();
    }

    /** Returns whether the live room currently owns an ordinary bomb entity. */
    public boolean bombActive() {
        return entityBridge.entityRuntime != null && entityBridge.entityRuntime.bombActive();
    }

    int bombDirectionForTest(int slot) {
        return entityBridge.entityRuntime == null ? 0xFF : entityBridge.entityRuntime.bombDirection(slot);
    }

    /** Returns whether the live room currently owns entity {@code $03}. */
    public boolean hookshotActive() {
        return entityBridge.entityRuntime != null && entityBridge.entityRuntime.hookshotActive();
    }

    /** Returns whether the live room currently owns the boomerang entity. */
    public boolean boomerangActive() {
        return entityBridge.entityRuntime != null && entityBridge.entityRuntime.boomerangActive();
    }

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
    public void advanceBoomerangSoundClock(boolean worldGameplay) {
        if (worldGameplay) {
            boomerangSoundCounter = (boomerangSoundCounter + 1)
                % RoomEntityRuntime.BOOMERANG_SFX_COUNTER_PERIOD;
        }
        // Main may run several world frames without an entity pass during a
        // transition. The next pass must use that clock without ticking twice.
        boomerangSoundClockProvided = true;
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

    public void tickEntities(int frameCounter, int linkEntityX, int linkEntityY,
                             int linkEntityZ, boolean linkAirborne, int linkDirection) {
        tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY,
            linkEntityZ, linkAirborne, EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE,
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

    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
                             int frameCounter, int linkEntityX, int linkEntityY,
                             int linkEntityZ, boolean linkAirborne,
                             int linkMotionState, int linkDirection,
                             boolean usingShield) {
        return tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY,
            linkEntityZ, linkAirborne, linkMotionState, linkDirection, 0, usingShield,
            1, 0, false, 0, 0, 0, 0, 0, 0);
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
        return tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY,
            linkEntityZ, false, linkMotionState, linkDirection, collisionType, usingShield,
            shieldLevel, invincibilityCounter, swordCollisionActive, swordX, swordWidth,
            swordY, swordHeight, linkSpeedX, linkSpeedY);
    }

    /** Rich live path with Link's explicit airborne state. */
    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
                             int frameCounter, int linkEntityX, int linkEntityY,
                             int linkEntityZ, boolean linkAirborne,
                             int linkMotionState, int linkDirection,
                             int collisionType, boolean usingShield, int shieldLevel,
                             int invincibilityCounter, boolean swordCollisionActive,
                             int swordX, int swordWidth, int swordY, int swordHeight,
                             int linkSpeedX, int linkSpeedY) {
        followingLinkX = linkEntityX & 0xFF;
        followingLinkY = linkEntityY & 0xFF;
        followingLinkZ = linkEntityZ & 0xFF;
        followingLinkDirection = linkDirection & 0xFF;
        currentLinkMotionState = linkMotionState & 0xFF;
        shouldGetLostInMysteriousWoods = false;
        if (activeRoom == null || entityBridge.entityRuntime == null) {
            return List.of();
        }
        if (!boomerangSoundClockProvided) {
            advanceBoomerangSoundClock(true);
        }
        entityBridge.entityRuntime.setBoomerangSoundCounterForFrame(boomerangSoundCounter);
        boomerangSoundClockProvided = false;
        indoorTorchPaletteEffect.tick(entityInputs.entityDialogActive, false,
            currentLinkMotionState, activeRoom.mapId());
        entityBridge.entityRuntime.setTorchPaletteTransitionState(indoorTorchPaletteEffect.targetAddress(),
            indoorTorchPaletteEffect.transitionActive());
        copyIndoorTorchPalettesToActiveRoom();
        tickOcarinaAnimationHandler();
        entityBridge.entityRuntime.setSwitchBlockAnimationActive(
            SwitchBlockAnimation.isAnimating(switchableObjectAnimationStage));
        entityBridge.entityRuntime.setDialogActive(entityInputs.entityDialogActive);
        entityBridge.entityRuntime.setTalkState(
            entityInputs.entityInventoryAppearing, entityInputs.entityDialogCooldown, entityInputs.entityWindowY);
        entityBridge.entityRuntime.setDialogAskSelectionIndex(entityInputs.entityDialogAskSelectionIndex);
        entityBridge.entityRuntime.setActiveMusic(entityInputs.entityMusicActive);
        entityBridge.entityRuntime.setBowWowState(bowWowState);
        entityBridge.entityRuntime.setLinkVelocityZ(entityInputs.entityLinkVelocityZ);
        entityBridge.entityRuntime.setOwlInstrumentFlags(
            Byte.toUnsignedInt(roomState.dungeonProgressFlags[0]),
            Byte.toUnsignedInt(roomState.dungeonProgressFlags[1]));
        entityBridge.entityRuntime.setActionButtonsHeld(entityInputs.actionButtonAHeld, entityInputs.actionButtonBHeld);
        int statueMapId = activeRoom.mapId();
        entityBridge.entityRuntime.setOwlStatueDialogId(
            activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
                && (statueMapId < 8 || statueMapId == 0xFF)
                ? owlStatueDialogResolver.globalDialogId(statueMapId, activeRoom.roomId(),
                    roomState.dungeonItemState.currentFlag(DungeonItemState.STONE_BEAK_INDEX) != 0)
                : -1);
        entityBridge.entityRuntime.setJoypadHeld(entityInputs.joypadHeld);
        entityBridge.entityRuntime.setPressedButtonsMask(entityInputs.entityPressedButtonsMask);
        entityBridge.entityRuntime.setPowerBraceletButtonHeld(entityInputs.powerBraceletButtonHeld);
        entityBridge.entityRuntime.setBombButtonHeld(entityInputs.bombButtonHeld);
        entityBridge.entityRuntime.setLiftedLinkC13B(followingEntityYOffset);
        entityBridge.entityRuntime.setBooBuddyTriggerCount(roomTriggerCount);
        updateWitchEnvironment();
        entityBridge.entityRuntime.setOcarinaPlayback(ocarinaPlaybackCountdown,
            ocarinaSongFlags, selectedSongIndex, ocarinaAnimationCounter,
            ocarinaAnimationPhase);
        entityBridge.entityRuntime.setGoldenLeavesCount(entityInputs.entityGoldenLeavesCount);
        entityBridge.entityRuntime.setSecretSeashellPegasusCollisionState(
            entityInputs.secretSeashellScreenShakeActive, entityInputs.secretSeashellPegasusCollisionActive,
            entityInputs.secretSeashellPegasusCollisionX, entityInputs.secretSeashellPegasusCollisionY);
        // rLY is not a meaningful value in the host renderer. Keep the
        // non-emulator policy explicit while preserving the ROM seed update.
        entityRandomByteSource.beginFrame(frameCounter & 0xFF, 0);
        if (pendingShovelDrop != null) {
            PendingShovelDrop drop = pendingShovelDrop;
            pendingShovelDrop = null;
            entityBridge.entityRuntime.spawnShovelDrop(
                drop.objectLeft(), drop.objectTop(), drop.linkEntityX(), drop.linkEntityY(),
                drop.allowDrop());
        }
        List<EntityProjectileEvent> events = entityBridge.entityRuntime.tick(new EntityFrameContext(
            frameCounter, linkEntityX, linkEntityY, collisionType & 0xFF,
            entityRandomByteSource,
            this::entityBackgroundCollision, this::pairoddProjectileObjectCollision,
            followingLinkPositionHistory, followingLinkZ, linkAirborne,
            followingLinkDirection, followingEntityYOffset,
            new EnemyProjectileCollision.LinkState(
                linkEntityX, linkEntityY, linkEntityZ, linkMotionState,
                romDirectionForProjectileCollision(linkDirection), usingShield, shieldLevel,
                invincibilityCounter), swordCollisionActive, swordX, swordWidth,
            swordY, swordHeight, linkSpeedX, linkSpeedY));
        for (DungeonWarpRequest request
            : entityBridge.entityRuntime.consumePendingDungeonWarpRequests()) {
            linksawakening.world.entity.event.WarpDestination destination = request.destination();
            pendingDungeonWarp = new Warp(Warp.CATEGORY_INDOOR, destination.mapId(),
                destination.roomId(), destination.x(), destination.y(), 0x34);
        }
        shouldGetLostInMysteriousWoods = entityBridge.entityRuntime.shouldGetLostInMysteriousWoods();
        int runtimeBowWowState = entityBridge.entityRuntime.bowWowState();
        if (runtimeBowWowState != bowWowState) {
            bowWowState = runtimeBowWowState;
            followingNpcState = new FollowingNpcState(
                followingNpcState.roosterFollowing(),
                followingNpcState.ghostFollowingState(),
                followingNpcState.marinFollowing(), runtimeBowWowState == 0x01,
                followingNpcState.instrument4Flags(),
                followingNpcState.powerBraceletLevel(), followingNpcState.db10());
            // The rescue/return handler changes the existing entity in place.
            // CreateFollowingNpcEntity runs on room entry, not on this quest
            // flag write: respawning here teleports BowWow onto Link and
            // reconstructs every other entity's runtime state.
        }
        if (ocarinaPlaybackCountdown > 0) {
            ocarinaPlaybackCountdown--;
            if (ocarinaPlaybackCountdown == 0) {
                finishOcarinaPlayback();
            }
        }
        List<BombExplosionEvent> bombExplosionEvents = entityBridge.entityRuntime.consumeBombExplosionEvents();
        entityBridge.pendingBombExplosionEvents.clear();
        entityBridge.pendingBombExplosionEvents.addAll(bombExplosionEvents);
        List<ChestRewardEvent> chestRewards =
            entityBridge.entityRuntime.consumePendingChestRewardEvents();
        for (ChestRewardEvent reward : chestRewards) {
            if (reward.itemType() >= ChestContentsTable.CHEST_MAP
                && reward.itemType() <= ChestContentsTable.CHEST_SMALL_KEY) {
                roomState.dungeonItemState.incrementCurrentFlag(reward.itemType());
            }
        }
        entityBridge.pendingChestRewardEvents.addAll(chestRewards);
        harvestKeyQuicksandEvents();
        harvestKeyRewardEvents();
        harvestSlimeKeyRewardEvents();
        harvestThrownRoomEventCollisions();
        harvestHeartContainerRewards();
        entityBridge.pendingMadamMeowMeowFullHealRequests.addAll(
            entityBridge.entityRuntime.consumePendingMadamMeowMeowFullHealRequests());
        harvestSwordPickupRewards();
        harvestToadstoolRewards();
        harvestInstrumentRewards();
        harvestInstrumentCompletions();
        harvestRichardProgressEvents();
        harvestWitchEvents();
        harvestOwlEventCompletions();
        for (RoomStatusPersistenceRequest request
            : entityBridge.entityRuntime.consumePendingRoomStatusPersistenceRequests()) {
            if (activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                roomState.overworldRoomStatus[activeRoom.roomId()] |= (byte) request.roomStatusMask();
            }
            tarinFlag = request.tarinFlag();
        }
        if (entityBridge.entityRuntime.consumePendingSwitchBlockAnimationRequest()
            && switchableObjectAnimationStage == 0) {
            switchableObjectAnimationStage = 0x01;
        }
        enemyDropCounters = entityBridge.entityRuntime.enemyDropCounters();
        if (transientVfxSystem != null) {
            for (TransientVfxRequest request
                : entityBridge.entityRuntime.transientVfxRequests()) {
                transientVfxSystem.spawn(request.type(), request.worldX(), request.worldY(),
                    request.variant());
            }
        }
        int clearedMask = entityBridge.entityRuntime.consumePendingClearedEntityMask();
        if (clearedMask != 0) {
            recentRoomEntityClears.addMask(activeRoom.roomId(), clearedMask);
        }
        int roomStatusMask = entityBridge.entityRuntime.consumePendingRoomStatusMask();
        if (roomStatusMask != 0 && activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            byte[] status = indoorStatusTableForMap(activeRoom.mapId());
            status[activeRoom.roomId()] |= (byte) roomStatusMask;
            entityBridge.entityRuntime.setEntityRoomStatus(
                Byte.toUnsignedInt(status[activeRoom.roomId()]));
        }
        applyHookshotBridgeUpdates(entityBridge.entityRuntime.hookshotBridgeUpdates());
        activeRoom.replaceEntities(entityBridge.entityRuntime.snapshot());
        applyBombObjectInteractions(bombExplosionEvents);
        applyBoomerangObjectInteractions(entityBridge.entityRuntime.boomerangObjectRequests());
        applyMagicRodObjectInteractions(entityBridge.entityRuntime.magicRodObjectRequests());
        applyMagicPowderObjectInteractions(entityBridge.entityRuntime.magicPowderObjectRequests());
        activeRoom.replaceEntities(entityBridge.entityRuntime.snapshot());
        tickPushedBlockMotion(linkEntityX, linkEntityY);
        tickRoomEvent(linkEntityX, linkEntityY);
        return events;
    }

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
            consumeWarpLinkStateRequests() {
        List<WarpLinkStateRequest> requests = entityBridge.entityRuntime == null
            ? List.of() : entityBridge.entityRuntime.consumePendingWarpLinkStateRequests();
        if (!requests.isEmpty()) {
            ocarinaPlaybackCountdown = 0;
            ocarinaAnimationCounter = 0;
            ocarinaAnimationPhase = 0;
            entityInputs.entityDialogActive = false;
            entityInputs.entityDialogCooldown = 0;
            entityInputs.entityInventoryAppearing = false;
        }
        return requests;
    }

    /** Returns and clears Hinox grab/throw writes for the last entity tick. */
    public List<HinoxLinkEffectRequest>
            consumeHinoxLinkEffectRequests() { return entityBridge.consumeHinoxLinkEffectRequests(); }

    /** Returns and clears explicit ROM Link fallen-pose requests. */
    public List<LinkFallenPoseRequest>
            consumeLinkFallenPoseRequests() { return entityBridge.consumeLinkFallenPoseRequests(); }

    /** Returns and clears a motion block emitted by a world handler rather than an entity. */
    public void setFreeMovementMode(boolean enabled) { entityBridge.setFreeMovementMode(enabled); }

    public boolean consumeWorldLinkMotionBlockRequest() {
        boolean pending = worldLinkMotionBlockPending;
        worldLinkMotionBlockPending = false;
        return pending;
    }

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
        if (activeRoom == null || entityBridge.entityRuntime == null) {
            return List.of();
        }
        List<EntityCombatEvent> events = entityBridge.entityRuntime.resolveCombat(
            frameCounter, linkEntityX, linkEntityY, linkAirborne, linkVerticalVelocity,
            linkInteractive, swordCollisionActive, swordX, swordWidth, swordY, swordHeight,
            attackContext);
        activeRoom.replaceEntities(entityBridge.entityRuntime.snapshot());
        return events;
    }

    public int clearEntity(int slot) {
        if (activeRoom == null || entityBridge.entityRuntime == null) {
            return 0;
        }
        int mask = entityBridge.entityRuntime.clearEntity(slot);
        if (mask != 0) {
            recentRoomEntityClears.addMask(activeRoom.roomId(), mask);
        }
        activeRoom.replaceEntities(entityBridge.entityRuntime.snapshot());
        return mask;
    }

    /**
     * Applies the ROM pickup collision cadence to the active room entities.
     * Item-specific effects are returned to the gameplay layer so room state
     * and persistent entity state stay separate.
     */
    /** Link coordinates use ROM entity origins, not sprite top-left; direction uses Link constants. */
    public EntityPickupEvent collectEntityIfNeeded(int frameCounter,
                                                   int linkEntityX,
                                                   int linkEntityY,
                                                   boolean linkAirborne,
                                                   boolean linkInteractive) {
        return collectEntityIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne,
            linkInteractive, followingLinkDirection, followingLinkZ);
    }

    public EntityPickupEvent collectEntityIfNeeded(int frameCounter,
                                                   int linkEntityX,
                                                   int linkEntityY,
                                                   boolean linkAirborne,
                                                   boolean linkInteractive,
                                                   int linkDirection) {
        return collectEntityIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne,
            linkInteractive, linkDirection, followingLinkZ);
    }

    public EntityPickupEvent collectEntityIfNeeded(int frameCounter,
                                                   int linkEntityX,
                                                   int linkEntityY,
                                                   boolean linkAirborne,
                                                   boolean linkInteractive,
                                                   int linkDirection,
                                                   int linkZ) {
        if (activeRoom == null || entityBridge.entityRuntime == null) {
            return null;
        }
        followingLinkZ = linkZ & 0xFF;
        followingLinkDirection = linkDirection & 0xFF;
        EntityPickupEvent event = entityBridge.entityRuntime.collectIfNeeded(
            frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive,
            linkDirection, linkZ);
        if (event == null) {
            return null;
        }
        if (event.persistentClearMask() != 0) {
            recentRoomEntityClears.addMask(
                activeRoom.roomId(), event.persistentClearMask());
        }
        if (event.type() == 0x30 && activeRoom.roomId() == 0x7C
            && activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            // PickDroppableKey sets the Angler's Tunnel source room flag before
            // it starts the held-item transition.
            roomState.indoorARoomStatus[0x69] |= 0x10;
        }
        if (event.type() == ENTITY_DROPPABLE_SECRET_SEASHELL) {
            // PickSecretSeashell opens Dialog0EF and completes the room after
            // the shared pickup collision has cleared the source entity.
            markActiveRoomCompleted();
            entityBridge.pendingRoomDialogRequests.add(new DialogRequest(0, 0xEF));
            entityBridge.pendingRoomEntityEvents.add(new EntityCombatEvent(
                event.slot(), event.type(), 0, false,
                EntityCombatEvent.SoundChannel.WAVE, 0x01));
        }
        harvestKeyRewardEvents();
        activeRoom.replaceEntities(entityBridge.entityRuntime.snapshot());
        return event;
    }

    public RoomBoundaryState boundaryState(int linkX, int linkY) {
        return boundaryState(linkX, linkY, false);
    }

    public RoomBoundaryState boundaryState(int linkX, int linkY, boolean linkAirborne) {
        return boundaryState(linkX, linkY, linkAirborne, 0);
    }

    public RoomBoundaryState boundaryState(int linkX, int linkY, boolean linkAirborne,
                                           int linkPhysicsModifier) {
        boolean sideViewEntityActive = activeRoom.entities() != null
            && !activeRoom.entities().slots().isEmpty()
            && activeRoom.entities().slots().get(0).status() != EntityStatus.DISABLED;
        return new RoomBoundaryState(
            activeRoom.mapCategory(),
            activeRoom.roomId(),
            activeRoom.indoorHasSouthEntrance(),
            activeRoom.hasWarps(),
            closedShutterMask,
            linkX,
            linkY,
            activeRoom.mapId(),
            linkAirborne,
            linkPhysicsModifier,
            sideViewEntityActive
        );
    }

    void setActiveRoom(LoadedRoom room) { roomLoadCoordinator.setActiveRoom(room); }

    void setActiveRoom(LoadedRoom room, int[][] objectPalettes) { roomLoadCoordinator.setActiveRoom(room, objectPalettes); }

    void initializeSwitchBlockTiles() { roomLoadCoordinator.initializeSwitchBlockTiles(); }

    void synchronizeSwitchBlockCollisionState() { roomLoadCoordinator.synchronizeSwitchBlockCollisionState(); }

    void synchronizeFollowingNpcEntitiesIfNeeded() {
        if (!followingNpcRoomNeedsSync || activeRoom == null || entityBridge.entityRuntime == null
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
        entityBridge.entityRuntime = RoomEntityRuntime.from(
            result.snapshot(), activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD,
            entityRandomByteSource, entitySpriteHandlerCatalog, enemyCombatTables,
            chestContentsTable, romTables, romData);
        entityBridge.entityRuntime.setColorShellWorld(colorShellWorld);
        entityBridge.entityRuntime.setFollowingNpcState(followingNpcState);
        entityBridge.entityRuntime.setEntityMapId(activeRoom.mapId());
        entityBridge.entityRuntime.setEntityRoomId(activeRoom.roomId());
        entityBridge.entityRuntime.setDungeonMinibossReady(activeRoom.mapId() >= 0
            && activeRoom.mapId() < roomState.dungeonProgressFlags.length
            && (roomState.dungeonProgressFlags[activeRoom.mapId()] & 0x01) != 0);
        entityBridge.entityRuntime.setOwlDialogResolver(owlEventDialogResolver::globalDialogId);
        entityBridge.entityRuntime.setOwlDefaultMusicResolver(owlEventDialogResolver::defaultMusicTrack);
        entityBridge.entityRuntime.setGroundInteraction(this::entityGroundInteraction);
        entityBridge.entityRuntime.setBackgroundInteraction(entityBackgroundInteraction);
        entityBridge.entityRuntime.setObjectQuery(this::entityObjectSample);
        entityBridge.entityRuntime.setObjectIntersectionQuery(this::entityObjectIntersectionSample);
        entityBridge.entityRuntime.setGroundInteractionSideScrolling(
            activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL);
        entityBridge.entityRuntime.setActionButtonsHeld(entityInputs.actionButtonAHeld, entityInputs.actionButtonBHeld);
        entityBridge.entityRuntime.setTalkState(
            entityInputs.entityInventoryAppearing, entityInputs.entityDialogCooldown, entityInputs.entityWindowY);
        entityBridge.entityRuntime.setJoypadHeld(entityInputs.joypadHeld);
        entityBridge.entityRuntime.setPressedButtonsMask(entityInputs.entityPressedButtonsMask);
        entityBridge.entityRuntime.setPowerBraceletButtonHeld(entityInputs.powerBraceletButtonHeld);
        entityBridge.entityRuntime.setBombButtonHeld(entityInputs.bombButtonHeld);
        entityBridge.entityRuntime.setLiftedLinkC13B(followingEntityYOffset);
        entityBridge.entityRuntime.setEntityRoomStatus(activeRoomStatusFlags());
        entityBridge.entityRuntime.setDungeonMinibossReady(activeRoom.mapId() >= 0
            && activeRoom.mapId() < roomState.dungeonProgressFlags.length
            && (roomState.dungeonProgressFlags[activeRoom.mapId()] & 0x01) != 0);
        entityBridge.entityRuntime.setTailKeyOwned(hasTailKey);
        entityBridge.entityRuntime.setSecretSeashellPegasusCollisionState(
            entityInputs.secretSeashellScreenShakeActive, entityInputs.secretSeashellPegasusCollisionActive,
            entityInputs.secretSeashellPegasusCollisionX, entityInputs.secretSeashellPegasusCollisionY);
        configureEnemyDropRuntime();
    }

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

    void markHeartContainerCollected() {
        if (activeRoom == null) {
            return;
        }
        byte[] activeStatus = indoorStatusTableForMap(activeRoom.mapId());
        activeStatus[activeRoom.roomId()] |= (byte) ROOM_STATUS_EVENT_2;
        if (activeRoom.mapId() == 0x06) {
            roomState.indoorBRoomStatus[0x2E] |= (byte) ROOM_STATUS_EVENT_2;
        } else if (activeRoom.mapId() == 0x03) {
            roomState.indoorARoomStatus[0x66] |= (byte) ROOM_STATUS_EVENT_2;
        }
    }

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

    void markActiveRoomCompleted() {
        if (activeRoom == null) {
            return;
        }
        byte[] status = activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            ? roomState.overworldRoomStatus : indoorStatusTableForMap(activeRoom.mapId());
        status[activeRoom.roomId()] |= (byte) ROOM_STATUS_EVENT_1;
    }

    /** Mirrors EntityCheckThrowAtTriggers' $35..$3C shutter-object test. */
    void harvestThrownRoomEventCollisions() {
        if (entityBridge.entityRuntime == null) {
            return;
        }
        for (ThrownBackgroundCollisionEvent collision
                : entityBridge.entityRuntime.consumePendingThrownBackgroundCollisions()) {
            int objectId = collision.objectId();
            if ((activeRoomEvent & EVENT_TRIGGER_MASK) == EVENT_TRIGGER_THROW_AT_DOOR
                && objectId >= 0x35 && objectId < 0x3D) {
                thrownAtDoorTriggerResolved = true;
            }
        }
    }

    /** Executes the source's first common dungeon-event path used by Tail Cave. */
    void tickRoomEvent(int linkEntityX, int linkEntityY) {
        tickIndoorKeyDoorAnimation();
        if (tickShutterClosing()) return;
        tickSwitchButton(linkEntityX, linkEntityY);
        tickRoomEventChestReveal(linkEntityX, linkEntityY);
        tickRoomEventStairReveal();
        if (pushedBlocksTriggerEffectPending) {
            pushedBlocksTriggerEffectPending = false;
            return;
        }
        if (activeRoom == null || activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            || activeRoomEvent == 0) {
            return;
        }
        int effect = activeRoomEvent & EVENT_EFFECT_MASK;
        // OpenLockedDoorsEffectHandler does not call EventEffectGuard:
        // opening a chest sets bit $10 too, but must not cancel the shutters.
        if (effect != EVENT_EFFECT_OPEN_LOCKED_DOORS
            && effect != EVENT_EFFECT_CLEAR_MIDBOSS
            && (activeRoomStatusFlags() & ROOM_STATUS_EVENT_1) != 0) {
            return;
        }
        int trigger = activeRoomEvent & EVENT_TRIGGER_MASK;
        if (!roomEventEffectExecuted
            && ((trigger == EVENT_TRIGGER_KILL_ALL_ENEMIES
                    && killAllEnemiesTriggerResolved())
                || (trigger == EVENT_TRIGGER_STEP_ON_BUTTON
                    && switchButtonPressed != 0)
                || (trigger == EVENT_TRIGGER_LIGHT_TORCHES
                    && roomTriggerCount == 0x02)
                || (trigger == EVENT_TRIGGER_THROW_AT_DOOR
                    && thrownAtDoorTriggerResolved)
                || (trigger == EVENT_TRIGGER_KILL_IN_ORDER
                    && entityBridge.entityRuntime != null
                    && entityBridge.entityRuntime.killOrderAt(0) == 0
                    && entityBridge.entityRuntime.killOrderAt(1) == 1
                    && entityBridge.entityRuntime.killOrderAt(2) == 2))) {
            roomEventEffectExecuted = true;
            colorShellSoundSink.play(GameplaySoundEvent.PUZZLE_SOLVED);
        }
        if (!roomEventEffectExecuted) {
            if ((effect == EVENT_EFFECT_OPEN_LOCKED_DOORS || effect == EVENT_EFFECT_CLEAR_MIDBOSS)
                && !shutterCloseStarted && pendingShutterCloseMask != 0
                && ((linkEntityX - 0x11) & 255) < 0x7E
                && ((linkEntityY - 0x16) & 255) < 0x5E) {
                shutterCloseStarted = true;
                colorShellSoundSink.play(GameplaySoundEvent.DOOR_CLOSED);
                tickShutterClosing();
            }
            return;
        }
        if (effect == EVENT_EFFECT_REVEAL_CHEST) {
            activeRoomEvent = 0;
            roomEventChestRevealCountdown = ROOM_EVENT_CHEST_REVEAL_COUNTDOWN;
            if (transientVfxSystem != null) {
                transientVfxSystem.spawn(TransientVfxType.CHEST_APPEARS, 0x88,
                    roomEventChestTop(linkEntityX, linkEntityY) + 0x10);
            }
        } else if (effect == EVENT_EFFECT_OPEN_LOCKED_DOORS) {
            openActiveRoomShutterDoors(true);
            activeRoomEvent = 0;
            colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
        } else if (effect == EVENT_EFFECT_DROP_KEY) {
            if (entityBridge.entityRuntime != null && entityBridge.entityRuntime.spawnRoomEventKeyDrop() >= 0) {
                activeRoom.replaceEntities(entityBridge.entityRuntime.snapshot());
            }
            activeRoomEvent = 0;
        } else if (effect == EVENT_EFFECT_REVEAL_STAIRWAY) {
            markActiveRoomCompleted();
            activeRoomEvent = 0;
            roomEventStairRevealCountdown = ROOM_EVENT_CHEST_REVEAL_COUNTDOWN;
            if (transientVfxSystem != null) {
                transientVfxSystem.spawn(TransientVfxType.STAIRS_APPEARS, 0x88, 0x20);
            }
        } else if (effect == EVENT_EFFECT_CLEAR_MIDBOSS) {
            openActiveRoomShutterDoors(true);
            if (activeRoomEvent == EVENT_CLEAR_MIDBOSS
                && activeRoom.mapId() >= 0
                && activeRoom.mapId() < roomState.dungeonProgressFlags.length) {
                roomState.dungeonProgressFlags[activeRoom.mapId()] |= 0x01;
                indoorStatusTableForMap(activeRoom.mapId())[activeRoom.roomId()] |= 0x20;
                entityBridge.entityRuntime.setDungeonMinibossReady(true);
                entityBridge.pendingRoomEntityEvents.add(new EntityCombatEvent(
                    0, 0x61, 0, false,
                    EntityCombatEvent.SoundChannel.JINGLE, 0x1B));
            }
            activeRoomEvent = 0;
        }
    }

    void tickPushedBlockMotion(int linkEntityX, int linkEntityY) {
        if (pushedBlockMotionFrames == 0 || activeRoom == null) {
            return;
        }
        if (entityInputs.entityDialogActive || entityInputs.entityInventoryAppearing || entityInputs.entityDialogCooldown != 0) {
            return;
        }
        pushedBlockMotionFrames--;
        int elapsedFrames = PUSHED_BLOCK_MOTION_FRAMES - pushedBlockMotionFrames;
        int distance = elapsedFrames >>> 1;
        int sourceX = ((pushedBlockSourceLocation & 0x0F) << 4) + 0x08;
        int sourceY = (pushedBlockSourceLocation & 0xF0) + 0x10;
        int x = sourceX;
        int y = sourceY;
        switch (pushedBlockDirection) {
            case 0 -> y -= distance;
            case 1 -> y += distance;
            case 2 -> x -= distance;
            case 3 -> x += distance;
            default -> {
                pushedBlockMotionFrames = 0;
                return;
            }
        }
        if (entityBridge.entityRuntime != null) {
            entityBridge.entityRuntime.movePushedBlock(pushedBlockEntitySlot, x, y);
            if (currentLinkMotionState < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE) {
                entityBridge.entityRuntime.resolvePushedBlockLinkContact(pushedBlockEntitySlot, linkEntityX, linkEntityY);
            }
            activeRoom.replaceEntities(entityBridge.entityRuntime.snapshot());
        }
        if (pushedBlockMotionFrames != 0) {
            return;
        }
        int destinationIndex = RoomConstants.ROOM_OBJECTS_BASE
            + pushedBlockDestinationLocation;
        if (destinationIndex >= 0
            && destinationIndex < activeRoom.roomObjectsArea().length) {
            activeRoom.roomObjectsArea()[destinationIndex] = OBJECT_SETTLED_PUSHED_BLOCK;
        }
        if (entityBridge.entityRuntime != null) {
            entityBridge.entityRuntime.removePushedBlock(pushedBlockEntitySlot);
            activeRoom.replaceEntities(entityBridge.entityRuntime.snapshot());
        }
        pushedBlockEntitySlot = -1;
        refreshActiveRoomTilemap();
        overworldCollision.setRoom(activeRoom.roomObjectsArea());
        overworldCollision.setGbcOverlay(null);
        if ((activeRoomEvent & EVENT_TRIGGER_MASK) == EVENT_TRIGGER_PUSH_SINGLE_BLOCK) {
            roomEventEffectExecuted = true;
            colorShellSoundSink.play(GameplaySoundEvent.PUZZLE_SOLVED);
        } else if ((activeRoomEvent & EVENT_TRIGGER_MASK) == EVENT_TRIGGER_PUSH_BLOCKS
            && pushedBlocksTriggerResolved()) {
            roomEventEffectExecuted = true;
            pushedBlocksTriggerEffectPending = true;
            colorShellSoundSink.play(GameplaySoundEvent.PUZZLE_SOLVED);
        }
    }

    boolean pushedBlocksTriggerResolved() {
        if (activeRoom == null || pushedBlockDestinationLocation < 0) {
            return false;
        }
        int neighborLocation = switch (pushedBlockDirection) {
            // The source checks the object collided with on the block's
            // movement-facing side; only horizontal pushes can satisfy $07.
            case 2 -> pushedBlockDestinationLocation - 1;
            case 3 -> pushedBlockDestinationLocation + 1;
            default -> -1;
        };
        if (neighborLocation < 0) {
            return false;
        }
        int neighbor = objectAtRoomLocation(neighborLocation);
        return neighbor == OBJECT_SETTLED_PUSHED_BLOCK || neighbor == OBJECT_PUSHABLE_BLOCK;
    }

    void tickIndoorKeyDoorAnimation() {
        if (indoorKeyDoorAnimationCountdown == 0 || indoorKeyDoorDirection < 0
            || activeRoom == null) {
            return;
        }
        worldLinkMotionBlockPending = true;
        indoorKeyDoorAnimationCountdown--;
        if (indoorKeyDoorAnimationCountdown != 0) {
            return;
        }

        int direction = indoorKeyDoorDirection;
        indoorKeyDoorDirection = -1;
        if (indoorBossDoorOpening) {
            replaceBossDoorObjects(indoorKeyDoorLocation);
            indoorBossDoorOpening = false;
        } else {
            replaceKeyDoorObjects(direction, indoorKeyDoorLocation);
        }
        indoorKeyDoorLocation = -1;
        byte[] status = indoorStatusTableForMap(activeRoom.mapId());
        status[activeRoom.roomId()] |= (byte) KEY_DOOR_CURRENT_STATUS[direction];
        int adjacentRoom = adjacentIndoorRoomIdForBombWall(direction);
        status[adjacentRoom] |= (byte) KEY_DOOR_ADJACENT_STATUS[direction];
        refreshActiveRoomTilemap();
        overworldCollision.setRoom(activeRoom.roomObjectsArea());
        overworldCollision.setGbcOverlay(null);
    }

    void replaceKeyDoorObjects(int direction, int location) {
        int firstClosedObject = KEY_DOOR_CLOSED_OBJECTS[direction];
        int[] openObjects = KEY_DOOR_OPEN_OBJECTS[direction];
        int[] objects = activeRoom.roomObjectsArea();
        int secondLocation = location + (direction < 2 ? 1 : 0x10);
        int[] locations = {location, secondLocation};
        for (int objectOffset = 0; objectOffset < locations.length; objectOffset++) {
            int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + locations[objectOffset];
            if (areaIndex >= 0 && areaIndex < objects.length
                && (objects[areaIndex] & 0xFF) == firstClosedObject + objectOffset) {
                objects[areaIndex] = openObjects[objectOffset];
                if (activeRoom.renderValues() != null) {
                    activeRoom.renderValues()[areaIndex] = openObjects[objectOffset];
                }
            }
        }
    }

    void replaceBossDoorObjects(int location) {
        int[] objects = activeRoom.roomObjectsArea();
        int[] openObjects = KEY_DOOR_OPEN_OBJECTS[0];
        for (int offset = 0; offset < 2; offset++) {
            int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + location + offset;
            if (areaIndex >= 0 && areaIndex < objects.length
                && objects[areaIndex] == 0xA4 + offset) {
                objects[areaIndex] = openObjects[offset];
                if (activeRoom.renderValues() != null) {
                    activeRoom.renderValues()[areaIndex] = openObjects[offset];
                }
            }
        }
    }

    boolean killAllEnemiesTriggerResolved() {
        for (RoomEntity entity : activeRoom.entities().slots()) {
            if (entity.status() != EntityStatus.DISABLED
                && (romTables.entityOptions1(entity.type())
                    & ENTITY_OPTION_EXCLUDED_FROM_KILL_ALL) == 0) {
                return false;
            }
        }
        return true;
    }

    void tickSwitchButton(int linkEntityX, int linkEntityY) {
        if (activeRoom == null || activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            || switchButtonPressed != 0) {
            return;
        }
        int objectUnderLink = overworldCollision.groundInteractionSample(
            linkEntityX, linkEntityY).objectId();
        if (objectUnderLink != OBJECT_SWITCH_BUTTON) {
            switchButtonPressCounter = 0;
            return;
        }
        switchButtonPressCounter = (switchButtonPressCounter + 1) & 0xFF;
        if (switchButtonPressCounter == SWITCH_BUTTON_PRESS_FRAMES) {
            switchButtonPressed = SWITCH_BUTTON_PRESSED;
            gpu.copyPressedFloorSwitchTiles(romData);
            colorShellSoundSink.play(GameplaySoundEvent.FLOOR_SWITCH);
        }
    }

    void tickRoomEventChestReveal(int linkEntityX, int linkEntityY) {
        if (roomEventChestRevealCountdown == 0) {
            return;
        }
        roomEventChestRevealCountdown--;
        if (roomEventChestRevealCountdown != ROOM_EVENT_CHEST_WRITE_COUNTDOWN) {
            return;
        }
        int location = roomEventChestTop(linkEntityX, linkEntityY) | 0x08;
        writeBombPuzzleObject(location, 0xA0);
        refreshActiveRoomTilemap();
        overworldCollision.setRoom(activeRoom.roomObjectsArea());
    }

    /** Type-$04 poof calls func_002_5F5C when its countdown reaches four. */
    void tickRoomEventStairReveal() {
        if (roomEventStairRevealCountdown == 0) {
            return;
        }
        roomEventStairRevealCountdown--;
        if (roomEventStairRevealCountdown != ROOM_EVENT_CHEST_WRITE_COUNTDOWN) {
            return;
        }
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + EVENT_STAIRWAY_LOCATION;
        activeRoom.roomObjectsArea()[areaIndex] = OBJECT_STAIRS_DOWN;
        configureStaircase(EVENT_STAIRWAY_LOCATION);
        refreshActiveRoomTilemap();
        overworldCollision.setRoom(activeRoom.roomObjectsArea());
        overworldCollision.setGbcOverlay(null);
    }

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

    void refreshActiveRoomTilemap() {
        RoomTilemap tilemap = activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            ? roomTilemapBuilder.buildOverworld(activeRoom.roomId(), activeRoom.roomObjectsArea())
            : roomTilemapBuilder.buildIndoor(activeRoom.mapId(), activeRoom.roomId(),
                activeRoom.roomObjectsArea());
        activeRoom.replaceTilemap(tilemap.tileIds(), tilemap.tileAttrs());
        applyHookshotBridgeTileOverrides();
        applyBombedWallTileOverrides();
        applyBombedBlockTileOverrides();
        applyBombedCaveDoorTileOverrides();
        applyRetainedTorchTiles();
        shutterTileOverrides.forEach((index, tile) -> activeRoom.tileIds()[index] = tile);
    }

    /** $02:599D selects each shutter; $02:5C04 closes it over eight frames. */
    boolean tickShutterClosing() {
        if (!shutterCloseStarted || activeRoom == null) return false;
        if (shutterCloseDirection < 0) {
            if (pendingShutterCloseMask == 0) return false;
            shutterCloseDirection = Integer.numberOfTrailingZeros(pendingShutterCloseMask);
            pendingShutterCloseMask &= ~(1 << shutterCloseDirection);
            shutterCloseLocation = new RoomObjectParser(romData).indoorShutterLocation(
                activeRoom.roomObjectsArea(), shutterCloseDirection);
            shutterCloseTicks = 0;
            return true;
        }
        worldLinkMotionBlockPending = true;
        if (shutterCloseLocation >= 0) {
            int direction = shutterCloseDirection;
            int left = (shutterCloseLocation & 15) * 16
                + Byte.toUnsignedInt(romData[RomBank.romOffset(2, 0x5BE0 + direction + 4)]);
            int top = (shutterCloseLocation >>> 4) * 16;
            int source = RomBank.romOffset(2, 0x5BC4 + direction * 4);
            int rowOffset = RomBank.romOffset(2, 0x5BE8 + direction + 4);
            for (int row = 0; row < 2; row++) {
                int y = top + Byte.toUnsignedInt(romData[rowOffset + row * 4]);
                int index = (y / 8) * RoomConstants.ROOM_TILE_WIDTH + left / 8;
                for (int col = 0; col < 2; col++) {
                    int tile = Byte.toUnsignedInt(romData[source + row * 2 + col]);
                    if (index + col < activeRoom.tileIds().length) {
                        activeRoom.tileIds()[index + col] = tile;
                        shutterTileOverrides.put(index + col, tile);
                    }
                }
            }
        }
        if (++shutterCloseTicks < 8) return true;
        int direction = shutterCloseDirection;
        shutterTileOverrides.clear();
        new RoomObjectParser(romData).applyIndoorShutterClosedState(
            activeRoom.roomObjectsArea(), 1 << direction);
        closedShutterMask |= 1 << direction;
        // $02:5D21 uses indoor A even for indoor B (the original's bank selection).
        byte[] status = activeRoom.mapId() == 0xFF ? roomState.colorDungeonRoomStatus : roomState.indoorARoomStatus;
        status[activeRoom.roomId()] &= (byte) ~KEY_DOOR_CURRENT_STATUS[direction];
        shutterCloseDirection = shutterCloseLocation = -1;
        refreshActiveRoomTilemap();
        overworldCollision.setRoom(activeRoom.roomObjectsArea());
        return true;
    }

    void openActiveRoomShutterDoors() {
        openActiveRoomShutterDoors(false);
    }

    void openActiveRoomShutterDoors(boolean persistDirectionalStatus) {
        pendingShutterCloseMask = closedShutterMask = 0;
        shutterCloseDirection = shutterCloseLocation = -1;
        shutterTileOverrides.clear();
        if (activeRoom == null) {
            return;
        }
        int shutterMask = activeRoom.shutterDoorMask();
        if (shutterMask != 0 && activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            RoomObjectParser parser = new RoomObjectParser(romData);
            int[] openDoorTypes = {0xF4, 0xF5, 0xF6, 0xF7};
            for (int direction = 0; direction < openDoorTypes.length; direction++) {
                if ((shutterMask & (1 << direction)) != 0) {
                    int location = parser.indoorShutterLocation(
                        activeRoom.roomObjectsArea(), direction);
                    if (location < 0) {
                        continue;
                    }
                    parser.applyIndoorDoorMacro(
                        activeRoom.roomObjectsArea(), openDoorTypes[direction],
                        location);
                    if (persistDirectionalStatus) {
                        byte[] status = indoorStatusTableForMap(activeRoom.mapId());
                        status[activeRoom.roomId()]
                            |= (byte) KEY_DOOR_CURRENT_STATUS[direction];
                        int adjacentRoom = adjacentIndoorRoomIdForBombWall(direction);
                        status[adjacentRoom]
                            |= (byte) KEY_DOOR_ADJACENT_STATUS[direction];
                    }
                }
            }
            refreshActiveRoomTilemap();
            overworldCollision.setRoom(activeRoom.roomObjectsArea());
            overworldCollision.setGbcOverlay(null);
        }
        activeRoom.openShutterDoors();
    }

    RoomEntityObjectSample entityObjectSample(RoomEntity entity) {
        OverworldCollision.GroundInteractionSample sample =
            overworldCollision.groundInteractionSample(entity.x(), entity.y());
        return new RoomEntityObjectSample(sample.objectId(), sample.physicsFlag(),
            sample.objectLeft(), sample.objectTop());
    }

    RoomEntityObjectSample entityObjectIntersectionSample(RoomEntity entity) {
        OverworldCollision.GroundInteractionSample sample =
            overworldCollision.entityInteractionSample(entity.x(), entity.y());
        return new RoomEntityObjectSample(sample.objectId(), sample.physicsFlag(),
            sample.objectLeft(), sample.objectTop());
    }

    void applyHookshotBridgeUpdates(
            List<HookshotBridgeUpdate> updates) {
        if (activeRoom == null || updates == null || updates.isEmpty()) {
            return;
        }
        int[] objects = activeRoom.roomObjectsArea();
        boolean changed = false;
        for (HookshotBridgeUpdate update : updates) {
            int areaIndex = hookshotObjectAreaIndex(update.objectLeft(), update.objectTop());
            if (areaIndex < 0 || areaIndex >= objects.length) {
                continue;
            }
            HookshotBridgeUpdate previous =
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

    void applyHookshotBridgeTileOverrides() {
        if (activeRoom == null || hookshotBridgeTileOverrides.isEmpty()) {
            return;
        }
        int[] tileIds = activeRoom.tileIds();
        for (HookshotBridgeUpdate update
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

    void applyBombedWallTileOverrides() {
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

    void applyBombedBlockTileOverrides() {
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

    void applyBombedCaveDoorTileOverrides() {
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

            RomAddress tileTable = RomSymbols.BombedCaveDoorTilesIndexesGBC;
            int topLeft = tileY * RoomConstants.ROOM_TILE_WIDTH + tileX;
            tileIds[topLeft] = new Rom(romData).u8(tileTable);
            tileIds[topLeft + 1] = new Rom(romData).u8(tileTable.plus(1));
            tileIds[topLeft + RoomConstants.ROOM_TILE_WIDTH] =
                new Rom(romData).u8(tileTable.plus(2));
            tileIds[topLeft + RoomConstants.ROOM_TILE_WIDTH + 1] =
                new Rom(romData).u8(tileTable.plus(3));
        }
    }

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
    boolean entityBackgroundCollision(RoomEntity entity, int direction,
                                              int nextX, int nextY) {
        return entityBackgroundCollisionResult(entity, direction, nextX, nextY).blocked();
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResult(
            RoomEntity entity, int direction, int nextX, int nextY) {
        int ignoreHitsCountdown = entityBridge.entityRuntime == null
            ? 0 : entityBridge.entityRuntime.enemyIgnoreHitsCountdown(entity.slot());
        return entityBackgroundCollisionResult(
            entity, direction, nextX, nextY, ignoreHitsCountdown, 0);
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResult(
            RoomEntity entity, int direction, int nextX, int nextY,
            int ignoreHitsCountdown) {
        return entityBackgroundCollisionResult(entity, direction, nextX, nextY,
            ignoreHitsCountdown, 0);
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResult(
            RoomEntity entity, int direction, int nextX, int nextY,
            int ignoreHitsCountdown, int frameCounter) {
        EntityCollisionPointProbe.Sample sample = entityCollisionPointProbe.sample(
            entity, direction, nextX, nextY);
        int pointX = sample.x();
        int pointY = sample.y();
        int objectId = overworldCollision.objectIdAtPoint(pointX, pointY);
        int physicsFlag = overworldCollision.objectPhysicsFlagAtPoint(pointX, pointY);
        int ledgeTimer = entityBridge.entityRuntime == null
            ? 0 : entityBridge.entityRuntime.ledgeTransitionTimer(entity.slot());
        if ((entity.z() & 0x80) != 0) {
            ledgeTimer = 0;
        }
        int thrownDirection = entityBridge.entityRuntime == null
            ? 0xFF : entityBridge.entityRuntime.thrownDirection(entity.slot());
        EntityBackgroundCollisionState state = new EntityBackgroundCollisionState(
            frameCounter, activeRoom != null
                && activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD,
            thrownDirection, ledgeTimer, switchBlocksState,
            overworldCollision.linkStandingOnSwitchBlock());
        EntityBackgroundCollisionResolution resolution =
            entityBackgroundCollisionResolver.resolveWithState(
                entity, direction, sample, objectId, physicsFlag,
                ignoreHitsCountdown, state);
        if (entityBridge.entityRuntime != null) {
            entityBridge.entityRuntime.setLedgeTransitionTimer(entity.slot(), resolution.nextLedgeTimer());
        }
        return resolution.result();
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResultForTest(
            RoomEntity entity, int direction, int nextX, int nextY) {
        return entityBackgroundCollisionResult(entity, direction, nextX, nextY);
    }

    EntityBackgroundCollisionResult entityBackgroundCollisionResultForTest(
            RoomEntity entity, int direction, int nextX, int nextY, int frameCounter) {
        int ignoreHitsCountdown = entityBridge.entityRuntime == null
            ? 0 : entityBridge.entityRuntime.enemyIgnoreHitsCountdown(entity.slot());
        return entityBackgroundCollisionResult(entity, direction, nextX, nextY,
            ignoreHitsCountdown, frameCounter);
    }

    /**
     * Mirrors the terrain/status and conveyor portion of bank-$03's
     * ApplyEntityInteractionWithBackground. Terrain physics and the entity
     * options byte stay ROM-backed; the runtime invokes this after the
     * entity-family handler has completed its movement for the frame.
     */
    RoomEntityGroundInteraction.Result entityGroundInteraction(
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

    boolean pairoddProjectileObjectCollision(RoomEntity entity) {
        int physicsFlag = overworldCollision.objectPhysicsFlagAtEntityPosition(
            entity.x(), entity.y());
        return PairoddProjectileObjectCollision.collidesWithPhysicsFlag(physicsFlag);
    }

    void loadRoomSpecificTilesIfNeeded(int roomId) { roomLoadCoordinator.loadRoomSpecificTilesIfNeeded(roomId); }

    int adjacentOverworldRoomId(int direction) { return roomTransitionController.adjacentOverworldRoomId(direction); }

    RoomScrollNavigation.IndoorRoomDestination adjacentIndoorRoom(int direction) { return roomTransitionController.adjacentIndoorRoom(direction); }

    static int scrollTarget(int direction) { return RoomScrollNavigation.scrollTarget(direction); }
}
