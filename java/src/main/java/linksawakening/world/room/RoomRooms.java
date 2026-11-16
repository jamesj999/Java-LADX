package linksawakening.world.room;

import linksawakening.world.entity.event.LiftedEntityState;

public interface RoomRooms {
    void loadInitialOverworld(int roomId);

    boolean musicSelectionIsScroll();

    void loadOverworld(int roomId);

    void loadIndoor(int mapId, int roomId);

    void loadIndoorFromSavedPosition(int mapId, int roomId, int savedIndoorMapPosition);

    void loadIndoor(int mapId, int roomId, int mapCategory);

    /** Current room data, or null before the first room load. */
    ActiveRoom activeRoomOrNull();

    /** Non-null current room; throws NullPointerException before the first room load. */
    ActiveRoom requireActiveRoom();

    boolean hasActiveRoom();

    int currentRoomId();

    int mapCategory();

    int mapId();

    int indoorRoomPositionForSave();

    /** Current palette plane, or null before the first room load. */
    int[][] palettesOrNull();

    boolean heartPiecePickupActive();

    String instrumentTrace();

    String moblinKingTrace();

    /** Owned snapshot, or null before the first room load. */
    RoomRenderSnapshot renderSnapshotOrNull();

    /** Reused read-only render projection, valid until the next tick or room change. */
    /** Nullable until the first room load; otherwise a same-thread view valid until the next tick. */

    /** Borrowed until next tick/room change; null before the first room load. */
    RoomRenderSnapshot.View renderViewOrNull();

    LiftedEntityState liftedEntityState();
}
