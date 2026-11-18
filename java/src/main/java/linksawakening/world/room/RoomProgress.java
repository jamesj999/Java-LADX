package linksawakening.world.room;

import linksawakening.world.transition.Warp;

public interface RoomProgress {
    void onMapBoundaryWarp(Runnable callback);

    int worldMapRoom();

    void restoreWorldMapRoom(int room);

    Warp respawnPoint();

    int respawnIndoorRoom();

    void setRespawnPoint(Warp point, int indoorRoom);

    void clearGameOverPowerCounter();

    void resetGameOverSwitchBlocks();
}
