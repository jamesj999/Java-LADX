package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.world.transition.Warp;

public interface RoomProgressCompatibility {
    RoomSession facade();
    /** @deprecated Use {@code progress().onMapBoundaryWarp()}. */
    @Deprecated(forRemoval = true)
    default void onMapBoundaryWarp(Runnable callback) {
        facade().progress().onMapBoundaryWarp(callback);
    }
    /** @deprecated Use {@code progress().respawnPoint()}. */
    @Deprecated(forRemoval = true)
    default Warp respawnPoint() {
        return facade().progress().respawnPoint();
    }

    /** @deprecated Use {@code progress().respawnIndoorRoom()}. */
    @Deprecated(forRemoval = true)
    default int respawnIndoorRoom() {
        return facade().progress().respawnIndoorRoom();
    }

    /** @deprecated Use {@code progress().setRespawnPoint()}. */
    @Deprecated(forRemoval = true)
    default void setRespawnPoint(Warp point, int indoorRoom) {
        facade().progress().setRespawnPoint(point, indoorRoom);
    }
    /** @deprecated Use {@code progress().clearGameOverPowerCounter()}. */
    @Deprecated(forRemoval = true)
    default void clearGameOverPowerCounter() {
        facade().progress().clearGameOverPowerCounter();
    }

    /** @deprecated Use {@code progress().resetGameOverSwitchBlocks()}. */
    @Deprecated(forRemoval = true)
    default void resetGameOverSwitchBlocks() {
        facade().progress().resetGameOverSwitchBlocks();
    }

    /** @deprecated Use {@code dungeon().setDungeonItemForDebug()}. */
    @Deprecated(forRemoval = true)
    default void setDungeonItemForDebug(int mapId, int index, int value) {
        facade().dungeon().setDungeonItemForDebug(mapId, index, value);
    }

    /** @deprecated Use {@code entityState().entityDefaultMusicTrack()}. */
    @Deprecated(forRemoval = true)
    default int entityDefaultMusicTrack() {
        return facade().entityState().entityDefaultMusicTrack();
    }
}
