package linksawakening.world.room;

import linksawakening.world.entity.event.LiftedEntityState;

final class RoomRoomsView implements RoomRooms {
    private final Dependencies dependencies;
    private final RoomSession session;

    RoomRoomsView(Dependencies dependencies, RoomSession session) {
        this.dependencies = dependencies;
        this.session = java.util.Objects.requireNonNull(session);
    }

    @Override
    public void loadInitialOverworld(int roomId) {
        session.loadInitialOverworld(roomId);
    }

    @Override
    public boolean musicSelectionIsScroll() {
        return dependencies.roomLoadCoordinator().musicSelectionIsScroll;
    }

    @Override
    public void loadOverworld(int roomId) {
        session.loadOverworld(roomId);
    }

    @Override
    public void loadIndoor(int mapId, int roomId) {
        session.loadIndoor(mapId, roomId);
    }

    @Override
    public void loadIndoorFromSavedPosition(int mapId, int roomId, int savedIndoorMapPosition) {
        session.loadIndoorFromSavedPosition(mapId, roomId, savedIndoorMapPosition);
    }

    @Override
    public void loadIndoor(int mapId, int roomId, int mapCategory) {
        session.loadIndoor(mapId, roomId, mapCategory);
    }

    @Override
    public ActiveRoom activeRoomOrNull() {
        return session.activeRoom();
    }

    @Override
    public ActiveRoom requireActiveRoom() {
        return session.requireActiveRoom();
    }

    @Override
    public int[][] palettesOrNull() {
        return session.palettes();
    }

    @Override
    public boolean hasActiveRoom() {
        return session.hasActiveRoom();
    }

    @Override
    public int currentRoomId() {
        return session.currentRoomId();
    }

    @Override
    public int mapCategory() {
        return session.mapCategory();
    }

    @Override
    public int mapId() {
        return session.mapId();
    }

    @Override
    public int indoorRoomPositionForSave() {
        return session.indoorRoomPositionForSave();
    }

    @Override
    public boolean heartPiecePickupActive() {
        return session.heartPiecePickupActive();
    }

    @Override
    public String instrumentTrace() {
        return session.instrumentTrace();
    }

    @Override
    public String moblinKingTrace() {
        return session.moblinKingTrace();
    }

    @Override
    public RoomRenderSnapshot.View renderViewOrNull() {
        return session.renderView();
    }

    @Override
    public RoomRenderSnapshot renderSnapshotOrNull() {
        return session.renderSnapshot();
    }

    @Override
    public LiftedEntityState liftedEntityState() {
        return session.liftedEntityState();
    }

    record Dependencies(RoomLoadCoordinator roomLoadCoordinator) {
        Dependencies {
            java.util.Objects.requireNonNull(roomLoadCoordinator, "roomLoadCoordinator");
        }
    }
}
