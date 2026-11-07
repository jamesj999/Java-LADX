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
    final RoomSessionContext context;

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
        context = new RoomSessionContext(this, romData, gpu, roomLoader, overworldTilesetTable, overworldCollision, transientVfxSystem, droppableRupeeSystem, roomLoadListener);
    }

    public void loadInitialOverworld(int roomId) { context.roomLoadCoordinator.loadInitialOverworld(roomId); }

    public void loadWarpDestination(Warp warp) { context.roomTransitionController.loadWarpDestination(warp); }

    /** $00:0F53 countdown and $01:61EE dungeon-title dialog gates. */
    public int tickDungeonTitleMessage(boolean dialogActive, boolean inventoryClosed, int linkMotionState, boolean freeMovement) { return context.roomTransitionController.tickDungeonTitleMessage(dialogActive, inventoryClosed, linkMotionState, freeMovement); }

    public void startAdjacentOverworldScroll(ScrollController scrollController, int direction, int linkScreenX, int linkScreenY) { context.roomTransitionController.startAdjacentOverworldScroll(scrollController, direction, linkScreenX, linkScreenY); }

    void startMysteriousWoodsLostScroll(ScrollController scrollController, int linkScreenX, int linkScreenY) { context.roomTransitionController.startMysteriousWoodsLostScroll(scrollController, linkScreenX, linkScreenY); }

    public void startAdjacentIndoorScroll(ScrollController scrollController, int direction, int linkScreenX, int linkScreenY) { context.roomTransitionController.startAdjacentIndoorScroll(scrollController, direction, linkScreenX, linkScreenY); }

    public void loadOverworld(int roomId) { context.roomLoadCoordinator.loadOverworld(roomId); }

    public void loadIndoor(int mapId, int roomId) { context.roomLoadCoordinator.loadIndoor(mapId, roomId); }

    /** Restores hMapRoom and the independently saved source wIndoorRoom byte. */
    public void loadIndoorFromSavedPosition(int mapId, int roomId, int savedIndoorMapPosition) { context.roomLoadCoordinator.loadIndoorFromSavedPosition(mapId, roomId, savedIndoorMapPosition); }

    public void loadIndoor(int mapId, int roomId, int mapCategory) { context.roomLoadCoordinator.loadIndoor(mapId, roomId, mapCategory); }

    void loadIndoor(int mapId, int roomId, int mapCategory, int transitionDirection) { context.roomLoadCoordinator.loadIndoor(mapId, roomId, mapCategory, transitionDirection); }

    /** Current ROM health byte for diagnostic captures. */
    public int entityHealth(int slot) { return context.entityBridge.entityRuntime.enemyHealth(slot); }

    public ActiveRoom activeRoom() {
        return context.activeRoom;
    }

    /** Identifies the current load instance for Link room-entry state. */
    public long roomLoadSerial() {
        return context.roomLoadSerial;
    }

    /** Distinguishes same-room loads performed by different live sessions. */
    public long roomLoadToken() {
        return (context.roomSessionId << 32) ^ context.roomLoadSerial;
    }

    public boolean hasActiveRoom() {
        return context.activeRoom != null;
    }

    public boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) { return context.roomStoryState.applyMarinWakeUpPresentation(x, y, spriteVariant); }

    /** Tarin's indoor handler updates his facing every 32 frames, even during the bed scene. */
    public void updateHouseTarinPresentation(int frameCounter, int linkX, int linkY) { context.roomStoryState.updateHouseTarinPresentation(frameCounter, linkX, linkY); }

    /** Supplies the patched EntityInitKeyDropPoint inventory byte. */
    public void setBirdKeyOwned(boolean owned) { context.roomStoryState.setBirdKeyOwned(owned); }

    /** Supplies wHasTailKey to the active owl-event handler. */
    public void setTailKeyOwned(boolean owned) { context.roomStoryState.setTailKeyOwned(owned); }

    public int currentRoomId() {
        return context.activeRoom.roomId();
    }

    public int mapCategory() {
        return context.activeRoom.mapCategory();
    }

    public int mapId() {
        return context.activeRoom.mapId();
    }

    /**
     * Returns the source {@code wIndoorRoom} value for the active indoor map.
     * A negative result means this map has no ROM layout table, so the caller
     * should preserve the existing save byte rather than inventing one.
     */
    public int indoorRoomPositionForSave() {
        if (context.activeRoom == null || context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            return -1;
        }
        return context.indoorMapPosition;
    }

    /** Room-status bytes in the active dungeon's 8x8 ROM map order. */
    public byte[] dungeonMinimapRoomStatusSnapshot() { return context.roomState.dungeonMinimapRoomStatusSnapshot(context.activeRoom); }

    public byte[] overworldRoomStatusSnapshot() { return context.roomState.overworldRoomStatusSnapshot(); }

    public byte[] indoorARoomStatusSnapshot() { return context.roomState.indoorARoomStatusSnapshot(); }

    public byte[] indoorBRoomStatusSnapshot() { return context.roomState.indoorBRoomStatusSnapshot(); }

    public byte[] colorDungeonRoomStatusSnapshot() { return context.roomState.colorDungeonRoomStatusSnapshot(); }

    /**
     * Clears the persistent WRAM-shaped world state created for a fresh save.
     * The source new-file path starts with zeroed room-status, entity-clear,
     * dungeon-item, and dungeon-progress regions; retaining any of these from
     * a previously played file can incorrectly unload opening entities such
     * as the beach sword and owl.
     */
    public void initializeNewGameWorldState() { context.roomStoryState.initializeNewGameWorldState(); }

    /** Restores the room-status WRAM tables loaded by the source save path. */
    public void restoreRoomStatuses(byte[] overworldStatus, byte[] indoorAStatus, byte[] indoorBStatus, byte[] colorDungeonStatus) { context.roomStoryState.restoreRoomStatuses(overworldStatus, indoorAStatus, indoorBStatus, colorDungeonStatus); }

    public byte[] dungeonItemFlagsSnapshot() { return context.roomState.dungeonItemFlagsSnapshot(); }

    public byte[] colorDungeonItemFlagsSnapshot() { return context.roomState.colorDungeonItemFlagsSnapshot(); }

    public byte[] currentDungeonItemFlagsSnapshot() { return context.roomState.currentDungeonItemFlagsSnapshot(); }

    public void restoreDungeonItemFlags(byte[] dungeonFlags, byte[] colorFlags) { context.roomState.restoreDungeonItemFlags(dungeonFlags, colorFlags); }

    /** Snapshots wHasInstrument1..wHasInstrument8 (DB65..DB6C). */
    public byte[] dungeonProgressFlagsSnapshot() { return context.roomState.dungeonProgressFlagsSnapshot(); }

    public void restoreDungeonProgressFlags(byte[] flags) { context.roomState.restoreDungeonProgressFlags(flags); }

    int overworldRoomStatusForTest(int roomId) { return context.roomState.overworldRoomStatusForTest(roomId); }

    int indoorRoomStatusForTest(int mapId, int roomId) { return context.roomState.indoorRoomStatusForTest(mapId, roomId); }

    void setEnemyDropCountersForTest(int droppedItemCount, int droppedBombArrowCount) {
        context.enemyDropCounters = new EnemyDropResolver.CounterState(
            droppedItemCount, droppedBombArrowCount);
    }

    EnemyDropResolver.CounterState enemyDropCountersForTest() {
        return context.enemyDropCounters;
    }

    void setSwitchButtonPressedForTest(int value) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException("Switch-button state must be an unsigned byte");
        }
        context.switchButtonPressed = value;
    }

    int switchButtonPressedForTest() {
        return context.switchButtonPressed;
    }

    public int[][] palettes() {
        return context.activeRoom == null ? null : context.activeRoom.palettes();
    }

    public boolean heartPiecePickupActive() {
        return context.entityBridge.entityRuntime != null && context.entityBridge.entityRuntime.heartPiecePickupActive();
    }

    public void setHeartPiecesCount(int count) {
        if (context.entityBridge.entityRuntime != null) context.entityBridge.entityRuntime.setHeartPiecesCount(count);
    }
    public List<HeartPiecePickupMotion.Update> consumeHeartPieceUpdates() {
        if (context.entityBridge.entityRuntime == null) return List.of();
        var updates = context.entityBridge.entityRuntime.consumeHeartPieceUpdates();
        if (updates.stream().anyMatch(HeartPiecePickupMotion.Update::complete)) markActiveRoomCompleted();
        return updates;
    }

    public String instrumentTrace() { return context.entityBridge.entityRuntime == null ? "" : context.entityBridge.entityRuntime.instrumentTrace(); }
    public String moblinKingTrace() { return context.entityBridge.entityRuntime == null ? "-1" : context.entityBridge.entityRuntime.moblinKingTrace(); }

    public RoomRenderSnapshot renderSnapshot() {
        if (context.activeRoom == null) return null;
        RoomRenderSnapshot snapshot = context.activeRoom.renderSnapshot();
        return new RoomRenderSnapshot(snapshot.tileIds(), snapshot.tileAttrs(), snapshot.palettes(),
            snapshot.entities(), context.entityBridge.entityRuntime == null ? null : context.entityBridge.entityRuntime.instrumentWarpVisual(),
            context.entityBridge.entityRuntime == null ? null : context.entityBridge.entityRuntime.heartPieceVisual());
    }

    /**
     * Applies the follower display-list selections used by
     * {@code CreateFollowingNpcEntity}. This low-level hook is retained for
     * callers that already own the dynamic entity state; new code should use
     * {@link #setFollowingNpcState(FollowingNpcState, int, int, int, int, int)}
     * so spawning and selection stay in one ROM-shaped path.
     */
    public void setFollowerSpriteOverrides(Map<Integer, EntitySpriteDefinition> overrides) { context.roomStoryState.setFollowerSpriteOverrides(overrides); }

    /**
     * Binds the current WRAM-like follower flags and Link coordinates to the
     * active room. The operation is applied once per room load or follower
     * state change, matching the room-transition caller in room_transition.asm.
     * Coordinates are hLinkPositionX/Y/Z and entityYOffset is wC13B.
     */
    public void setFollowingNpcState(FollowingNpcState state, int linkX, int linkY, int linkZ, int entityYOffset, int linkDirection) { context.roomStoryState.setFollowingNpcState(state, linkX, linkY, linkZ, entityYOffset, linkDirection); }

    public FollowingNpcState followingNpcState() { return context.roomStoryState.followingNpcState(); }

    /** Mirrors wIsBowWowFollowingLink, including BOW_WOW_KIDNAPPED=$80. */
    public int bowWowState() { return context.roomStoryState.bowWowState(); }

    public void setBowWowState(int state) { context.roomStoryState.setBowWowState(state); }

    public int richardSpokenFlag() { return context.roomStoryState.richardSpokenFlag(); }

    public void setRichardSpokenFlag(int flag) { context.roomStoryState.setRichardSpokenFlag(flag); }

    /** Mirrors the unsigned-byte world progression flag at wTarinFlag. */
    public int tarinFlag() { return context.roomStoryState.tarinFlag(); }

    public void setTarinFlag(int flag) { context.roomStoryState.setTarinFlag(flag); }

    /** Supplies the held J_A|J_B state consumed by input-driven entity handlers. */
    public void setEntityActionButtonsHeld(boolean actionButtonsHeld) { context.entityInputs.setEntityActionButtonsHeld(actionButtonsHeld); }

    /** The cling flag produced by the previous entity-handler pass. */
    public boolean gelClingingToLink() {
        return context.entityBridge.entityRuntime != null && context.entityBridge.entityRuntime.gelClingingToLink();
    }

    /** Supplies wDialogState's active gate to the room entity handlers. */
    public void setEntityLinkVelocityZ(int velocity) { context.entityInputs.setEntityLinkVelocityZ(velocity); }

    public void setEntityDialogActive(boolean active) { context.entityInputs.setEntityDialogActive(active); }

    /** Supplies the remaining WRAM gates read by ShouldLinkTalkToEntity_05. */
    public void setEntityTalkState(boolean inventoryAppearing, int dialogCooldown,
                                   int windowY) { context.entityInputs.setEntityTalkState(inventoryAppearing, dialogCooldown, windowY); }

    public void setEntityDialogAskSelectionIndex(int selectionIndex) { context.entityInputs.setEntityDialogAskSelectionIndex(selectionIndex); }

    /** Supplies the source wActiveMusicIndex gate to entity state machines. */
    public void setEntityMusicActive(boolean active) { context.entityInputs.setEntityMusicActive(active); }

    /** Supplies the distinct held A/B states consumed by ROM input handlers. */
    public void setEntityActionButtonsHeld(boolean actionButtonAHeld, boolean actionButtonBHeld) { context.entityInputs.setEntityActionButtonsHeld(actionButtonAHeld, actionButtonBHeld); }

    /** Supplies whether any held Game Boy joypad bit is active. */
    public void setEntityJoypadHeld(boolean joypadHeld) { context.entityInputs.setEntityJoypadHeld(joypadHeld); }

    /** Supplies hPressedButtonsMask's currently-held D-pad bits to entities. */
    public void setEntityPressedButtonsMask(int pressedButtonsMask) { context.entityInputs.setEntityPressedButtonsMask(pressedButtonsMask); }

    /** Supplies the B/A inventory bytes consumed by Like Like's handler. */
    public void setEntityInventorySlots(int itemA, int itemB) { context.entityInputs.setEntityInventorySlots(itemA, itemB); }

    /** Supplies the upgrade bytes consumed by the live chest dialog handler. */
    public void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) { context.roomStoryState.setChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel); }

    /** Supplies the two WRAM values checked by SleepyToadstoolEntityHandler. */
    public void setToadstoolPlayerState(boolean hasToadstool, int magicPowderCount) { context.entityInputs.setToadstoolPlayerState(hasToadstool, magicPowderCount); }

    public void setEntityDefaultMusicTrack(int trackId) { context.roomStoryState.setEntityDefaultMusicTrack(trackId); }

    void updateWitchEnvironment() { context.roomStoryState.updateWitchEnvironment(); }

    /** Supplies wGoldenLeavesCount to the live Hiding Slime Key handler. */
    public void setEntityGoldenLeavesCount(int goldenLeavesCount) { context.entityInputs.setEntityGoldenLeavesCount(goldenLeavesCount); }

    /** Supplies the live HRAM shake/collision values used by tree shells. */
    public void setSecretSeashellPegasusCollisionState(boolean screenShakeActive,
                                                        boolean collisionActive,
                                                        int collisionX, int collisionY) { context.entityInputs.setSecretSeashellPegasusCollisionState(screenShakeActive, collisionActive, collisionX, collisionY); }

    /** Supplies the held A/B state used by EntityGetLiftedUp. */
    public void setEntityPowerBraceletButtonHeld(boolean buttonHeld) { context.entityInputs.setEntityPowerBraceletButtonHeld(buttonHeld); }

    /** Supplies the held A/B state for the equipped-bomb lift/throw path. */
    public void setEntityBombButtonHeld(boolean buttonHeld) { context.entityInputs.setEntityBombButtonHeld(buttonHeld); }

    /** Supplies wLinkAttackStepAnimationCountdown to input-driven entity handlers. */
    public void setEntityAttackStepAnimationCountdown(int countdown) { context.entityInputs.setEntityAttackStepAnimationCountdown(countdown); }

    /** Supplies hLinkFinalPositionX captured before the current Link update. */
    public void setEntityFinalPositionX(int positionX) { context.entityInputs.setEntityFinalPositionX(positionX); }

    /** Returns wShouldGetLostInMysteriousWoods from the latest entity pass. */
    public boolean shouldGetLostInMysteriousWoods() { return context.roomStoryState.shouldGetLostInMysteriousWoods(); }

    /** Starts the source UseOcarina countdown consumed by active entities. */
    public boolean startOcarina(int countdown, int songFlags, int selectedSong) { return context.roomTransitionController.startOcarina(countdown, songFlags, selectedSong); }

    /** Mirrors wLinkPlayingOcarinaCountdown for Link's motion gate. */
    public boolean ocarinaPlaying() { return context.roomTransitionController.ocarinaPlaying(); }

    /** Mirrors hLinkAnimationState $75/$76 while LinkPlayingOcarinaHandler runs. */
    public int ocarinaAnimationState() { return context.roomTransitionController.ocarinaAnimationState(); }

    /**
     * Returns the ROM destination prepared by the completed Mambo branch.
     * The caller starts the non-interactive transition and applies this warp
     * only after the source's {@code $C0}-frame effect has finished.
     */
    public Warp consumeManboPondTransitionRequest() { return context.roomTransitionController.consumeManboPondTransitionRequest(); }

    /** Returns the room's prepared warp after the instrument effect completes. */
    public Warp consumeInstrumentTransitionRequest() { return context.roomTransitionController.consumeInstrumentTransitionRequest(); }

    /** Returns the completed entity-$61 request emitted by the ROM handler. */
    Warp consumeClearedDungeonWarp() { return context.roomTransitionController.consumeClearedDungeonWarp(); }

    /** Supplies the player fields consumed by the ROM enemy-drop resolver. */
    public void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp) { context.entityBridge.setEnemyDropPlayerState(maxHearts, health, activePowerUp); }

    public RoomEntityRuntime.LiftedEntityState liftedEntityState() {
        return context.entityBridge.entityRuntime == null
            ? RoomEntityRuntime.LiftedEntityState.none()
            : context.entityBridge.entityRuntime.liftedEntityState();
    }

    /** Throws the currently held object using Link's Java direction order. */
    public boolean throwLiftedEntity(int linkDirection) { return context.roomPlayerItemActions.throwLiftedEntity(linkDirection); }

    /**
     * Mirrors {@code FireHookshot}: the item path supplies Link's current ROM
     * coordinates and the room owns the player-projectile entity slot.
     */
    public boolean fireHookshot(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean linkAirborne, boolean linkPushing) { return context.roomPlayerItemActions.fireHookshot(linkEntityX, linkEntityY, linkEntityZ, romDirection, linkAirborne, linkPushing); }

    /** Mirrors UseBoomerang's player-projectile spawn bridge. */
    public boolean fireBoomerang(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask) { return context.roomPlayerItemActions.fireBoomerang(linkEntityX, linkEntityY, linkEntityZ, romDirection, pressedButtonsMask); }

    /** Mirrors the level-two branch of UseSword's player-projectile spawn. */
    public boolean fireSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return context.roomPlayerItemActions.fireSwordBeam(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    /** Mirrors the delayed UseMagicRod player-projectile spawn bridge. */
    public boolean fireMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return context.roomPlayerItemActions.fireMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    public boolean fireMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) { return context.roomPlayerItemActions.fireMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower); }

    /** Mirrors the delayed UseMagicPowder sprinkle-entity spawn bridge. */
    public boolean sprinkleMagicPowder(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return context.roomPlayerItemActions.sprinkleMagicPowder(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    /**
     * Mirrors {@code UseShovel}: the item always enters its 24-frame pose
     * window when Link is grounded, while the adjacent-cell probe determines
     * whether the sound is a poke or a normal dig.
     *
     * <p>The coordinates are the ROM's {@code hLinkPositionX/Y} values. The
     * caller supplies ROM direction order: RIGHT, LEFT, UP, DOWN.</p>
     */
    public ShovelStartResult startShovel(int linkEntityX, int linkEntityY, int romDirection, boolean linkAirborne) { return context.roomPlayerItemActions.startShovel(linkEntityX, linkEntityY, romDirection, linkAirborne); }

    /** Applies the ROM shovel timer's delayed room mutation and finish path. */
    public void advanceShovel(int linkEntityX, int linkEntityY, int romDirection, int timer) { context.roomPlayerItemActions.advanceShovel(linkEntityX, linkEntityY, romDirection, timer); }

    /** Resolves the ROM shovel animation table through Java's Link direction order. */
    public int shovelAnimationState(int javaDirection, int timer) { return context.roomPlayerItemActions.shovelAnimationState(javaDirection, timer); }

    ShovelProbe probeShovel(int linkEntityX, int linkEntityY, int romDirection) { return context.roomPlayerItemActions.probeShovel(linkEntityX, linkEntityY, romDirection); }

    void writeShovelHole(ShovelProbe probe) { context.roomPlayerItemActions.writeShovelHole(probe); }

    /**
     * Mirrors the ordinary player-bomb placement bridge. The item has already
     * applied PlaceBomb's inventory ordering; this method owns the room/runtime
     * allocation and immutable snapshot refresh.
     */
    public boolean placeBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return context.roomPlayerItemActions.placeBomb(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    /** Returns whether the last successful bomb placement queued the top-view bump jingle. */
    public boolean lastBombPlacementPlayedBump() { return context.roomPlayerItemActions.lastBombPlacementPlayedBump(); }

    /** Result of the ROM ShootArrow bridge, including its conditional whoosh. */
    public record ArrowShotResult(boolean spawned, boolean playWhoosh) {
    }

    /** Mirrors ShootArrow's player-projectile bridge and refreshes the room snapshot. */
    public boolean shootArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return context.roomPlayerItemActions.shootArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    public ArrowShotResult shootArrowResult(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return context.roomPlayerItemActions.shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    /** ShootArrow's label_140F speed-table selector for Piece of Power. */
    public ArrowShotResult shootArrowResult(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) { return context.roomPlayerItemActions.shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower); }

    /** Returns the modeled player-projectile count used by the item gates. */
    public int activeProjectileCount() { return context.roomPlayerItemActions.activeProjectileCount(); }

    /** Returns whether the live room currently owns an ordinary bomb entity. */
    public boolean bombActive() { return context.roomPlayerItemActions.bombActive(); }

    int bombDirectionForTest(int slot) {
        return context.entityBridge.entityRuntime == null ? 0xFF : context.entityBridge.entityRuntime.bombDirection(slot);
    }

    /** Returns whether the live room currently owns entity {@code $03}. */
    public boolean hookshotActive() { return context.roomPlayerItemActions.hookshotActive(); }

    /** Returns whether the live room currently owns the boomerang entity. */
    public boolean boomerangActive() { return context.roomPlayerItemActions.boomerangActive(); }

    public void setColorShellSoundSink(GameplaySoundSink soundSink) {
        context.colorShellSoundSink = soundSink == null ? GameplaySoundSink.none() : soundSink;
    }

    void setColorShellStateForTest(int slot, int state, int transitionCountdown,
                                   int direction, int speedX, int speedY) {
        if (context.entityBridge.entityRuntime != null) {
            context.entityBridge.entityRuntime.setColorShellStateForTest(slot, state, transitionCountdown,
                direction, speedX, speedY);
        }
    }

    int entityGroundStatusForTest(int slot) {
        if (context.entityBridge.entityRuntime == null) {
            return 0;
        }
        return context.entityBridge.entityRuntime.groundStatus(slot);
    }

    void setEntityIgnoreHitsCountdownForTest(int slot, int value) {
        if (context.entityBridge.entityRuntime != null) {
            context.entityBridge.entityRuntime.setEnemyIgnoreHitsCountdownForTest(slot, value);
        }
    }

    void setEntityThrownDirectionForTest(int slot, int value) {
        if (context.entityBridge.entityRuntime != null) {
            context.entityBridge.entityRuntime.setThrownDirection(slot, value);
        }
    }

    void setEntityLedgeTimerForTest(int slot, int value) {
        if (context.entityBridge.entityRuntime != null) {
            context.entityBridge.entityRuntime.setLedgeTransitionTimer(slot, value);
        }
    }

    void setEntitySwitchBlocksStateForTest(int value) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(
                "Switch-block state must be an unsigned byte: " + value);
        }
        context.switchBlocksState = value;
        synchronizeSwitchBlockCollisionState();
    }

    void setSwitchableObjectAnimationStageForTest(int value) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(
                "Switch-block animation stage must be an unsigned byte: " + value);
        }
        context.switchableObjectAnimationStage = value;
    }

    int entitySwitchBlocksStateForTest() {
        return context.switchBlocksState & 0xFF;
    }

    int switchableObjectAnimationStageForTest() {
        return context.switchableObjectAnimationStage & 0xFF;
    }

    boolean linkCollisionPointBlockedForTest(int pixelX, int pixelY) {
        return context.overworldCollision.pointBlocked(pixelX, pixelY);
    }

    int entityLedgeTimerForTest(int slot) {
        return context.entityBridge.entityRuntime == null ? 0 : context.entityBridge.entityRuntime.ledgeTransitionTimer(slot);
    }

    int entityFallingTargetXForTest(int slot) {
        return context.entityBridge.entityRuntime == null ? 0 : context.entityBridge.entityRuntime.fallingTargetX(slot);
    }

    int entityTransitionCountdownForTest(int slot) {
        if (context.entityBridge.entityRuntime == null) {
            return 0;
        }
        RoomEntity entity = context.activeRoom == null ? null : context.activeRoom.entities().slots().get(slot);
        return entity != null && entity.type() == WarpMotion.ENTITY_TYPE
            ? context.entityBridge.entityRuntime.warpTransitionCountdownForTest(slot)
            : context.entityBridge.entityRuntime.transitionCountdown(slot);
    }

    int roomTriggerCountForTest() {
        return context.roomTriggerCount & 0xFF;
    }

    int activeRoomEventForTest() {
        return context.activeRoomEvent & 0xFF;
    }

    boolean hasDungeonInstrumentForTest(int mapId) {
        return mapId >= 0 && mapId < context.roomState.dungeonProgressFlags.length
            && (context.roomState.dungeonProgressFlags[mapId] & 0x02) != 0;
    }

    int indoorTorchPaletteEffectAddressForTest() {
        return context.indoorTorchPaletteEffect.effectAddress();
    }

    void igniteIndoorTorchPaletteForTest() {
        context.indoorTorchPaletteEffect.igniteTorch();
    }

    void setRoomTriggerCountForTest(int value) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException("Room trigger count must be an unsigned byte");
        }
        context.roomTriggerCount = value;
    }

    int entityDropSpeedXForTest(int slot) {
        return context.entityBridge.entityRuntime == null ? 0 : context.entityBridge.entityRuntime.dropSpeedX(slot);
    }

    int entityDropSpeedZForTest(int slot) {
        return context.entityBridge.entityRuntime == null ? 0 : context.entityBridge.entityRuntime.dropSpeedZ(slot);
    }

    void replaceEntityRuntimeForTest(RoomEntityRuntime runtime) {
        context.entityBridge.entityRuntime = runtime;
    }

    /** WorldHandler's shared sound clock persists across entity and room loads. */
    public void advanceBoomerangSoundClock(boolean worldGameplay) { context.entityBridge.advanceBoomerangSoundClock(worldGameplay); }

    public void tickEntities(int frameCounter) { context.entityBridge.tickEntities(frameCounter); }

    public void tickEntities(int frameCounter, int linkEntityX, int linkEntityY) { context.entityBridge.tickEntities(frameCounter, linkEntityX, linkEntityY); }

    public void tickEntities(int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkDirection) { context.entityBridge.tickEntities(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkDirection); }

    public void tickEntities(int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, boolean linkAirborne, int linkDirection) { context.entityBridge.tickEntities(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkAirborne, linkDirection); }

    /**
     * Advances entities and returns the ROM projectile interactions generated
     * during this frame.  The older tick overloads route through this method
     * with a non-interactive Link state, so callers that do not provide player
     * interaction state retain their previous behavior.
     */
    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkMotionState, int linkDirection, boolean usingShield) { return context.entityBridge.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkMotionState, linkDirection, usingShield); }

    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, boolean linkAirborne, int linkMotionState, int linkDirection, boolean usingShield) { return context.entityBridge.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkAirborne, linkMotionState, linkDirection, usingShield); }

    /** Advances entities with the additional ROM shield and invincibility fields. */
    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkMotionState, int linkDirection, boolean usingShield, int shieldLevel, int invincibilityCounter) { return context.entityBridge.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkMotionState, linkDirection, usingShield, shieldLevel, invincibilityCounter); }

    /** Advances entities with Link's optional current sword collision rectangle. */
    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkMotionState, int linkDirection, boolean usingShield, int shieldLevel, int invincibilityCounter, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight) { return context.entityBridge.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkMotionState, linkDirection, usingShield, shieldLevel, invincibilityCounter, swordCollisionActive, swordX, swordWidth, swordY, swordHeight); }

    /** Advances entities with Link's current ROM collision byte. */
    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkMotionState, int linkDirection, int collisionType, boolean usingShield, int shieldLevel, int invincibilityCounter, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight) { return context.entityBridge.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkMotionState, linkDirection, collisionType, usingShield, shieldLevel, invincibilityCounter, swordCollisionActive, swordX, swordWidth, swordY, swordHeight); }

    /** Advances entities with the current Link speed bytes for enemy-bomb recoil. */
    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkMotionState, int linkDirection, int collisionType, boolean usingShield, int shieldLevel, int invincibilityCounter, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight, int linkSpeedX, int linkSpeedY) { return context.entityBridge.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkMotionState, linkDirection, collisionType, usingShield, shieldLevel, invincibilityCounter, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, linkSpeedX, linkSpeedY); }

    /** Rich live path with Link's explicit airborne state. */
    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, boolean linkAirborne, int linkMotionState, int linkDirection, int collisionType, boolean usingShield, int shieldLevel, int invincibilityCounter, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight, int linkSpeedX, int linkSpeedY) { return context.entityBridge.tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkAirborne, linkMotionState, linkDirection, collisionType, usingShield, shieldLevel, invincibilityCounter, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, linkSpeedX, linkSpeedY); }

    /** Mirrors the countdown-zero branches of LinkPlayingOcarinaHandler. */
    void finishOcarinaPlayback() { context.roomTransitionController.finishOcarinaPlayback(); }

    void tickOcarinaAnimationHandler() { context.roomTransitionController.tickOcarinaAnimationHandler(); }

    /** Returns and clears entity side-effect events emitted by the last tick. */
    public List<EntityCombatEvent> consumeEntityEvents() { return context.entityBridge.consumeEntityEvents(); }

    /** Returns and clears handlers that restore Link's pre-entity final position. */
    public List<LinkFinalPositionRequest>
            consumeLinkFinalPositionRequests() { return context.entityBridge.consumeLinkFinalPositionRequests(); }

    /** Returns and clears Rooster's Link HRAM writes from the last entity tick. */
    public List<RoosterLinkStateRequest>
            consumeRoosterLinkStateRequests() { return context.entityBridge.consumeRoosterLinkStateRequests(); }

    /** Returns and clears handlers that block Link's next interactive motion frame. */
    public List<LinkMotionBlockRequest>
            consumeLinkMotionBlockRequests() { return context.entityBridge.consumeLinkMotionBlockRequests(); }

    /** Returns and clears side-view platform Link writes from the last entity tick. */
    public List<SideViewPlatformLinkRequest>
            consumeSideViewPlatformLinkRequests() { return context.entityBridge.consumeSideViewPlatformLinkRequests(); }

    /** Returns state-0 side-view pot Link writes from the last entity tick. */
    public List<SideViewPotLinkRequest>
            consumeSideViewPotLinkRequests() { return context.entityBridge.consumeSideViewPotLinkRequests(); }

    /** Returns entity-$61 WarpState3 Link writes and clears supported session actions. */
    public List<WarpLinkStateRequest>
    consumeWarpLinkStateRequests() { return context.entityBridge.consumeWarpLinkStateRequests(); }

    /** Returns and clears Hinox grab/throw writes for the last entity tick. */
    public List<HinoxLinkEffectRequest>
            consumeHinoxLinkEffectRequests() { return context.entityBridge.consumeHinoxLinkEffectRequests(); }

    /** Returns and clears explicit ROM Link fallen-pose requests. */
    public List<LinkFallenPoseRequest>
            consumeLinkFallenPoseRequests() { return context.entityBridge.consumeLinkFallenPoseRequests(); }

    /** Returns and clears a motion block emitted by a world handler rather than an entity. */
    public void setFreeMovementMode(boolean enabled) { context.entityBridge.setFreeMovementMode(enabled); }

    public boolean consumeWorldLinkMotionBlockRequest() { return context.entityBridge.consumeWorldLinkMotionBlockRequest(); }

    public List<LinkFacingRequest> consumeLinkFacingRequests() { return context.entityBridge.consumeLinkFacingRequests(); }

    public List<LinkAttackClearRequest>
            consumeLinkAttackClearRequests() { return context.entityBridge.consumeLinkAttackClearRequests(); }

    /** Returns lifted-throw attack-step writes for Main's post-entity phase. */
    public List<LinkAttackStepRequest>
            consumeLinkAttackStepRequests() { return context.entityBridge.consumeLinkAttackStepRequests(); }

    public List<LinkHeldItemPoseRequest>
            consumeLinkHeldItemPoseRequests() { return context.entityBridge.consumeLinkHeldItemPoseRequests(); }

    public boolean gotItemPresentationActive() { return context.entityBridge.gotItemPresentationActive(); }

    public boolean powerUpPickupSequenceActive() { return context.entityBridge.powerUpPickupSequenceActive(); }

    public boolean swordPickupSequenceActive() { return context.entityBridge.swordPickupSequenceActive(); }

    public List<LinkSwordSpinPoseRequest>
            consumeLinkSwordSpinPoseRequests() { return context.entityBridge.consumeLinkSwordSpinPoseRequests(); }

    public List<LinkSwordFinalPoseRequest>
            consumeLinkSwordFinalPoseRequests() { return context.entityBridge.consumeLinkSwordFinalPoseRequests(); }

    /** Returns and clears ROM screen-shake requests emitted by the last entity tick. */
    public List<ScreenShakeRequest> consumeScreenShakeRequests() { return context.entityBridge.consumeScreenShakeRequests(); }

    /** Returns and clears chest reward applications emitted by the last entity tick(s). */
    public List<ChestRewardEvent> consumeChestRewardEvents() { return context.entityBridge.consumeChestRewardEvents(); }

    /** Returns and clears key or hookshot rewards emitted by key-drop entities. */
    public List<KeyRewardEvent> consumeKeyRewardEvents() { return context.entityBridge.consumeKeyRewardEvents(); }

    /** Returns and clears Hiding Slime Key leaf-count updates emitted by entity handlers. */
    public List<SlimeKeyRewardEvent> consumeSlimeKeyRewardEvents() { return context.entityBridge.consumeSlimeKeyRewardEvents(); }

    /** Returns and clears completed boss heart-container rewards. */
    public List<HeartContainerRewardEvent>
            consumeHeartContainerRewards() { return context.entityBridge.consumeHeartContainerRewards(); }

    public List<MadamMeowMeowFullHealRequest>
            consumeMadamMeowMeowFullHealRequests() { return context.entityBridge.consumeMadamMeowMeowFullHealRequests(); }

    public List<RichardProgressEvent> consumeRichardProgressEvents() { return context.entityBridge.consumeRichardProgressEvents(); }

    public List<SwordPickupRewardEvent> consumeSwordPickupRewards() { return context.entityBridge.consumeSwordPickupRewards(); }

    public List<ToadstoolRewardEvent> consumeToadstoolRewards() { return context.entityBridge.consumeToadstoolRewards(); }

    public List<WitchExchangeEvent> consumeWitchExchangeEvents() { return context.entityBridge.consumeWitchExchangeEvents(); }

    public List<WitchRewardEvent> consumeWitchRewardEvents() { return context.entityBridge.consumeWitchRewardEvents(); }

    /** Returns the raw music-track request emitted by a chest, or {@code -1}. */
    public int consumePendingMusicTrack() { return context.entityBridge.consumePendingMusicTrack(); }

    public int consumePendingMusicFadeOutCountdown() { return context.entityBridge.consumePendingMusicFadeOutCountdown(); }

    /** Returns and clears Like Like capture/release requests from the last tick. */
    public List<LikeLikeEvent> consumeLikeLikeEvents() { return context.entityBridge.consumeLikeLikeEvents(); }

    /** Returns and clears ROM dialog requests emitted by the last entity tick. */
    public List<DialogRequest> consumeEntityDialogRequests() { return context.entityBridge.consumeEntityDialogRequests(); }

    /** Returns and clears source-shaped bomb-explosion interaction requests from the last tick. */
    public List<BombExplosionEvent> consumeBombExplosionEvents() { return context.entityBridge.consumeBombExplosionEvents(); }

    /** Advances the ROM's VBlank tile path, including switch blocks. */
    public void tickGameplayVBlank() { context.entityBridge.tickGameplayVBlank(); }

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
    public List<EntityCombatEvent> resolveEntityCombat(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight) { return context.entityBridge.resolveEntityCombat(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, swordCollisionActive, swordX, swordWidth, swordY, swordHeight); }

    public List<EntityCombatEvent> resolveEntityCombat(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight, EnemyAttackContext attackContext) { return context.entityBridge.resolveEntityCombat(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, attackContext); }

    public List<EntityCombatEvent> resolveEntityCombat(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, int linkVerticalVelocity, boolean linkInteractive, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight, EnemyAttackContext attackContext) { return context.entityBridge.resolveEntityCombat(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkVerticalVelocity, linkInteractive, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, attackContext); }

    public int clearEntity(int slot) { return context.entityBridge.clearEntity(slot); }

    /**
     * Applies the ROM pickup collision cadence to the active room entities.
     * Item-specific effects are returned to the gameplay layer so room state
     * and persistent entity state stay separate.
     */
    /** Link coordinates use ROM entity origins, not sprite top-left; direction uses Link constants. */
    public EntityPickupEvent collectEntityIfNeeded(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive) { return context.entityBridge.collectEntityIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive); }

    public EntityPickupEvent collectEntityIfNeeded(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive, int linkDirection) { return context.entityBridge.collectEntityIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection); }

    public EntityPickupEvent collectEntityIfNeeded(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive, int linkDirection, int linkZ) { return context.entityBridge.collectEntityIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection, linkZ); }

    public RoomBoundaryState boundaryState(int linkX, int linkY) { return context.roomCollisionQueries.boundaryState(linkX, linkY); }

    public RoomBoundaryState boundaryState(int linkX, int linkY, boolean linkAirborne) { return context.roomCollisionQueries.boundaryState(linkX, linkY, linkAirborne); }

    public RoomBoundaryState boundaryState(int linkX, int linkY, boolean linkAirborne, int linkPhysicsModifier) { return context.roomCollisionQueries.boundaryState(linkX, linkY, linkAirborne, linkPhysicsModifier); }

    void setActiveRoom(LoadedRoom room) { context.roomLoadCoordinator.setActiveRoom(room); }

    void setActiveRoom(LoadedRoom room, int[][] objectPalettes) { context.roomLoadCoordinator.setActiveRoom(room, objectPalettes); }

    void initializeSwitchBlockTiles() { context.roomLoadCoordinator.initializeSwitchBlockTiles(); }

    void synchronizeSwitchBlockCollisionState() { context.roomLoadCoordinator.synchronizeSwitchBlockCollisionState(); }

    void synchronizeFollowingNpcEntitiesIfNeeded() { context.roomStoryState.synchronizeFollowingNpcEntitiesIfNeeded(); }

    void configureEnemyDropRuntime() { context.roomLoadCoordinator.configureEnemyDropRuntime(); }

    boolean roomHasBossBattle() { return context.roomLoadCoordinator.roomHasBossBattle(); }

    void clearTransientRoomState() { context.roomTransientState.clearTransientRoomState(); }

    void applyBombObjectInteractions(List<BombExplosionEvent> events) { context.roomObjectMutations.applyBombObjectInteractions(events); }

    void applyBasicBombObjectCandidate(BombObjectInteraction.Candidate candidate) { context.roomObjectMutations.applyBasicBombObjectCandidate(candidate); }

    void applyBoomerangObjectInteractions( List<BoomerangObjectRequest> requests) { context.roomObjectMutations.applyBoomerangObjectInteractions(requests); }

    void applyMagicRodObjectInteractions( List<MagicRodObjectRequest> requests) { context.roomObjectMutations.applyMagicRodObjectInteractions(requests); }

    void applyMagicPowderObjectInteractions( List<MagicPowderObjectRequest> requests) { context.roomObjectMutations.applyMagicPowderObjectInteractions(requests); }

    boolean writeMagicPowderTorchObject(int location, int expectedObject, int replacementObject, boolean redraw) { return context.roomObjectMutations.writeMagicPowderTorchObject(location, expectedObject, replacementObject, redraw); }

    void retainTorchTiles(int areaIndex) { context.roomObjectMutations.retainTorchTiles(areaIndex); }

    void applyRetainedTorchTiles() { context.roomObjectMutations.applyRetainedTorchTiles(); }

    int countUnlitTorches() { return context.roomObjectMutations.countUnlitTorches(); }

    void copyIndoorTorchPalettesToActiveRoom() { context.roomObjectMutations.copyIndoorTorchPalettesToActiveRoom(); }

    void applyPuzzleBombObjectCandidate(BombObjectInteraction.Candidate candidate, int sourceSlot, int sourceZ) { context.roomObjectMutations.applyPuzzleBombObjectCandidate(candidate, sourceSlot, sourceZ); }

    void applyOutdoorBombableCaveDoorCandidate(BombObjectInteraction.Candidate candidate, int sourceSlot) { context.roomObjectMutations.applyOutdoorBombableCaveDoorCandidate(candidate, sourceSlot); }

    int objectAtRoomLocation(int location) {
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0)
            + (location & 0x0F);
        int[] objects = context.activeRoom.roomObjectsArea();
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
    public ChestOpenResult tryOpenChest(int linkPixelX, int linkPixelY, int linkDirection, boolean actionPressed, int swordLevel) { return context.roomDoorInteractions.tryOpenChest(linkPixelX, linkPixelY, linkDirection, actionPressed, swordLevel); }

    /** Mirrors bank 2's locked-door branch for physics categories $90-$93. */
    public boolean tryUnlockIndoorKeyDoor(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) { return context.roomDoorInteractions.tryUnlockIndoorKeyDoor(linkPixelX, linkPixelY, linkDirection, collisionType); }

    /** $02:70F2-$710F checks ROM Link coordinates against the authored door origin. */
    static boolean alignedWithIndoorKeyDoor(int location, int direction,
                                                    int linkPixelX, int linkPixelY) {
        int offset = direction < 2
            ? linkPixelX + 8 - ((location & 15) << 4) - 0x0C
            : linkPixelY + 16 - (location & 0xF0) - 0x14;
        return (offset & 0xFF) < 8;
    }

    /** Mirrors bank 2's $A7 collision counter and entity-$06 spawn branch. */
    public boolean tryPushIndoorBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) { return context.roomDoorInteractions.tryPushIndoorBlock(linkPixelX, linkPixelY, linkDirection, collisionType); }

    /** Mirrors bank 0's sustained Power Bracelet pull on indoor object $20/$8E. */
    public boolean tryLiftIndoorObject(int linkPixelX, int linkPixelY, int linkDirection, boolean braceletButtonHeld, int pressedButtonsMask) { return context.roomDoorInteractions.tryLiftIndoorObject(linkPixelX, linkPixelY, linkDirection, braceletButtonHeld, pressedButtonsMask); }

    /** Includes the source's shortened pull while Piece of Power is active. */
    public boolean tryLiftIndoorObject(int linkPixelX, int linkPixelY, int linkDirection, boolean braceletButtonHeld, int pressedButtonsMask, boolean pieceOfPowerActive) { return context.roomDoorInteractions.tryLiftIndoorObject(linkPixelX, linkPixelY, linkDirection, braceletButtonHeld, pressedButtonsMask, pieceOfPowerActive); }

    int entityPhysicsFlagsForTest(int slot) {
        return context.entityBridge.entityRuntime == null ? 0 : context.entityBridge.entityRuntime.physicsFlags(slot);
    }

    /** Dispatches bank 2's shared $A7/$DE interactive-block collision branch. */
    public boolean tryInteractWithIndoorBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) { return context.roomDoorInteractions.tryInteractWithIndoorBlock(linkPixelX, linkPixelY, linkDirection, collisionType); }

    /** Mirrors bank 2's $DE interactive-block branch and TryOpenLockedDoor. */
    public boolean tryUnlockIndoorKeyholeBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) { return context.roomDoorInteractions.tryUnlockIndoorKeyholeBlock(linkPixelX, linkPixelY, linkDirection, collisionType); }

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
    public boolean tryUnlockTailCaveKeyhole(int linkPixelX, int linkPixelY, int linkDirection, int collisionType, boolean hasTailKey) { return context.roomDoorInteractions.tryUnlockTailCaveKeyhole(linkPixelX, linkPixelY, linkDirection, collisionType, hasTailKey); }

    /** Mirrors bank 2's room-$B5 Slime Key branch of the shared keyhole handler. */
    public boolean tryUnlockKeyCavernKeyhole(int linkPixelX, int linkPixelY, int linkDirection, int collisionType, int goldenLeavesCount) { return context.roomDoorInteractions.tryUnlockKeyCavernKeyhole(linkPixelX, linkPixelY, linkDirection, collisionType, goldenLeavesCount); }

    boolean tryUnlockOverworldDungeonKeyhole( int linkPixelX, int linkPixelY, int linkDirection, int collisionType, int roomId, boolean hasKey, int dialogLowId) { return context.roomDoorInteractions.tryUnlockOverworldDungeonKeyhole(linkPixelX, linkPixelY, linkDirection, collisionType, roomId, hasKey, dialogLowId); }

    /** One RenderTranscientRumble step, called once per unpaused gameplay frame. */
    public int tickTailCaveKeyholeSequence() { return context.roomDoorInteractions.tickTailCaveKeyholeSequence(); }

    public boolean tailCaveKeyholeSequenceActive() { return context.roomDoorInteractions.tailCaveKeyholeSequenceActive(); }

    int tailCaveKeyholeCountdownForTest() { return context.roomDoorInteractions.tailCaveKeyholeCountdownForTest(); }

    int nextWorldMusicTrackCountdownForTest() { return context.roomDoorInteractions.nextWorldMusicTrackCountdownForTest(); }

    void writeTailCaveGateAnimationFrame(int countdown) { context.roomDoorInteractions.writeTailCaveGateAnimationFrame(countdown); }

    void tickNextWorldMusicTrackCountdown() { context.roomDoorInteractions.tickNextWorldMusicTrackCountdown(); }

    void writeBombPuzzleObject(int location, int objectId) { context.roomObjectMutations.writeBombPuzzleObject(location, objectId); }

    void refreshOverworldCollisionAfterObjectMutation() { context.roomObjectMutations.refreshOverworldCollisionAfterObjectMutation(); }

    void applyIndoorBombableWallCandidate(BombObjectInteraction.Candidate candidate, int sourceSlot) { context.roomObjectMutations.applyIndoorBombableWallCandidate(candidate, sourceSlot); }

    void queuePuzzleSolvedJingle(int sourceSlot) { context.roomObjectMutations.queuePuzzleSolvedJingle(sourceSlot); }

    boolean applyIndoorBombableBlockCandidate(BombObjectInteraction.Candidate candidate) { return context.roomObjectMutations.applyIndoorBombableBlockCandidate(candidate); }

    int adjacentIndoorRoomIdForBombWall(int wallIndex) { return context.scrollNavigation.adjacentIndoorRoomIdForBombWall(context.activeRoom, context.indoorMapPosition, wallIndex); }

    int findIndoorMapPosition(int mapId, int roomId) { return context.roomState.findIndoorMapPosition(mapId, roomId); }

    int indoorMapLayoutOffset(int mapId) { return context.roomState.indoorMapLayoutOffset(mapId); }

    byte[] indoorStatusTableForMap(int mapId) { return context.roomState.indoorStatusTableForMap(mapId); }

    int activeRoomStatusFlags() { return context.roomState.activeRoomStatusFlags(context.activeRoom); }

    void harvestKeyQuicksandEvents() { context.roomEntityRewards.harvestKeyQuicksandEvents(); }

    void harvestKeyRewardEvents() { context.roomEntityRewards.harvestKeyRewardEvents(); }

    void harvestHeartContainerRewards() { context.roomEntityRewards.harvestHeartContainerRewards(); }

    void harvestSwordPickupRewards() { context.roomEntityRewards.harvestSwordPickupRewards(); }

    void harvestToadstoolRewards() { context.roomEntityRewards.harvestToadstoolRewards(); }

    void harvestInstrumentRewards() { context.roomEntityRewards.harvestInstrumentRewards(); }

    void harvestInstrumentCompletions() { context.roomEntityRewards.harvestInstrumentCompletions(); }

    void harvestRichardProgressEvents() { context.roomEntityRewards.harvestRichardProgressEvents(); }

    void harvestWitchEvents() { context.roomEntityRewards.harvestWitchEvents(); }

    void harvestOwlEventCompletions() { context.roomEntityRewards.harvestOwlEventCompletions(); }

    void markHeartContainerCollected() { context.roomStoryState.markHeartContainerCollected(); }

    void harvestSlimeKeyRewardEvents() { context.roomEntityRewards.harvestSlimeKeyRewardEvents(); }

    void markActiveRoomCompleted() { context.roomStoryState.markActiveRoomCompleted(); }

    /** Mirrors EntityCheckThrowAtTriggers' $35..$3C shutter-object test. */
    void harvestThrownRoomEventCollisions() { context.roomPuzzleController.harvestThrownRoomEventCollisions(); }

    /** Executes the source's first common dungeon-event path used by Tail Cave. */
    void tickRoomEvent(int linkEntityX, int linkEntityY) { context.roomPuzzleController.tickRoomEvent(linkEntityX, linkEntityY); }

    void tickPushedBlockMotion(int linkEntityX, int linkEntityY) { context.roomPuzzleController.tickPushedBlockMotion(linkEntityX, linkEntityY); }

    boolean pushedBlocksTriggerResolved() { return context.roomPuzzleController.pushedBlocksTriggerResolved(); }

    void tickIndoorKeyDoorAnimation() { context.roomPuzzleController.tickIndoorKeyDoorAnimation(); }

    void replaceKeyDoorObjects(int direction, int location) { context.roomPuzzleController.replaceKeyDoorObjects(direction, location); }

    void replaceBossDoorObjects(int location) { context.roomPuzzleController.replaceBossDoorObjects(location); }

    boolean killAllEnemiesTriggerResolved() { return context.roomPuzzleController.killAllEnemiesTriggerResolved(); }

    void tickSwitchButton(int linkEntityX, int linkEntityY) { context.roomPuzzleController.tickSwitchButton(linkEntityX, linkEntityY); }

    void tickRoomEventChestReveal(int linkEntityX, int linkEntityY) { context.roomPuzzleController.tickRoomEventChestReveal(linkEntityX, linkEntityY); }

    /** Type-$04 poof calls func_002_5F5C when its countdown reaches four. */
    void tickRoomEventStairReveal() { context.roomPuzzleController.tickRoomEventStairReveal(); }

    /** Mirrors renderTranscientVFXs' hStaircase leave-and-return state machine. */
    public Warp pollStaircaseWarp(int linkEntityX, int linkEntityY, int linkZ, boolean carryingObject) { return context.roomTransitionController.pollStaircaseWarp(linkEntityX, linkEntityY, linkZ, carryingObject); }

    void configureStaircase(int location) { context.roomTransitionController.configureStaircase(location); }

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

    int colorShellObjectAt(RoomEntity entity, int relativeOffset) { return context.roomObjectMutations.colorShellObjectAt(entity, relativeOffset); }

    void writeColorShellObject(RoomEntity entity, int objectId) { context.roomObjectMutations.writeColorShellObject(entity, objectId); }

    void refreshActiveRoomTilemap() { context.roomTileRedraw.refreshActiveRoomTilemap(); }

    /** $02:599D selects each shutter; $02:5C04 closes it over eight frames. */
    boolean tickShutterClosing() { return context.roomPuzzleController.tickShutterClosing(); }

    void openActiveRoomShutterDoors() { context.roomPuzzleController.openActiveRoomShutterDoors(); }

    void openActiveRoomShutterDoors(boolean persistDirectionalStatus) { context.roomPuzzleController.openActiveRoomShutterDoors(persistDirectionalStatus); }

    RoomEntityObjectSample entityObjectSample(RoomEntity entity) { return context.roomCollisionQueries.entityObjectSample(entity); }

    RoomEntityObjectSample entityObjectIntersectionSample(RoomEntity entity) { return context.roomCollisionQueries.entityObjectIntersectionSample(entity); }

    void applyHookshotBridgeUpdates( List<HookshotBridgeUpdate> updates) { context.roomTileRedraw.applyHookshotBridgeUpdates(updates); }

    void applyHookshotBridgeTileOverrides() { context.roomTileRedraw.applyHookshotBridgeTileOverrides(); }

    void applyBombedWallTileOverrides() { context.roomTileRedraw.applyBombedWallTileOverrides(); }

    void applyBombedBlockTileOverrides() { context.roomTileRedraw.applyBombedBlockTileOverrides(); }

    void applyBombedCaveDoorTileOverrides() { context.roomTileRedraw.applyBombedCaveDoorTileOverrides(); }

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
    boolean entityBackgroundCollision(RoomEntity entity, int direction, int nextX, int nextY) { return context.roomCollisionQueries.entityBackgroundCollision(entity, direction, nextX, nextY); }

    EntityBackgroundCollisionResult entityBackgroundCollisionResult( RoomEntity entity, int direction, int nextX, int nextY) { return context.roomCollisionQueries.entityBackgroundCollisionResult(entity, direction, nextX, nextY); }

    EntityBackgroundCollisionResult entityBackgroundCollisionResult( RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown) { return context.roomCollisionQueries.entityBackgroundCollisionResult(entity, direction, nextX, nextY, ignoreHitsCountdown); }

    EntityBackgroundCollisionResult entityBackgroundCollisionResult( RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown, int frameCounter) { return context.roomCollisionQueries.entityBackgroundCollisionResult(entity, direction, nextX, nextY, ignoreHitsCountdown, frameCounter); }

    EntityBackgroundCollisionResult entityBackgroundCollisionResultForTest( RoomEntity entity, int direction, int nextX, int nextY) { return context.roomCollisionQueries.entityBackgroundCollisionResultForTest(entity, direction, nextX, nextY); }

    EntityBackgroundCollisionResult entityBackgroundCollisionResultForTest( RoomEntity entity, int direction, int nextX, int nextY, int frameCounter) { return context.roomCollisionQueries.entityBackgroundCollisionResultForTest(entity, direction, nextX, nextY, frameCounter); }

    /**
     * Mirrors the terrain/status and conveyor portion of bank-$03's
     * ApplyEntityInteractionWithBackground. Terrain physics and the entity
     * options byte stay ROM-backed; the runtime invokes this after the
     * entity-family handler has completed its movement for the frame.
     */
    RoomEntityGroundInteraction.Result entityGroundInteraction( RoomEntity entity, int frameCounter, int previousGroundStatus, int speedZ, boolean sideScrolling) { return context.roomCollisionQueries.entityGroundInteraction(entity, frameCounter, previousGroundStatus, speedZ, sideScrolling); }

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

    boolean pairoddProjectileObjectCollision(RoomEntity entity) { return context.roomCollisionQueries.pairoddProjectileObjectCollision(entity); }

    void loadRoomSpecificTilesIfNeeded(int roomId) { context.roomLoadCoordinator.loadRoomSpecificTilesIfNeeded(roomId); }

    int adjacentOverworldRoomId(int direction) { return context.roomTransitionController.adjacentOverworldRoomId(direction); }

    RoomScrollNavigation.IndoorRoomDestination adjacentIndoorRoom(int direction) { return context.roomTransitionController.adjacentIndoorRoom(direction); }

    static int scrollTarget(int direction) { return RoomScrollNavigation.scrollTarget(direction); }
}
