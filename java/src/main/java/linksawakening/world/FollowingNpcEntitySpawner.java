package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.EntitySpriteSelection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final int ROOM_SECTION_OW_GHOST_TRIGGER = 0x40;
    private static final int ROOM_INDOOR_B_MRS_MEOW_MEOW = 0xA7;

    private final EntitySpriteHandlerCatalog spriteHandlers;

    public FollowingNpcEntitySpawner(EntitySpriteHandlerCatalog spriteHandlers) {
        if (spriteHandlers == null) {
            throw new IllegalArgumentException("Entity sprite handlers cannot be null");
        }
        this.spriteHandlers = spriteHandlers;
    }

    /**
     * Applies the four source-ordered follower creation paths. Link values
     * are the ROM hLinkPositionX/Y/Z bytes; {@code entityYOffset} is wC13B.
     */
    public Result synchronize(RoomEntitySnapshot initial,
                              FollowingNpcRoomContext room,
                              FollowingNpcState requestedState,
                              int linkX,
                              int linkY,
                              int linkZ,
                              int entityYOffset,
                              int linkDirection,
                              LinkPositionHistory history) {
        if (initial == null || room == null || requestedState == null || history == null) {
            throw new IllegalArgumentException("Follower synchronization inputs cannot be null");
        }
        if (!isEligibleRoom(room)) {
            return new Result(initial, requestedState, false);
        }

        List<RoomEntity> slots = new ArrayList<>(initial.slots());
        Map<Integer, EntitySpriteDefinition> overrides = initial.spriteSelection() == null
            ? new HashMap<>()
            : new HashMap<>(initial.spriteSelection().spriteOverrides());
        FollowingNpcState state = requestedState;
        boolean changed = false;

        if (state.roosterFollowing()) {
            SpawnResult spawn = spawn(slots, ENTITY_ROOSTER, byteValue(linkX),
                byteValue(linkY + entityYOffset),
                spriteHandlers.forFollowerEntityType(ENTITY_ROOSTER));
            changed |= spawn.changed();
            if (spawn.entity() != null) {
                overrides.put(ENTITY_ROOSTER, spawn.entity().spriteDefinition());
            }
        }

        if (state.ghostFollowingState() == 1) {
            SpawnResult spawn = spawn(slots, ENTITY_GHOST, byteValue(linkX),
                byteValue(linkY + entityYOffset),
                spriteHandlers.forFollowerEntityType(ENTITY_GHOST));
            changed |= spawn.changed();
            if (spawn.entity() != null) {
                overrides.put(ENTITY_GHOST, spawn.entity().spriteDefinition());
            }
        } else if (state.ghostFollowingState() == 2) {
            boolean shouldFollow = !room.indoor()
                && room.roomId() >= ROOM_SECTION_OW_GHOST_TRIGGER
                && (state.instrument4Flags() & 0x02) != 0
                && state.powerBraceletLevel() < 0x02;
            state = state.withGhostFollowingState(shouldFollow ? 1 : 0);
            changed |= state.ghostFollowingState() != requestedState.ghostFollowingState();
        }

        if (state.marinFollowing()) {
            SpawnResult spawn = spawn(slots, ENTITY_MARIN_AT_THE_SHORE, byteValue(linkX),
                byteValue(linkY + entityYOffset),
                spriteHandlers.forFollowerEntityType(ENTITY_MARIN_AT_THE_SHORE));
            changed |= spawn.changed();
            if (spawn.entity() != null) {
                history.fill(linkX, linkY + entityYOffset, linkZ, linkDirection);
                int x = byteValue(linkX);
                int y = byteValue(linkY + entityYOffset);
                if (state.db10()) {
                    x = byteValue(x + 0x20);
                    y = byteValue(y + 0x10);
                }
                RoomEntity marin = spawn.entity();
                slots.set(marin.slot(), withPosition(marin, x, y));
                overrides.put(ENTITY_MARIN_AT_THE_SHORE, marin.spriteDefinition());
            }
        }

        if (room.roomId() != ROOM_INDOOR_B_MRS_MEOW_MEOW && state.bowWowFollowing()) {
            SpawnResult spawn = spawn(slots, ENTITY_BOW_WOW, byteValue(linkX), byteValue(linkY),
                spriteHandlers.forFollowerEntityType(ENTITY_BOW_WOW));
            changed |= spawn.changed();
            if (spawn.entity() != null) {
                overrides.put(ENTITY_BOW_WOW, spawn.entity().spriteDefinition());
            }
        }

        RoomEntitySnapshot snapshot = new RoomEntitySnapshot(slots,
            withOverrides(initial.spriteSelection(), overrides), initial.spriteTiles());
        return new Result(snapshot, state, changed);
    }

    private SpawnResult spawn(List<RoomEntity> slots, int type, int x, int y,
                              EntitySpriteDefinition definition) {
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
                RoomEntity entity = new RoomEntity(slot, -1, type, x, y, EntityStatus.ACTIVE,
                    definition, definition.initialVariant());
                slots.set(slot, entity);
                return new SpawnResult(entity, true);
            }
        }
        return new SpawnResult(null, changed);
    }

    private static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), entity.spriteVariant(),
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
    }

    private static EntitySpriteSelection withOverrides(EntitySpriteSelection selection,
                                                        Map<Integer, EntitySpriteDefinition> overrides) {
        return selection == null ? null : selection.withSpriteOverrides(overrides);
    }

    private static boolean isEligibleRoom(FollowingNpcRoomContext room) {
        if (!room.indoor()) {
            return true;
        }
        if (room.sideScrolling()) {
            return false;
        }
        if (room.mapId() == MAP_S_FACE_SHRINE || room.mapId() == MAP_KANALET
            || room.mapId() == MAP_DREAM_SHRINE || room.mapId() < MAP_CAVE_B) {
            return false;
        }
        return room.roomId() != ROOM_INDOOR_B_MANBO
            && room.roomId() != ROOM_INDOOR_B_FISHING_MINIGAME;
    }

    private static int byteValue(int value) {
        return value & 0xFF;
    }

    public record Result(RoomEntitySnapshot snapshot, FollowingNpcState state, boolean changed) {
        public Result {
            if (snapshot == null || state == null) {
                throw new IllegalArgumentException("Follower result cannot be null");
            }
        }
    }

    private record SpawnResult(RoomEntity entity, boolean changed) {
    }
}
