package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.W_TILESET_NO_UPDATE;

import linksawakening.world.transition.Warp;

final class RoomLocationState {
    void beginEntityTick() {
        shouldGetLostInMysteriousWoods = false;
    }

    private static long nextRoomSessionId;
    final long roomSessionId = ++nextRoomSessionId;
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

    void onMapBoundaryWarp(Runnable callback) {
        mapBoundaryWarp = java.util.Objects.requireNonNull(callback);
    }

    void setRespawnPoint(Warp point, int indoorRoom) {
        respawnPoint = point;
        respawnIndoorRoom = indoorRoom;
    }
}
