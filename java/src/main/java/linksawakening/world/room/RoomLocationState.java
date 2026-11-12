package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import linksawakening.world.transition.Warp;

final class RoomLocationState {
    final long roomSessionId = ++nextRoomSessionId;
    ActiveRoom activeRoom;
    /** Increments on every ROM room load, including a reload of the same room. */
    long roomLoadSerial;
    /** ROM wIndoorRoom: the current position in the active map's 8x8 layout. */
    int indoorMapPosition = -1;

    Warp pendingDungeonWarp;
    linksawakening.world.transition.Warp respawnPoint;
    int respawnIndoorRoom;
    Runnable mapBoundaryWarp = () -> {};
    int currentOverworldTilesetId = W_TILESET_NO_UPDATE;
    boolean shouldGetLostInMysteriousWoods;

    void setRespawnPoint(Warp point, int indoorRoom) {
        respawnPoint = point;
        respawnIndoorRoom = indoorRoom;
    }
}
