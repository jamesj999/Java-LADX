package linksawakening.world.room;

import linksawakening.world.transition.Warp;

final class RoomProgression implements RoomProgress {
    private final Dependencies dependencies;
    private final LoadedRoomHandle rooms;

    RoomProgression(Dependencies dependencies, LoadedRoomHandle rooms) {
        this.dependencies = dependencies;
        this.rooms = java.util.Objects.requireNonNull(rooms);
    }

    @Override
    public void onMapBoundaryWarp(Runnable callback) {
        dependencies.location().onMapBoundaryWarp(callback);
    }

    @Override
    public Warp respawnPoint() {
        return dependencies.location().respawnPoint;
    }

    @Override
    public int respawnIndoorRoom() {
        return dependencies.location().respawnIndoorRoom;
    }

    @Override
    public void setRespawnPoint(Warp point, int indoorRoom) {
        dependencies.location().setRespawnPoint(point, indoorRoom);
    }

    @Override
    public void clearGameOverPowerCounter() {
        dependencies.entityFrame().clearGameOverPowerCounter();
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded != null) dependencies.loads().configureEnemyDropRuntime(loaded);
    }

    @Override
    public void resetGameOverSwitchBlocks() {
        dependencies.entityFrame().clearRecentRoomEntityClears();
        dependencies.terrain().resetGameOverSwitchBlocks();
    }

    record Dependencies(
            RoomEntitySessionState entityFrame,
            RoomLocationState location,
            RoomTerrainState terrain,
            RoomLoadCoordinator loads) {
        Dependencies {
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(terrain, "terrain");
            java.util.Objects.requireNonNull(loads, "loads");
        }
    }
}
