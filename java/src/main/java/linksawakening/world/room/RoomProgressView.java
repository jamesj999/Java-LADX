package linksawakening.world.room;

import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.transition.Warp;

final class RoomProgressView implements RoomProgress {
    private final Dependencies dependencies;
    private final RoomSession session;

    RoomProgressView(Dependencies dependencies, RoomSession session) {
        this.dependencies = dependencies;
        this.session = session;
    }

    public void onMapBoundaryWarp(Runnable callback) {
        dependencies.location().mapBoundaryWarp = java.util.Objects.requireNonNull(callback);
    }

    public Warp respawnPoint() {
        return dependencies.location().respawnPoint;
    }

    public int respawnIndoorRoom() {
        return dependencies.location().respawnIndoorRoom;
    }

    public void setRespawnPoint(Warp point, int indoorRoom) {
        dependencies.location().respawnPoint = point;
        dependencies.location().respawnIndoorRoom = indoorRoom;
    }

    public void clearGameOverPowerCounter() {
        dependencies.entityFrame().enemyDropCounters = new EnemyDropResolver.CounterState(
                dependencies.entityFrame().enemyDropCounters.guardianAcornCounter(), 0);
        session.configureEnemyDropRuntime();
    }

    public void resetGameOverSwitchBlocks() {
        dependencies.entityFrame().recentRoomEntityClears.reset();
        dependencies.terrain().switchBlocksState = 0;
        dependencies.terrain().switchableObjectAnimationStage = 0;
    }

    record Dependencies(RoomEntitySessionState entityFrame, RoomLocationState location, RoomTerrainState terrain) {
        Dependencies {
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(terrain, "terrain");
        }
    }
}
