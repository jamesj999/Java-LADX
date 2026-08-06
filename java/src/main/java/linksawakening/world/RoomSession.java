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
    private static final int ENTITY_OPT1_NO_GROUND_INTERACTION = 0x10;
    private static final int ENTITY_OPT1_SPLASH_IN_WATER = 0x08;
    private static final int ENTITY_FISH = 0xCC;
    private static final int ENTITY_PEAHAT = 0xA0;
    private static final int ENTITY_ROOSTER = 0xD5;
    private static final int ENTITY_BOW_WOW = 0x6D;
    private static final int ENTITY_MARIN_AT_THE_SHORE = 0xC1;
    private static final int ENTITY_HEART_CONTAINER = 0x36;
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
    private final RomTables romTables;
    private final EnemyDropResolver enemyDropResolver;
    private final EntityCollisionPointProbe entityCollisionPointProbe;
    private final EntityBackgroundCollisionResolver entityBackgroundCollisionResolver;
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
    private int followingLinkX = 0x08;
    private int followingLinkY = 0x10;
    private int followingLinkZ;
    private int followingEntityYOffset;
    private int followingLinkDirection;
    private boolean followingNpcRoomNeedsSync;
    private boolean actionButtonsHeld;
    private boolean powerBraceletButtonHeld;
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
        this.romTables = RomTables.loadFromRom(romData);
        this.entityBackgroundCollisionResolver = new EntityBackgroundCollisionResolver(romTables);
        this.enemyDropResolver = new EnemyDropResolver(romData);
        this.entityCollisionPointProbe = new EntityCollisionPointProbe(romTables);
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

    /** Supplies the held J_A|J_B state consumed by input-driven entity handlers. */
    public void setEntityActionButtonsHeld(boolean actionButtonsHeld) {
        this.actionButtonsHeld = actionButtonsHeld;
        if (entityRuntime != null) {
            entityRuntime.setActionButtonsHeld(actionButtonsHeld);
        }
    }

    /** Supplies the held A/B state used by EntityGetLiftedUp. */
    public void setEntityPowerBraceletButtonHeld(boolean buttonHeld) {
        this.powerBraceletButtonHeld = buttonHeld;
        if (entityRuntime != null) {
            entityRuntime.setPowerBraceletButtonHeld(buttonHeld);
        }
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
    }

    int entitySwitchBlocksStateForTest() {
        return switchBlocksState & 0xFF;
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
        followingLinkX = linkEntityX & 0xFF;
        followingLinkY = linkEntityY & 0xFF;
        followingLinkZ = linkEntityZ & 0xFF;
        followingLinkDirection = linkDirection & 0xFF;
        currentLinkMotionState = linkMotionState & 0xFF;
        if (activeRoom == null || entityRuntime == null) {
            return List.of();
        }
        entityRuntime.setActionButtonsHeld(actionButtonsHeld);
        entityRuntime.setPowerBraceletButtonHeld(powerBraceletButtonHeld);
        entityRuntime.setLiftedLinkC13B(followingEntityYOffset);
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
        enemyDropCounters = entityRuntime.enemyDropCounters();
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
            entityRandomByteSource, entitySpriteHandlerCatalog, enemyCombatTables);
        if (entityRuntime != null) {
            entityRuntime.setColorShellWorld(colorShellWorld);
            entityRuntime.setFollowingNpcState(followingNpcState);
            entityRuntime.setEntityMapId(activeRoom.mapId());
            entityRuntime.setGroundInteraction(this::entityGroundInteraction);
            entityRuntime.setBackgroundInteraction(entityBackgroundInteraction);
            entityRuntime.setGroundInteractionSideScrolling(
                activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL);
            entityRuntime.setActionButtonsHeld(actionButtonsHeld);
            entityRuntime.setPowerBraceletButtonHeld(powerBraceletButtonHeld);
            entityRuntime.setLiftedLinkC13B(followingEntityYOffset);
            configureEnemyDropRuntime();
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
            entityRandomByteSource, entitySpriteHandlerCatalog, enemyCombatTables);
        entityRuntime.setColorShellWorld(colorShellWorld);
        entityRuntime.setFollowingNpcState(followingNpcState);
        entityRuntime.setEntityMapId(activeRoom.mapId());
        entityRuntime.setGroundInteraction(this::entityGroundInteraction);
        entityRuntime.setBackgroundInteraction(entityBackgroundInteraction);
        entityRuntime.setGroundInteractionSideScrolling(
            activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL);
        entityRuntime.setActionButtonsHeld(actionButtonsHeld);
        entityRuntime.setPowerBraceletButtonHeld(powerBraceletButtonHeld);
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
            thrownDirection, ledgeTimer, switchBlocksState);
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
