package linksawakening.world.room;

import linksawakening.world.object.BombObjectInteraction;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.BombExplosionEvent;

import linksawakening.world.entity.RoomEntityGroundInteraction;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.EnemyAttackContext;
import linksawakening.world.entity.RoomEntityObjectSample;
import linksawakening.world.entity.EntityPickupEvent;
import linksawakening.world.entity.FollowingNpcState;

import linksawakening.world.entity.event.LiftedEntityState;

import linksawakening.world.entity.event.HeartPiecePickupUpdate;

import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.RoomBoundaryState;
import linksawakening.world.transition.Warp;

import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
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
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.vfx.TransientVfxSystem;
import java.util.List;
import java.util.Map;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_WIDTH;

/** Public room API delegating to room-scoped collaborators. */
public final class RoomSession extends RoomSessionRules {
    final RoomSessionContext context;
    static long nextRoomSessionId;

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
        this(romData, gpu, roomLoader, overworldTilesetTable, overworldCollision,
            transientVfxSystem, droppableRupeeSystem, roomLoadListener,
            new linksawakening.world.entity.RomRandomByteSource());
    }

    /** Shares GetRandomByte's single wRandomSeed with equipment and world actions. */
    public RoomSession(byte[] romData,
                       GPU gpu,
                       RoomLoader roomLoader,
                       OverworldTilesetTable overworldTilesetTable,
                       OverworldCollision overworldCollision,
                       TransientVfxSystem transientVfxSystem,
                       DroppableRupeeSystem droppableRupeeSystem,
                       RoomLoadListener roomLoadListener,
                       linksawakening.world.entity.RomRandomByteSource randomByteSource) {
        context = new RoomSessionContext(this, romData, gpu, roomLoader, overworldTilesetTable,
            overworldCollision, transientVfxSystem, droppableRupeeSystem, roomLoadListener,
            randomByteSource);
    }
    public void loadInitialOverworld(int roomId) { context.roomLoadCoordinator.loadInitialOverworld(roomId); }
    /** Called only when a warp crosses the indoor/overworld boundary. */
    public void onMapBoundaryWarp(Runnable callback) {
        context.mapBoundaryWarp = java.util.Objects.requireNonNull(callback);
    }

    /** Source wSpawnLocationData, retained across scrolling but updated by non-side-view warps. */
    public Warp respawnPoint() { return context.respawnPoint; }
    public int respawnIndoorRoom() { return context.respawnIndoorRoom; }
    public void setRespawnPoint(Warp point, int indoorRoom) {
        context.respawnPoint = point;
        context.respawnIndoorRoom = indoorRoom;
    }
    /** $01:4244 clears only the Piece of Power counter, preserving the acorn counter. */
    public void clearGameOverPowerCounter() {
        context.enemyDropCounters = new EnemyDropResolver.CounterState(
            context.enemyDropCounters.guardianAcornCounter(), 0);
        configureEnemyDropRuntime();
    }
    public void resetGameOverSwitchBlocks() {
        context.recentRoomEntityClears.reset();
        context.switchBlocksState = 0;
        context.switchableObjectAnimationStage = 0;
    }
    public void loadWarpDestination(Warp warp) { context.roomTransitionController.loadWarpDestination(warp); }
    /** $00:0F53 countdown and $01:61EE dungeon-title dialog gates. */
    public int tickDungeonTitleMessage(boolean dialogActive, boolean inventoryClosed, int linkMotionState, boolean freeMovement) { return context.roomTransitionController.tickDungeonTitleMessage(dialogActive, inventoryClosed, linkMotionState, freeMovement); }
    public void startAdjacentOverworldScroll(ScrollController scrollController, int direction, int linkScreenX, int linkScreenY) { context.roomTransitionController.startAdjacentOverworldScroll(scrollController, direction, linkScreenX, linkScreenY); }
    void startMysteriousWoodsLostScroll(ScrollController scrollController, int linkScreenX, int linkScreenY) { context.roomTransitionController.startMysteriousWoodsLostScroll(scrollController, linkScreenX, linkScreenY); }
    public void startAdjacentIndoorScroll(ScrollController scrollController, int direction, int linkScreenX, int linkScreenY) { context.roomTransitionController.startAdjacentIndoorScroll(scrollController, direction, linkScreenX, linkScreenY); }
    /** Whether the most recent load follows room_transition.asm rather than the warp loader. */
    public boolean musicSelectionIsScroll() { return context.roomLoadCoordinator.musicSelectionIsScroll; }

    public void loadOverworld(int roomId) { context.roomLoadCoordinator.loadOverworld(roomId); }
    public void loadIndoor(int mapId, int roomId) { context.roomLoadCoordinator.loadIndoor(mapId, roomId); }
    /** Restores hMapRoom and the independently saved source wIndoorRoom byte. */
    public void loadIndoorFromSavedPosition(int mapId, int roomId, int savedIndoorMapPosition) { context.roomLoadCoordinator.loadIndoorFromSavedPosition(mapId, roomId, savedIndoorMapPosition); }
    public void loadIndoor(int mapId, int roomId, int mapCategory) { context.roomLoadCoordinator.loadIndoor(mapId, roomId, mapCategory); }
    void loadIndoor(int mapId, int roomId, int mapCategory, int transitionDirection) { context.roomLoadCoordinator.loadIndoor(mapId, roomId, mapCategory, transitionDirection); }
    /** Current ROM health byte for diagnostic captures. */
    public int entityHealth(int slot) { return context.roomSessionViews.entityHealth(slot); }
    public ActiveRoom activeRoom() { return context.roomSessionViews.activeRoom(); }
    /** Identifies the current load instance for Link room-entry state. */
    public long roomLoadSerial() { return context.roomSessionViews.roomLoadSerial(); }
    /** Distinguishes same-room loads performed by different live sessions. */
    public long roomLoadToken() { return context.roomSessionViews.roomLoadToken(); }
    public boolean hasActiveRoom() { return context.roomSessionViews.hasActiveRoom(); }
    public boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) { return context.roomStoryState.applyMarinWakeUpPresentation(x, y, spriteVariant); }
    /** Tarin's indoor handler updates his facing every 32 frames, even during the bed scene. */
    public void updateHouseTarinPresentation(int frameCounter, int linkX, int linkY) { context.roomStoryState.updateHouseTarinPresentation(frameCounter, linkX, linkY); }
    /** Supplies the patched EntityInitKeyDropPoint inventory byte. */
    public void setBirdKeyOwned(boolean owned) { context.roomStoryState.setBirdKeyOwned(owned); }
    /** Supplies wHasTailKey to the active owl-event handler. */
    public void setTailKeyOwned(boolean owned) { context.roomStoryState.setTailKeyOwned(owned); }
    public int currentRoomId() { return context.roomSessionViews.currentRoomId(); }
    public int mapCategory() { return context.roomSessionViews.mapCategory(); }
    public int mapId() { return context.roomSessionViews.mapId(); }
    /**
     * Returns the source {@code wIndoorRoom} value for the active indoor map.
     * A negative result means this map has no ROM layout table, so the caller
     * should preserve the existing save byte rather than inventing one.
     */
    public int indoorRoomPositionForSave() { return context.roomSessionViews.indoorRoomPositionForSave(); }
    /** Room-status bytes in the active dungeon's 8x8 ROM map order. */
    public byte[] dungeonMinimapRoomStatusSnapshot() { return context.roomSessionViews.dungeonMinimapRoomStatusSnapshot(); }
    public byte[] overworldRoomStatusSnapshot() { return context.roomSessionViews.overworldRoomStatusSnapshot(); }
    public byte[] indoorARoomStatusSnapshot() { return context.roomSessionViews.indoorARoomStatusSnapshot(); }
    public byte[] indoorBRoomStatusSnapshot() { return context.roomSessionViews.indoorBRoomStatusSnapshot(); }
    public byte[] colorDungeonRoomStatusSnapshot() { return context.roomSessionViews.colorDungeonRoomStatusSnapshot(); }
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
    public void setDungeonItemForDebug(int mapId, int index, int value) {
        context.roomState.dungeonItemState.setFlagForDebug(mapId, index, value);
    }
    public byte[] dungeonItemFlagsSnapshot() { return context.roomSessionViews.dungeonItemFlagsSnapshot(); }
    public byte[] colorDungeonItemFlagsSnapshot() { return context.roomSessionViews.colorDungeonItemFlagsSnapshot(); }
    public byte[] currentDungeonItemFlagsSnapshot() { return context.roomSessionViews.currentDungeonItemFlagsSnapshot(); }
    public void restoreDungeonItemFlags(byte[] dungeonFlags, byte[] colorFlags) { context.roomSessionViews.restoreDungeonItemFlags(dungeonFlags, colorFlags); }
    /** Snapshots wHasInstrument1..wHasInstrument8 (DB65..DB6C). */
    public byte[] dungeonProgressFlagsSnapshot() { return context.roomSessionViews.dungeonProgressFlagsSnapshot(); }
    public void restoreDungeonProgressFlags(byte[] flags) { context.roomSessionViews.restoreDungeonProgressFlags(flags); }
    int overworldRoomStatusForTest(int roomId) { return context.roomSessionViews.overworldRoomStatusForTest(roomId); }
    int indoorRoomStatusForTest(int mapId, int roomId) { return context.roomSessionViews.indoorRoomStatusForTest(mapId, roomId); }
    void setEnemyDropCountersForTest(int droppedItemCount, int droppedBombArrowCount) { context.roomSessionViews.setEnemyDropCountersForTest(droppedItemCount, droppedBombArrowCount); }
    /** Accepted contact hurt, including enemy projectiles ($03:6DB8). */
    public void resetGuardianAcornCounterOnHurt() {
        var runtime = context.entityBridge.entityRuntime;
        var counters = runtime == null ? context.enemyDropCounters
            : runtime.inspection().enemyDropCounters();
        context.enemyDropCounters = EnemyDropResolver.resetGuardianAcornCounter(counters);
        if (runtime != null) runtime.configuration().setEnemyDropCounters(context.enemyDropCounters);
    }

    EnemyDropResolver.CounterState enemyDropCountersForTest() { return context.roomSessionViews.enemyDropCountersForTest(); }
    void setSwitchButtonPressedForTest(int value) { context.roomSessionViews.setSwitchButtonPressedForTest(value); }
    int switchButtonPressedForTest() { return context.roomSessionViews.switchButtonPressedForTest(); }
    public int[][] palettes() { return context.roomSessionViews.palettes(); }
    public boolean heartPiecePickupActive() { return context.roomSessionViews.heartPiecePickupActive(); }
    public void setHeartPiecesCount(int count) { context.roomSessionViews.setHeartPiecesCount(count); }
    public List<HeartPiecePickupUpdate> consumeHeartPieceUpdates() { return context.roomSessionViews.consumeHeartPieceUpdates(); }
    public String instrumentTrace() { return context.roomSessionViews.instrumentTrace(); }
    public String moblinKingTrace() { return context.roomSessionViews.moblinKingTrace(); }
    public RoomRenderSnapshot renderSnapshot() { return context.roomSessionViews.renderSnapshot(); }
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
    public void setHouseNpcTradeItem(int tradeItem) { context.roomStoryState.setHouseNpcTradeItem(tradeItem); }

    public void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) { context.roomStoryState.setChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel); }
    /** Supplies the two WRAM values checked by SleepyToadstoolEntityHandler. */
    public void setToadstoolPlayerState(boolean hasToadstool, int magicPowderCount) { context.entityInputs.setToadstoolPlayerState(hasToadstool, magicPowderCount); }
    public boolean inBossBattle() {
        return context.entityBridge.entityRuntime != null
            && context.entityBridge.entityRuntime.inspection().inBossBattle();
    }
    public int entityDefaultMusicTrack() { return context.entityDefaultMusicTrack; }

    public void setEntityDefaultMusicTrack(int trackId) { context.roomStoryState.setEntityDefaultMusicTrack(trackId); }
    void updateWitchEnvironment() { context.roomStoryState.updateWitchEnvironment(); }
    /** Supplies wGoldenLeavesCount to the live Hiding Slime Key handler. */
    public void setEntityGoldenLeavesCount(int goldenLeavesCount) { context.entityInputs.setEntityGoldenLeavesCount(goldenLeavesCount); }
    /** Supplies wScreenShakeCountdown to ceiling-triggered entity handlers. */
    public void setEntityScreenShakeCountdown(int countdown) {
        if (context.entityBridge.entityRuntime != null) context.entityBridge.entityRuntime.configuration().setScreenShakeCountdown(countdown);
    }

    /** Supplies the live shake/collision values used by tree shells. */
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
    public LiftedEntityState liftedEntityState() { return context.roomSessionViews.liftedEntityState(); }
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
    int bombDirectionForTest(int slot) { return context.roomSessionViews.bombDirectionForTest(slot); }
    /** Returns whether the live room currently owns entity {@code $03}. */
    public boolean hookshotActive() { return context.roomPlayerItemActions.hookshotActive(); }
    /** Returns whether the live room currently owns the boomerang entity. */
    public boolean boomerangActive() { return context.roomPlayerItemActions.boomerangActive(); }
    public void setColorShellSoundSink(GameplaySoundSink soundSink) { context.roomSessionViews.setColorShellSoundSink(soundSink); }
    void setColorShellStateForTest(int slot, int state, int transitionCountdown, int direction, int speedX, int speedY) { context.roomSessionViews.setColorShellStateForTest(slot, state, transitionCountdown, direction, speedX, speedY); }
    int entityGroundStatusForTest(int slot) { return context.roomSessionViews.entityGroundStatusForTest(slot); }
    void setEntityIgnoreHitsCountdownForTest(int slot, int value) { context.roomSessionViews.setEntityIgnoreHitsCountdownForTest(slot, value); }
    void setEntityThrownDirectionForTest(int slot, int value) { context.roomSessionViews.setEntityThrownDirectionForTest(slot, value); }
    void setEntityLedgeTimerForTest(int slot, int value) { context.roomSessionViews.setEntityLedgeTimerForTest(slot, value); }
    void setEntitySwitchBlocksStateForTest(int value) { context.roomSessionViews.setEntitySwitchBlocksStateForTest(value); }
    void setSwitchableObjectAnimationStageForTest(int value) { context.roomSessionViews.setSwitchableObjectAnimationStageForTest(value); }
    int entitySwitchBlocksStateForTest() { return context.roomSessionViews.entitySwitchBlocksStateForTest(); }
    int switchableObjectAnimationStageForTest() { return context.roomSessionViews.switchableObjectAnimationStageForTest(); }
    boolean linkCollisionPointBlockedForTest(int pixelX, int pixelY) { return context.roomSessionViews.linkCollisionPointBlockedForTest(pixelX, pixelY); }
    int entityLedgeTimerForTest(int slot) { return context.roomSessionViews.entityLedgeTimerForTest(slot); }
    int entityFallingTargetXForTest(int slot) { return context.roomSessionViews.entityFallingTargetXForTest(slot); }
    int entityTransitionCountdownForTest(int slot) { return context.roomSessionViews.entityTransitionCountdownForTest(slot); }
    int roomTriggerCountForTest() { return context.roomSessionViews.roomTriggerCountForTest(); }
    int activeRoomEventForTest() { return context.roomSessionViews.activeRoomEventForTest(); }
    boolean hasDungeonInstrumentForTest(int mapId) { return context.roomSessionViews.hasDungeonInstrumentForTest(mapId); }
    int indoorTorchPaletteEffectAddressForTest() { return context.roomSessionViews.indoorTorchPaletteEffectAddressForTest(); }
    void igniteIndoorTorchPaletteForTest() { context.roomSessionViews.igniteIndoorTorchPaletteForTest(); }
    void setRoomTriggerCountForTest(int value) { context.roomSessionViews.setRoomTriggerCountForTest(value); }
    int entityDropSpeedXForTest(int slot) { return context.roomSessionViews.entityDropSpeedXForTest(slot); }
    int entityDropSpeedZForTest(int slot) { return context.roomSessionViews.entityDropSpeedZForTest(slot); }
    void replaceEntityRuntimeForTest(RoomEntityRuntime runtime) { context.roomSessionViews.replaceEntityRuntimeForTest(runtime); }
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
    public boolean deferContactRecoil(int slot, int linkX, int linkY) { return context.entityBridge.deferContactRecoil(slot, linkX, linkY); }
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
    static int romDirectionForProjectileCollision(int linkDirection) { return RoomInteractionGeometry.romDirectionForProjectileCollision(linkDirection); }
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
    public EntityPickupEvent collectEntityIfNeeded(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive, int linkDirection, int linkZ, boolean swordActive, int swordX, int swordWidth, int swordY, int swordHeight) { return context.entityBridge.collectEntityIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection, linkZ, swordActive, swordX, swordWidth, swordY, swordHeight); }
    public RoomBoundaryState boundaryState(int linkX, int linkY) { return context.roomCollisionQueries.boundaryState(linkX, linkY); }
    public RoomBoundaryState boundaryState(int linkX, int linkY, boolean linkAirborne) { return context.roomCollisionQueries.boundaryState(linkX, linkY, linkAirborne); }
    public RoomBoundaryState boundaryState(int linkX, int linkY, boolean linkAirborne, int linkPhysicsModifier) { return context.roomCollisionQueries.boundaryState(linkX, linkY, linkAirborne, linkPhysicsModifier); }
    void setActiveRoom(LoadedRoom room) { context.roomLoadCoordinator.setActiveRoom(room); }
    void setActiveRoom(LoadedRoom room, int[][] objectPalettes) { context.roomLoadCoordinator.setActiveRoom(room, objectPalettes); }
    void initializeSwitchBlockTiles() { context.roomLoadCoordinator.initializeSwitchBlockTiles(); }
    void synchronizeSwitchBlockCollisionState() { context.roomLoadCoordinator.synchronizeSwitchBlockCollisionState(); }
    void synchronizeFollowingNpcEntitiesIfNeeded() { context.roomStoryState.synchronizeFollowingNpcEntitiesIfNeeded(); }
    public void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp, boolean lowHealth) {
        setEnemyDropPlayerState(maxHearts, health, activePowerUp);
        context.enemyDropLowHealth = lowHealth;
        configureEnemyDropRuntime();
    }

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
    int objectAtRoomLocation(int location) { return context.roomSessionViews.objectAtRoomLocation(location); }
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
    static boolean alignedWithIndoorKeyDoor(int location, int direction, int linkPixelX, int linkPixelY) { return RoomInteractionGeometry.alignedWithIndoorKeyDoor(location, direction, linkPixelX, linkPixelY); }
    /** Mirrors bank 2's $A7 collision counter and entity-$06 spawn branch. */
    public boolean tryPushIndoorBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) { return context.roomDoorInteractions.tryPushIndoorBlock(linkPixelX, linkPixelY, linkDirection, collisionType); }
    /** Mirrors bank 0's sustained Power Bracelet pull on indoor object $20/$8E. */
    public void setLiftStrengthBoost(boolean redTunic, boolean pieceOfPower) {
        if (context.entityBridge.entityRuntime != null) {
            context.entityBridge.entityRuntime.configuration().setLiftStrengthBoost(redTunic, pieceOfPower);
        }
    }

    public boolean tryLiftObject(int x, int y, int direction, boolean held, int buttons, boolean power, int level, boolean anyPowerUp) {
        return context.roomDoorInteractions.tryLiftObject(x, y, direction, held, buttons, power, level, anyPowerUp);
    }

    public boolean tryLiftObject(int x, int y, int direction, boolean held, int buttons, boolean power, int level) {
        return context.roomDoorInteractions.tryLiftObject(x, y, direction, held, buttons, power, level);
    }

    public boolean tryLiftIndoorObject(int linkPixelX, int linkPixelY, int linkDirection, boolean braceletButtonHeld, int pressedButtonsMask) { return context.roomDoorInteractions.tryLiftIndoorObject(linkPixelX, linkPixelY, linkDirection, braceletButtonHeld, pressedButtonsMask); }
    /** Includes the source's shortened pull while Piece of Power is active. */
    public boolean tryLiftIndoorObject(int linkPixelX, int linkPixelY, int linkDirection, boolean braceletButtonHeld, int pressedButtonsMask, boolean pieceOfPowerActive) { return context.roomDoorInteractions.tryLiftIndoorObject(linkPixelX, linkPixelY, linkDirection, braceletButtonHeld, pressedButtonsMask, pieceOfPowerActive); }
    int entityPhysicsFlagsForTest(int slot) { return context.roomSessionViews.entityPhysicsFlagsForTest(slot); }
    /** Dispatches bank 2's shared $A7/$DE interactive-block collision branch. */
    public boolean tryInteractWithIndoorBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) { return context.roomDoorInteractions.tryInteractWithIndoorBlock(linkPixelX, linkPixelY, linkDirection, collisionType); }
    /** Mirrors bank 2's $DE interactive-block branch and TryOpenLockedDoor. */
    public boolean tryUnlockIndoorKeyholeBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) { return context.roomDoorInteractions.tryUnlockIndoorKeyholeBlock(linkPixelX, linkPixelY, linkDirection, collisionType); }
    static int keyDoorDirectionIndex(int linkDirection) { return RoomInteractionGeometry.keyDoorDirectionIndex(linkDirection); }
    static int keyDoorCollisionBit(int direction) { return RoomInteractionGeometry.keyDoorCollisionBit(direction); }
    static int[][] keyDoorCollisionPoints(int x, int y, int direction) { return RoomInteractionGeometry.keyDoorCollisionPoints(x, y, direction); }
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
    int adjacentIndoorRoomIdForBombWall(int wallIndex) { return context.roomSessionViews.adjacentIndoorRoomIdForBombWall(wallIndex); }
    int findIndoorMapPosition(int mapId, int roomId) { return context.roomSessionViews.findIndoorMapPosition(mapId, roomId); }
    int indoorMapLayoutOffset(int mapId) { return context.roomSessionViews.indoorMapLayoutOffset(mapId); }
    byte[] indoorStatusTableForMap(int mapId) { return context.roomSessionViews.indoorStatusTableForMap(mapId); }
    int activeRoomStatusFlags() { return context.roomSessionViews.activeRoomStatusFlags(); }
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
    static boolean withinUnsignedByteRange(int value, int center, int bias, int size) { return RoomInteractionGeometry.withinUnsignedByteRange(value, center, bias, size); }
    static int roomEventChestTop(int linkEntityX, int linkEntityY) { return RoomInteractionGeometry.roomEventChestTop(linkEntityX, linkEntityY); }
    static void requireLength(byte[] values, int expectedLength, String label) { RoomInteractionGeometry.requireLength(values, expectedLength, label); }
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
    static int hookshotObjectAreaIndex(int objectLeft, int objectTop) { return RoomInteractionGeometry.hookshotObjectAreaIndex(objectLeft, objectTop); }
    static int colorShellObjectIndex(RoomEntity entity) { return RoomInteractionGeometry.colorShellObjectIndex(entity); }
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
    static RoomEntityGroundInteraction.PitTransition pitTransitionFor( RoomEntity entity, OverworldCollision.GroundInteractionSample sample, int linkMotionState) { return RoomInteractionGeometry.pitTransitionFor(entity, sample, linkMotionState); }
    static int groundStatusFor(OverworldCollision.GroundInteractionSample sample) { return RoomInteractionGeometry.groundStatusFor(sample); }
    static boolean splashAllowed(int options, int previousGroundStatus, int currentGroundStatus, int speedZ, boolean sideScrolling) { return RoomInteractionGeometry.splashAllowed(options, previousGroundStatus, currentGroundStatus, speedZ, sideScrolling); }
    static RoomEntity withEntityPosition(RoomEntity entity, int x, int y) { return RoomInteractionGeometry.withEntityPosition(entity, x, y); }
    boolean pairoddProjectileObjectCollision(RoomEntity entity) { return context.roomCollisionQueries.pairoddProjectileObjectCollision(entity); }
    void loadRoomSpecificTilesIfNeeded(int roomId) { context.roomLoadCoordinator.loadRoomSpecificTilesIfNeeded(roomId); }
    int adjacentOverworldRoomId(int direction) { return context.roomTransitionController.adjacentOverworldRoomId(direction); }
    RoomScrollNavigation.IndoorRoomDestination adjacentIndoorRoom(int direction) { return context.roomTransitionController.adjacentIndoorRoom(direction); }
    static int scrollTarget(int direction) { return RoomInteractionGeometry.scrollTarget(direction); }
}
