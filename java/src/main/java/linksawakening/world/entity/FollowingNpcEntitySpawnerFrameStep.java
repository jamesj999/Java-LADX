package linksawakening.world.entity;

import java.util.ArrayList;
import java.util.HashMap;

/** Per-invocation scratch for FollowingNpcEntitySpawner.synchronize; values are assigned in source order. */
final class FollowingNpcEntitySpawnerFrameStep {
    private final FollowingNpcEntitySpawner owner;
    private linksawakening.world.entity.FollowingNpcSpawn inputs;
    private linksawakening.world.entity.RoomEntitySnapshot initial;
    private linksawakening.world.entity.FollowingNpcRoomContext room;
    private linksawakening.world.entity.FollowingNpcState requestedState;
    private int linkX;
    private int linkY;
    private int linkZ;
    private int entityYOffset;
    private int linkDirection;
    private linksawakening.world.entity.LinkPositionHistory history;
    private java.util.List<linksawakening.world.entity.RoomEntity> slots;
    private java.util.Map<java.lang.Integer, linksawakening.entity.EntitySpriteDefinition> overrides;
    private linksawakening.world.entity.FollowingNpcState state;
    private boolean changed;
    private linksawakening.world.entity.RoomEntitySnapshot snapshot;

    FollowingNpcEntitySpawnerFrameStep(
            FollowingNpcEntitySpawner owner, linksawakening.world.entity.FollowingNpcSpawn inputs) {
        this.owner = owner;
        this.inputs = inputs;
    }

    linksawakening.world.entity.FollowingNpcEntitySpawner.Result run() {
        if (!prepareRoomAndRooster()) return completedResult;
        if (!spawnGhostAndMarin()) return completedResult;
        if (!spawnBowWowAndSnapshot()) return completedResult;

        return new FollowingNpcEntitySpawner.Result(snapshot, state, changed);
    }

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
                        FollowingNpcEntitySpawner.ENTITY_ROOSTER, spawn.entity().spriteDefinition());
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
                        FollowingNpcEntitySpawner.ENTITY_GHOST, spawn.entity().spriteDefinition());
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
                    owner.spriteHandlers.forFollowerEntityType(FollowingNpcEntitySpawner.ENTITY_MARIN_AT_THE_SHORE));
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
                        FollowingNpcEntitySpawner.ENTITY_BOW_WOW, spawn.entity().spriteDefinition());
            }
        }

        snapshot = new RoomEntitySnapshot(
                slots,
                FollowingNpcEntitySpawner.withOverrides(initial.spriteSelection(), overrides),
                initial.spriteTiles(),
                initial.sideScrolling());
        return true;
    }
}
