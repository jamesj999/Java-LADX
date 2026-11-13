package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.W_TILESET_NO_UPDATE;

import linksawakening.world.transition.Warp;

final class RoomLocationState {
    final long roomSessionId = ++RoomSession.nextRoomSessionId;
    /** Absent before the first room load; public room projections then return null. */
    ActiveRoom activeRoom;
    /** Increments on every ROM room load, including a reload of the same room. */
    long roomLoadSerial;
    /** ROM wIndoorRoom: the current position in the active map's 8x8 layout. */
    int indoorMapPosition = -1;

    /** Absent when no entity has requested a dungeon warp. */
    Warp pendingDungeonWarp;
    /** Absent until a dungeon entrance establishes a respawn destination. */
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
