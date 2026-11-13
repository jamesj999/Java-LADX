package linksawakening.world.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.EntitySpriteSelection;

/**
 * ROM-shaped implementation of bank $01's CreateFollowingNpcEntity. The
 * routine is intentionally a room-load operation: handler movement and
 * per-frame follower history consumption remain in their entity handlers.
 */
public final class FollowingNpcEntitySpawner {
    public static final int ENTITY_BOW_WOW = 0x6D;
    public static final int ENTITY_MARIN_AT_THE_SHORE = 0xC1;
    public static final int ENTITY_GHOST = 0xD4;
    public static final int ENTITY_ROOSTER = 0xD5;

    private static final int MAP_S_FACE_SHRINE = 0x16;
    private static final int MAP_KANALET = 0x14;
    private static final int MAP_DREAM_SHRINE = 0x13;
    private static final int MAP_CAVE_B = 0x0A;
    private static final int ROOM_INDOOR_B_MANBO = 0xFD;
    private static final int ROOM_INDOOR_B_FISHING_MINIGAME = 0xB1;
    static final int ROOM_SECTION_OW_GHOST_TRIGGER = 0x40;
    static final int ROOM_INDOOR_B_MRS_MEOW_MEOW = 0xA7;

    // Optional ROM-backed data; absent in ROM-free or unconfigured presentation state.
    final EntitySpriteHandlerCatalog spriteHandlers;

    public FollowingNpcEntitySpawner(EntitySpriteHandlerCatalog spriteHandlers) {
        if (spriteHandlers == null) {
            throw new IllegalArgumentException("Entity sprite handlers cannot be null");
        }
        this.spriteHandlers = spriteHandlers;
    }

    public Result synchronize(FollowingNpcSpawn inputs) {
        return followingNpcEntitySpawnerFrameStep.run(this, inputs);
    }

    SpawnResult spawn(List<RoomEntity> slots, int type, int x, int y, EntitySpriteDefinition definition) {
        boolean changed = false;
        // CreateFollowingNpcEntity clears every active instance before calling
        // SpawnNewEntity, and both searches enumerate slots from $0F down.
        for (int slot = slots.size() - 1; slot >= 0; slot--) {
            RoomEntity entity = slots.get(slot);
            if (entity.type() == type && entity.loaded()) {
                slots.set(slot, RoomEntity.disabled(slot));
                changed = true;
            }
        }

        for (int slot = slots.size() - 1; slot >= 0; slot--) {
            if (!slots.get(slot).loaded()) {
                RoomEntity entity = new RoomEntity(
                        slot,
                        -1,
                        type,
                        x,
                        y,
                        EntityStatus.ACTIVE,
                        definition,
                        definition.initialVariant(),
                        0,
                        0,
                        0,
                        -1,
                        false);
                slots.set(slot, entity);
                return new SpawnResult(entity, true);
            }
        }
        return new SpawnResult(null, changed);
    }

