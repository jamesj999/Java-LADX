package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.transition.Warp;

interface RoomProgressCompatibility {
    RoomSession facade();
    /** Called only when a warp crosses the indoor/overworld boundary. */
    default void onMapBoundaryWarp(Runnable callback) {
        facade().context.mapBoundaryWarp = java.util.Objects.requireNonNull(callback);
    }
    /** Source wSpawnLocationData, retained across scrolling but updated by non-side-view warps. */
    default Warp respawnPoint() {
        return facade().context.respawnPoint;
    }

    default int respawnIndoorRoom() {
        return facade().context.respawnIndoorRoom;
    }

    default void setRespawnPoint(Warp point, int indoorRoom) {
        facade().context.respawnPoint = point;
        facade().context.respawnIndoorRoom = indoorRoom;
    }
    /** $01:4244 clears only the Piece of Power counter, preserving the acorn counter. */
    default void clearGameOverPowerCounter() {
        facade().context.enemyDropCounters = new EnemyDropResolver.CounterState(
                facade().context.enemyDropCounters.guardianAcornCounter(), 0);
        facade().context.roomLoadCoordinator.configureEnemyDropRuntime();
    }

    default void resetGameOverSwitchBlocks() {
        facade().context.recentRoomEntityClears.reset();
        facade().context.switchBlocksState = 0;
        facade().context.switchableObjectAnimationStage = 0;
    }

    default void setDungeonItemForDebug(int mapId, int index, int value) {
        facade().context.roomState.dungeonItemState.setFlagForDebug(mapId, index, value);
    }

    default int entityDefaultMusicTrack() {
        return facade().context.entityDefaultMusicTrack;
    }
}
