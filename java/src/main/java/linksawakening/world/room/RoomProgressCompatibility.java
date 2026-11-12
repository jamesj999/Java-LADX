package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.transition.Warp;

interface RoomProgressCompatibility {
    RoomSession facade();
    /** Called only when a warp crosses the indoor/overworld boundary. */
    default void onMapBoundaryWarp(Runnable callback) {
        facade().components.location().mapBoundaryWarp = java.util.Objects.requireNonNull(callback);
    }
    /** Source wSpawnLocationData, retained across scrolling but updated by non-side-view warps. */
    default Warp respawnPoint() {
        return facade().components.location().respawnPoint;
    }

    default int respawnIndoorRoom() {
        return facade().components.location().respawnIndoorRoom;
    }

    default void setRespawnPoint(Warp point, int indoorRoom) {
        facade().components.location().respawnPoint = point;
        facade().components.location().respawnIndoorRoom = indoorRoom;
    }
    /** $01:4244 clears only the Piece of Power counter, preserving the acorn counter. */
    default void clearGameOverPowerCounter() {
        facade().components.entityFrame().enemyDropCounters = new EnemyDropResolver.CounterState(
                facade().components.entityFrame().enemyDropCounters.guardianAcornCounter(), 0);
        facade().components.roomLoadCoordinator().configureEnemyDropRuntime();
    }

    default void resetGameOverSwitchBlocks() {
        facade().components.entityFrame().recentRoomEntityClears.reset();
        facade().components.terrain().switchBlocksState = 0;
        facade().components.terrain().switchableObjectAnimationStage = 0;
    }

    default void setDungeonItemForDebug(int mapId, int index, int value) {
        facade().components.roomState().dungeonItemState.setFlagForDebug(mapId, index, value);
    }

    default int entityDefaultMusicTrack() {
        return facade().components.followers().entityDefaultMusicTrack;
    }
}
