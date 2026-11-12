package linksawakening.world.entity;

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

    final EntitySpriteHandlerCatalog spriteHandlers;

    public FollowingNpcEntitySpawner(EntitySpriteHandlerCatalog spriteHandlers) {
        if (spriteHandlers == null) {
            throw new IllegalArgumentException("Entity sprite handlers cannot be null");
        }
        this.spriteHandlers = spriteHandlers;
    }

    /** Compatibility for world.room.RoomStoryState; use FollowingNpcSpawn. */
    @Deprecated
    public Result synchronize(
            RoomEntitySnapshot initial,
            FollowingNpcRoomContext room,
            FollowingNpcState requestedState,
            int linkX,
            int linkY,
            int linkZ,
            int entityYOffset,
            int linkDirection,
            LinkPositionHistory history) {
        return synchronize(new FollowingNpcSpawn(
                initial, room, requestedState, linkX, linkY, linkZ, entityYOffset, linkDirection, history));
    }

    public Result synchronize(FollowingNpcSpawn inputs) {
        return new FollowingNpcEntitySpawnerFrameStep(this, inputs).run();
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
            EntitySpriteSelection selection, Map<Integer, EntitySpriteDefinition> overrides) {
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
}
