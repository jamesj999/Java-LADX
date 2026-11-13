package linksawakening.world.room;

import linksawakening.world.entity.event.LiftedEntityState;

final class RoomRoomsView implements RoomRooms {
    private final Dependencies dependencies;
    private final RoomSession session;

    RoomRoomsView(Dependencies dependencies, RoomSession session) {
        this.dependencies = dependencies;
        this.session = java.util.Objects.requireNonNull(session);
    }

    public void loadInitialOverworld(int roomId) {
        dependencies.roomLoadCoordinator().loadInitialOverworld(roomId);
    }

    public boolean musicSelectionIsScroll() {
        return dependencies.roomLoadCoordinator().musicSelectionIsScroll;
    }

    public void loadOverworld(int roomId) {
        dependencies.roomLoadCoordinator().loadOverworld(roomId);
    }

    public void loadIndoor(int mapId, int roomId) {
        dependencies.roomLoadCoordinator().loadIndoor(mapId, roomId);
    }

    public void loadIndoorFromSavedPosition(int mapId, int roomId, int savedIndoorMapPosition) {
        dependencies.roomLoadCoordinator().loadIndoorFromSavedPosition(mapId, roomId, savedIndoorMapPosition);
    }

    public void loadIndoor(int mapId, int roomId, int mapCategory) {
        dependencies.roomLoadCoordinator().loadIndoor(mapId, roomId, mapCategory);
    }

    public ActiveRoom activeRoom() {
        return session.activeRoom();
    }

    public boolean hasActiveRoom() {
        return session.hasActiveRoom();
    }

    public int currentRoomId() {
        return session.currentRoomId();
    }

    public int mapCategory() {
        return session.mapCategory();
    }

    public int mapId() {
        return session.mapId();
    }

    public int indoorRoomPositionForSave() {
        return session.indoorRoomPositionForSave();
    }

    public int[][] palettes() {
        return session.palettes();
    }

    public boolean heartPiecePickupActive() {
        return session.heartPiecePickupActive();
    }

    public String instrumentTrace() {
        return session.instrumentTrace();
    }

    public String moblinKingTrace() {
        return session.moblinKingTrace();
    }

    public RoomRenderSnapshot.View renderViewOrNull() {
        return session.renderView();
    }

    public RoomRenderSnapshot renderSnapshotOrNull() {
        return session.renderSnapshot();
    }

    public RoomRenderSnapshot.View renderView() {
        return session.renderView();
    }

    public RoomRenderSnapshot renderSnapshot() {
        return session.renderSnapshot();
    }

    public LiftedEntityState liftedEntityState() {
        return session.liftedEntityState();
    }

    record Dependencies(RoomLoadCoordinator roomLoadCoordinator, RoomSessionViews roomSessionViews) {
        Dependencies {
            java.util.Objects.requireNonNull(roomLoadCoordinator, "roomLoadCoordinator");
            java.util.Objects.requireNonNull(roomSessionViews, "roomSessionViews");
        }
    }
}
