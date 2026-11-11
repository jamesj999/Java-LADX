package linksawakening.world.room;

import linksawakening.world.object.OverworldBushInteraction;

import linksawakening.world.entity.LinkPositionHistory;

import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.EntityCollisionPointProbe;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.ColorShellWorld;
import linksawakening.world.entity.RomEnemyCombatTables;
import linksawakening.world.entity.FollowingNpcEntitySpawner;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.entity.FollowingNpcState;
import linksawakening.world.entity.EntityBackgroundCollisionResolver;

import linksawakening.world.transition.Warp;

import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.room.RoomSession.*;

/** Shared lifetime and state for the room facade and its focused collaborators. */
final class RoomSessionContext {
    final RoomSessionViews roomSessionViews;

    final RoomTransientState roomTransientState;
    final RoomEntityRewards roomEntityRewards;
    final RoomTileRedraw roomTileRedraw;
    final RoomCollisionQueries roomCollisionQueries;
    final RoomPlayerItemActions roomPlayerItemActions;
    final RoomPuzzleController roomPuzzleController;
    final RoomStoryState roomStoryState;
    final RoomDoorInteractions roomDoorInteractions;
    final RoomObjectMutations roomObjectMutations;
    final RoomTransitionController roomTransitionController;
    final RoomLoadCoordinator roomLoadCoordinator;

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
    final RomRandomByteSource entityRandomByteSource;
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
    final RoomEntityBackgroundInteraction entityBackgroundInteraction ;
    final LinkPositionHistory followingLinkPositionHistory = new LinkPositionHistory();

    final long roomSessionId = ++nextRoomSessionId;
    ActiveRoom activeRoom;
    /** Increments on every ROM room load, including a reload of the same room. */
    long roomLoadSerial;
    /** ROM wIndoorRoom: the current position in the active map's 8x8 layout. */
    int indoorMapPosition = -1;
    final RoomWorldState roomState;
    final RoomScrollNavigation scrollNavigation;
    final RoomEntityBridge entityBridge;
    final RoomEntityInputs entityInputs;

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
    linksawakening.world.transition.Warp respawnPoint;
    int respawnIndoorRoom;
    int boomerangSoundCounter;
    boolean boomerangSoundClockProvided;
    int enemyDropMaxHearts = 3;
    Runnable mapBoundaryWarp = () -> {};
    int enemyDropHealth = 6;
    Boolean enemyDropLowHealth;
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
    int playerSwordLevel;
    int playerBraceletLevel;
    int houseNpcTradeItem;
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
    final ColorShellWorld colorShellWorld ;
    RoomSessionContext(RoomSession session, byte[] romData,
                       GPU gpu,
                       RoomLoader roomLoader,
                       OverworldTilesetTable overworldTilesetTable,
                       OverworldCollision overworldCollision,
                       TransientVfxSystem transientVfxSystem,
                       DroppableRupeeSystem droppableRupeeSystem,
                       RoomLoadListener roomLoadListener,
                       RomRandomByteSource randomByteSource) {
        entityRandomByteSource = java.util.Objects.requireNonNull(randomByteSource);
        roomSessionViews = new RoomSessionViews(session);
        roomTransitionController = new RoomTransitionController(session);
        roomPlayerItemActions = new RoomPlayerItemActions(session);
        roomPuzzleController = new RoomPuzzleController(session);
        roomObjectMutations = new RoomObjectMutations(session);
        roomCollisionQueries = new RoomCollisionQueries(session);
        roomLoadCoordinator = new RoomLoadCoordinator(session);
        roomTileRedraw = new RoomTileRedraw(session);
        roomTransientState = new RoomTransientState(session);
        roomStoryState = new RoomStoryState(session);
        entityBridge = new RoomEntityBridge(session);
        entityInputs = new RoomEntityInputs(() -> entityBridge.entityRuntime);
        roomDoorInteractions = new RoomDoorInteractions(session);
        roomEntityRewards = new RoomEntityRewards(session);
        entityBackgroundInteraction =
        new RoomEntityBackgroundInteraction() {
            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction,
                                                           int nextX, int nextY) {
                return session.entityBackgroundCollisionResult(entity, direction, nextX, nextY, 0);
            }

            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction,
                                                           int nextX, int nextY,
                                                           int ignoreHitsCountdown) {
                return session.entityBackgroundCollisionResult(
                    entity, direction, nextX, nextY, ignoreHitsCountdown);
            }

            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction,
                                                           int nextX, int nextY,
                                                           int ignoreHitsCountdown,
                                                           int frameCounter) {
                return session.entityBackgroundCollisionResult(
                    entity, direction, nextX, nextY, ignoreHitsCountdown, frameCounter);
            }
        };
        colorShellWorld = new ColorShellWorld() {
        @Override
        public int objectAt(RoomEntity entity, int relativeOffset) {
            return session.colorShellObjectAt(entity, relativeOffset);
        }

        @Override
        public void writeObject(RoomEntity entity, int objectId) {
            session.writeColorShellObject(entity, objectId);
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
}
