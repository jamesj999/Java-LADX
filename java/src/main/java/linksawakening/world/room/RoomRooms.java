package linksawakening.world.room;

import linksawakening.world.entity.event.LiftedEntityState;

public interface RoomRooms {
    void loadInitialOverworld(int roomId);

    boolean musicSelectionIsScroll();

    void loadOverworld(int roomId);

    void loadIndoor(int mapId, int roomId);

    void loadIndoorFromSavedPosition(int mapId, int roomId, int savedIndoorMapPosition);

    void loadIndoor(int mapId, int roomId, int mapCategory);

    ActiveRoom activeRoom();

    boolean hasActiveRoom();

    int currentRoomId();

    int mapCategory();

    int mapId();

    int indoorRoomPositionForSave();

    int[][] palettes();

    boolean heartPiecePickupActive();

    String instrumentTrace();

    String moblinKingTrace();

    /** Owned snapshot, or null before the first room load. */
    RoomRenderSnapshot renderSnapshotOrNull();

    /** Compatibility name; prefer renderSnapshotOrNull(). */
    RoomRenderSnapshot renderSnapshot();

    /** Reused read-only render projection, valid until the next tick or room change. */
    /** Nullable until the first room load; otherwise a same-thread view valid until the next tick. */
    RoomRenderSnapshot.View renderView();

    /** Borrowed until next tick/room change; null before the first room load. */
    RoomRenderSnapshot.View renderViewOrNull();

    LiftedEntityState liftedEntityState();
}
