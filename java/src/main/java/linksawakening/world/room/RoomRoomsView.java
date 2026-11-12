package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.world.entity.event.LiftedEntityState;

final class RoomRoomsView implements RoomRooms {
    private final Dependencies dependencies;

    RoomRoomsView(Dependencies dependencies) {
        this.dependencies = dependencies;
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
        return dependencies.roomSessionViews().activeRoom();
    }

    public boolean hasActiveRoom() {
        return dependencies.roomSessionViews().hasActiveRoom();
    }

    public int currentRoomId() {
        return dependencies.roomSessionViews().currentRoomId();
    }

    public int mapCategory() {
        return dependencies.roomSessionViews().mapCategory();
    }

    public int mapId() {
        return dependencies.roomSessionViews().mapId();
    }

    public int indoorRoomPositionForSave() {
        return dependencies.roomSessionViews().indoorRoomPositionForSave();
    }

    public int[][] palettes() {
        return dependencies.roomSessionViews().palettes();
    }

    public boolean heartPiecePickupActive() {
        return dependencies.roomSessionViews().heartPiecePickupActive();
    }

    public String instrumentTrace() {
        return dependencies.roomSessionViews().instrumentTrace();
    }

    public String moblinKingTrace() {
        return dependencies.roomSessionViews().moblinKingTrace();
    }

    public RoomRenderSnapshot renderSnapshot() {
        return dependencies.roomSessionViews().renderSnapshot();
    }

    public LiftedEntityState liftedEntityState() {
        return dependencies.roomSessionViews().liftedEntityState();
    }

    record Dependencies(RoomLoadCoordinator roomLoadCoordinator, RoomSessionViews roomSessionViews) {}
}
