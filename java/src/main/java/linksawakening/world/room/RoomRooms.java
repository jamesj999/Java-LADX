package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

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

    RoomRenderSnapshot renderSnapshot();

    LiftedEntityState liftedEntityState();
}
