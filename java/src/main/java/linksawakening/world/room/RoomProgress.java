package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.world.transition.Warp;

public interface RoomProgress {
    void onMapBoundaryWarp(Runnable callback);

    Warp respawnPoint();

    int respawnIndoorRoom();

    void setRespawnPoint(Warp point, int indoorRoom);

    void clearGameOverPowerCounter();

    void resetGameOverSwitchBlocks();
}