    static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return EntitySnapshots.withMotion(
                entity, x, y, entity.spriteVariant(), entity.entityFlipAttribute(), entity.z(), -1, false);
    }

    static EntitySpriteSelection withOverrides(
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ EntitySpriteSelection
                    selection,
            Map<Integer, EntitySpriteDefinition> overrides) {
        return selection == null ? null : selection.withSpriteOverrides(overrides);
    }

    static boolean isEligibleRoom(FollowingNpcRoomContext room) {
        if (!room.indoor()) {
            return true;
        }
        if (room.sideScrolling()) {
            return false;
        }
        if (room.mapId() == MAP_S_FACE_SHRINE
                || room.mapId() == MAP_KANALET
                || room.mapId() == MAP_DREAM_SHRINE
                || room.mapId() < MAP_CAVE_B) {
            return false;
        }
        return room.roomId() != ROOM_INDOOR_B_MANBO && room.roomId() != ROOM_INDOOR_B_FISHING_MINIGAME;
    }

    static int byteValue(int value) {
        return value & 0xFF;
    }

    public record Result(RoomEntitySnapshot snapshot, FollowingNpcState state, boolean changed) {
        public Result {
            if (snapshot == null || state == null) {
                throw new IllegalArgumentException("Follower result cannot be null");
            }
        }
    }

    record SpawnResult(RoomEntity entity, boolean changed) {}

    final FollowingNpcEntitySpawnerFrameStep followingNpcEntitySpawnerFrameStep =
            new FollowingNpcEntitySpawnerFrameStep(this);

    static final class FollowingNpcEntitySpawnerFrameStep {
        private final FollowingNpcEntitySpawner owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.FollowingNpcSpawn inputs;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntitySnapshot initial;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.FollowingNpcRoomContext room;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.FollowingNpcState requestedState;
        private int linkX;
        private int linkY;
        private int linkZ;
        private int entityYOffset;
        private int linkDirection;
        // Optional room geometry/history; null preserves the path without that room input.
        private linksawakening.world.entity.LinkPositionHistory history;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private java.util.List<linksawakening.world.entity.RoomEntity> slots;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private java.util.Map<java.lang.Integer, linksawakening.entity.EntitySpriteDefinition> overrides;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.FollowingNpcState state;
        private boolean changed;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntitySnapshot snapshot;

        FollowingNpcEntitySpawnerFrameStep(FollowingNpcEntitySpawner owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.FollowingNpcEntitySpawner.Result run() {
            if (!prepareRoomAndRooster()) return completedResult;
            if (!spawnGhostAndMarin()) return completedResult;
            if (!spawnBowWowAndSnapshot()) return completedResult;

            return new FollowingNpcEntitySpawner.Result(snapshot, state, changed);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.FollowingNpcEntitySpawner.Result completedResult;

        private boolean prepareRoomAndRooster() {

            initial = inputs.initial();
            room = inputs.room();
            requestedState = inputs.requestedState();
            linkX = inputs.linkX();
            linkY = inputs.linkY();
            linkZ = inputs.linkZ();
            entityYOffset = inputs.entityYOffset();
            linkDirection = inputs.linkDirection();
            history = inputs.history();
            if (initial == null || room == null || requestedState == null || history == null) {
                throw new IllegalArgumentException("Follower synchronization inputs cannot be null");
            }
            if (!FollowingNpcEntitySpawner.isEligibleRoom(room)) {
                {
                    completedResult = new FollowingNpcEntitySpawner.Result(initial, requestedState, false);
                    return false;
                }
            }

            slots = new ArrayList<>(initial.slots());
            overrides = initial.spriteSelection() == null
                    ? new HashMap<>()
                    : new HashMap<>(initial.spriteSelection().spriteOverrides());
            state = requestedState;
            changed = false;

            if (state.roosterFollowing()) {
                FollowingNpcEntitySpawner.SpawnResult spawn = owner.spawn(
                        slots,
                        FollowingNpcEntitySpawner.ENTITY_ROOSTER,
                        FollowingNpcEntitySpawner.byteValue(linkX),
                        FollowingNpcEntitySpawner.byteValue(linkY + entityYOffset),
                        owner.spriteHandlers.forFollowerEntityType(FollowingNpcEntitySpawner.ENTITY_ROOSTER));
                changed |= spawn.changed();
                if (spawn.entity() != null) {
                    overrides.put(
                            FollowingNpcEntitySpawner.ENTITY_ROOSTER,
                            spawn.entity().spriteDefinition());
                }
            }
            return true;
        }

        private boolean spawnGhostAndMarin() {

            if (state.ghostFollowingState() == 1) {
                FollowingNpcEntitySpawner.SpawnResult spawn = owner.spawn(
                        slots,
                        FollowingNpcEntitySpawner.ENTITY_GHOST,
                        FollowingNpcEntitySpawner.byteValue(linkX),
                        FollowingNpcEntitySpawner.byteValue(linkY + entityYOffset),
                        owner.spriteHandlers.forFollowerEntityType(FollowingNpcEntitySpawner.ENTITY_GHOST));
                changed |= spawn.changed();
                if (spawn.entity() != null) {
                    overrides.put(
                            FollowingNpcEntitySpawner.ENTITY_GHOST,
                            spawn.entity().spriteDefinition());
                }
            } else if (state.ghostFollowingState() == 2) {
                boolean shouldFollow = !room.indoor()
                        && room.roomId() >= FollowingNpcEntitySpawner.ROOM_SECTION_OW_GHOST_TRIGGER
                        && (state.instrument4Flags() & 0x02) != 0
                        && state.powerBraceletLevel() < 0x02;
                state = state.withGhostFollowingState(shouldFollow ? 1 : 0);
                changed |= state.ghostFollowingState() != requestedState.ghostFollowingState();
            }

            if (state.marinFollowing()) {
                FollowingNpcEntitySpawner.SpawnResult spawn = owner.spawn(
                        slots,
                        FollowingNpcEntitySpawner.ENTITY_MARIN_AT_THE_SHORE,
                        FollowingNpcEntitySpawner.byteValue(linkX),
                        FollowingNpcEntitySpawner.byteValue(linkY + entityYOffset),
                        owner.spriteHandlers.forFollowerEntityType(
                                FollowingNpcEntitySpawner.ENTITY_MARIN_AT_THE_SHORE));
                changed |= spawn.changed();
                if (spawn.entity() != null) {
                    history.fill(linkX, linkY + entityYOffset, linkZ, linkDirection);
                    int x = FollowingNpcEntitySpawner.byteValue(linkX);
                    int y = FollowingNpcEntitySpawner.byteValue(linkY + entityYOffset);
                    if (state.db10()) {
                        x = FollowingNpcEntitySpawner.byteValue(x + 0x20);
                        y = FollowingNpcEntitySpawner.byteValue(y + 0x10);
                    }
                    RoomEntity marin = spawn.entity();
                    slots.set(marin.slot(), FollowingNpcEntitySpawner.withPosition(marin, x, y));
                    overrides.put(FollowingNpcEntitySpawner.ENTITY_MARIN_AT_THE_SHORE, marin.spriteDefinition());
                }
            }
            return true;
        }

        private boolean spawnBowWowAndSnapshot() {

            if (room.roomId() != FollowingNpcEntitySpawner.ROOM_INDOOR_B_MRS_MEOW_MEOW && state.bowWowFollowing()) {
                FollowingNpcEntitySpawner.SpawnResult spawn = owner.spawn(
                        slots,
                        FollowingNpcEntitySpawner.ENTITY_BOW_WOW,
                        FollowingNpcEntitySpawner.byteValue(linkX),
                        FollowingNpcEntitySpawner.byteValue(linkY),
                        owner.spriteHandlers.forFollowerEntityType(FollowingNpcEntitySpawner.ENTITY_BOW_WOW));
                changed |= spawn.changed();
                if (spawn.entity() != null) {
                    overrides.put(
                            FollowingNpcEntitySpawner.ENTITY_BOW_WOW,
                            spawn.entity().spriteDefinition());
                }
            }

            snapshot = new RoomEntitySnapshot(
                    slots,
                    FollowingNpcEntitySpawner.withOverrides(initial.spriteSelection(), overrides),
                    initial.spriteTiles(),
                    initial.sideScrolling());
            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.FollowingNpcEntitySpawner.Result run(
                FollowingNpcEntitySpawner owner, linksawakening.world.entity.FollowingNpcSpawn inputs) {
            if (inUse) {
                return new FollowingNpcEntitySpawnerFrameStep(owner).run(owner, inputs);
            }
            inUse = true;
            initial = null;
            room = null;
            requestedState = null;
            linkX = 0;
            linkY = 0;
            linkZ = 0;
            entityYOffset = 0;
            linkDirection = 0;
            history = null;
            slots = null;
            state = null;
            changed = false;
            snapshot = null;
            completedResult = null;

            this.inputs = inputs;
            try {
                return run();
            } finally {
                inUse = false;

                this.inputs = null;
                this.initial = null;
                this.room = null;
                this.requestedState = null;
                this.history = null;
                this.slots = null;
                this.state = null;
                this.snapshot = null;
                this.completedResult = null;
            }
        }
    }
}
