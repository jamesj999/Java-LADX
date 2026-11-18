package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.W_TILESET_NO_UPDATE;

import java.util.Objects;
import linksawakening.world.transition.Warp;

final class RoomLocationState {
    void beginEntityTick() {
        shouldGetLostInMysteriousWoods = false;
    }

    private static long nextRoomSessionId;
    private final long roomSessionId = ++nextRoomSessionId;
    /** Increments on every ROM room load, including a reload of the same room. */
    private long roomLoadSerial;
    /** ROM wIndoorRoom: the current position in the active map's 8x8 layout. */
    private int indoorMapPosition = -1;

    /** Absent when no entity has requested a dungeon warp. */
    private Warp pendingDungeonWarp;
    /** Absent until a dungeon entrance establishes a respawn destination. */
    private linksawakening.world.transition.Warp respawnPoint;

    private int respawnIndoorRoom;
    private int worldMapRoom = 0xA3;
    private Runnable mapBoundaryWarp = () -> {};
    private int currentOverworldTilesetId = W_TILESET_NO_UPDATE;
    private boolean shouldGetLostInMysteriousWoods;

    void onMapBoundaryWarp(Runnable callback) {
        mapBoundaryWarp = Objects.requireNonNull(callback);
    }

    void setRespawnPoint(Warp point, int indoorRoom) {
        respawnPoint = point;
        respawnIndoorRoom = indoorRoom;
    }

    Warp pendingDungeonWarp() {
        return this.pendingDungeonWarp;
    }

    Warp pendingDungeonWarp(Warp value) {
        this.pendingDungeonWarp = value;
        return this.pendingDungeonWarp;
    }

    int indoorMapPosition() {
        return this.indoorMapPosition;
    }

    int indoorMapPosition(int value) {
        this.indoorMapPosition = value;
        return this.indoorMapPosition;
    }

    int worldMapRoom() {
        return worldMapRoom;
    }

    void setWorldMapRoom(int room) {
        worldMapRoom = room;
    }

    int respawnIndoorRoom() {
        return this.respawnIndoorRoom;
    }

    Runnable mapBoundaryWarp() {
        return this.mapBoundaryWarp;
    }

    int currentOverworldTilesetId() {
        return this.currentOverworldTilesetId;
    }

    int currentOverworldTilesetId(int value) {
        this.currentOverworldTilesetId = value;
        return this.currentOverworldTilesetId;
    }

    long roomLoadSerial() {
        return this.roomLoadSerial;
    }

    long postIncrementRoomLoadSerial() {
        return this.roomLoadSerial++;
    }

    boolean shouldGetLostInMysteriousWoods() {
        return this.shouldGetLostInMysteriousWoods;
    }

    boolean shouldGetLostInMysteriousWoods(boolean value) {
        this.shouldGetLostInMysteriousWoods = value;
        return this.shouldGetLostInMysteriousWoods;
    }

    linksawakening.world.transition.Warp respawnPoint() {
        return this.respawnPoint;
    }

    long roomSessionId() {
        return this.roomSessionId;
    }
}
