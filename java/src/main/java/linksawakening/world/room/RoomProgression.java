package linksawakening.world.room;

import java.util.Objects;
import linksawakening.world.transition.Warp;

final class RoomProgression implements RoomProgress {
    private final Dependencies dependencies;
    private final LoadedRoomHandle rooms;

    RoomProgression(Dependencies dependencies, LoadedRoomHandle rooms) {
        this.dependencies = Objects.requireNonNull(dependencies, "dependencies");
        this.rooms = Objects.requireNonNull(rooms);
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
        rooms.update(dependencies, (room, state) -> state.loads().configureEnemyDropRuntime(room));
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
            Objects.requireNonNull(entityFrame, "entityFrame");
            Objects.requireNonNull(location, "location");
            Objects.requireNonNull(terrain, "terrain");
            Objects.requireNonNull(loads, "loads");
        }
    }
}
