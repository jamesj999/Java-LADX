package linksawakening.world.room;

import java.util.List;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.world.entity.event.HeartPiecePickupUpdate;
import linksawakening.world.entity.event.LiftedEntityState;
import linksawakening.world.transition.Warp;

/** Read-only room queries; borrowed projections follow room replacement and owned snapshots preserve history. */
final class RoomSessionViews {
    private final Dependencies dependencies;
    private final RoomRenderSnapshot.View renderView;

    RoomSessionViews(Dependencies dependencies) {
        this.dependencies = dependencies;
        renderView = new RoomRenderLiveView(
                () -> dependencies.location().activeRoom, () -> dependencies.entityRuntimeState().runtime);
    }

    int entityHealth(int slot) {
        return dependencies.entityRuntimeState().runtime.enemies().enemyHealth(slot);
    }

    ActiveRoom activeRoom() {
        return dependencies.location().activeRoom;
    }

    long roomLoadSerial() {
        return dependencies.location().roomLoadSerial;
    }

    long roomLoadToken() {
        return (dependencies.location().roomSessionId << 32) ^ dependencies.location().roomLoadSerial;
    }

    boolean hasActiveRoom() {
        return dependencies.location().activeRoom != null;
    }

    int currentRoomId() {
        return dependencies.location().activeRoom.roomId();
    }

    int mapCategory() {
        return dependencies.location().activeRoom.mapCategory();
    }

    int mapId() {
        return dependencies.location().activeRoom.mapId();
    }

    int indoorRoomPositionForSave() {
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            return -1;
        }
        return dependencies.location().indoorMapPosition;
    }

    byte[] dungeonMinimapRoomStatusSnapshot() {
        return dependencies.roomState().dungeonMinimapRoomStatusSnapshot(dependencies.location().activeRoom);
    }

    byte[] overworldRoomStatusSnapshot() {
        return dependencies.roomState().overworldRoomStatusSnapshot();
    }

    byte[] indoorARoomStatusSnapshot() {
        return dependencies.roomState().indoorARoomStatusSnapshot();
    }

    byte[] indoorBRoomStatusSnapshot() {
        return dependencies.roomState().indoorBRoomStatusSnapshot();
    }

    byte[] colorDungeonRoomStatusSnapshot() {
        return dependencies.roomState().colorDungeonRoomStatusSnapshot();
    }

    byte[] dungeonItemFlagsSnapshot() {
        return dependencies.roomState().dungeonItemFlagsSnapshot();
    }

    byte[] colorDungeonItemFlagsSnapshot() {
        return dependencies.roomState().colorDungeonItemFlagsSnapshot();
    }

    byte[] currentDungeonItemFlagsSnapshot() {
        return dependencies.roomState().currentDungeonItemFlagsSnapshot();
    }

    void restoreDungeonItemFlags(byte[] dungeonFlags, byte[] colorFlags) {
        dependencies.roomState().restoreDungeonItemFlags(dungeonFlags, colorFlags);
    }

    byte[] dungeonProgressFlagsSnapshot() {
        return dependencies.roomState().dungeonProgressFlagsSnapshot();
    }

    void restoreDungeonProgressFlags(byte[] flags) {
        dependencies.roomState().restoreDungeonProgressFlags(flags);
    }

    int[][] palettes() {
        return dependencies.location().activeRoom == null
                ? null
                : dependencies.location().activeRoom.palettes();
    }

    boolean heartPiecePickupActive() {
        return dependencies.entityRuntimeState().runtime != null
                && dependencies.entityRuntimeState().runtime.inspection().heartPiecePickupActive();
    }

    void setHeartPiecesCount(int count) {
        if (dependencies.entityRuntimeState().runtime != null)
            dependencies.entityRuntimeState().runtime.setHeartPiecesCount(count);
    }

    List<HeartPiecePickupUpdate> consumeHeartPieceUpdates() {
        if (dependencies.entityRuntimeState().runtime == null) return List.of();
        var updates = dependencies.entityRuntimeState().runtime.consumeHeartPieceUpdates();
        for (int i = 0; i < updates.size(); i++) {
            if (updates.get(i).complete()) {
                dependencies.roomState().markCompleted(dependencies.location().activeRoom);
                break;
            }
        }
        return updates;
    }

    String instrumentTrace() {
        return dependencies.entityRuntimeState().runtime == null
                ? ""
                : dependencies.entityRuntimeState().runtime.inspection().instrumentTrace();
    }

    String moblinKingTrace() {
        return dependencies.entityRuntimeState().runtime == null
                ? "-1"
                : dependencies.entityRuntimeState().runtime.inspection().moblinKingTrace();
    }

    RoomRenderSnapshot.View renderView() {
        return dependencies.location().activeRoom == null ? null : renderView;
    }

    RoomRenderSnapshot renderSnapshot() {
        if (dependencies.location().activeRoom == null) return null;
        RoomRenderSnapshot snapshot = dependencies.location().activeRoom.renderSnapshot();
        return new RoomRenderSnapshot(
                snapshot.tileIds(),
                snapshot.tileAttrs(),
                snapshot.palettes(),
                snapshot.entities(),
                dependencies.entityRuntimeState().runtime == null
                        ? null
                        : dependencies.entityRuntimeState().runtime.inspection().instrumentWarpVisual(),
                dependencies.entityRuntimeState().runtime == null
                        ? null
                        : dependencies.entityRuntimeState().runtime.inspection().heartPieceVisual(),
                snapshot.linkGrassPalette());
    }

    LiftedEntityState liftedEntityState() {
        return dependencies.entityRuntimeState().runtime == null
                ? LiftedEntityState.none()
                : dependencies.entityRuntimeState().runtime.inspection().liftedEntityState();
    }

    void setColorShellSoundSink(GameplaySoundSink soundSink) {
        dependencies.entityFrame().colorShellSoundSink = soundSink == null ? GameplaySoundSink.none() : soundSink;
    }

    int objectAtRoomLocation(int location) {
        return objectAtRoomLocation(dependencies.location().activeRoom, location);
    }

    int objectAtRoomLocation(ActiveRoom room, int location) {
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        int[] objects = room.roomObjectsArea();
        return areaIndex < 0 || areaIndex >= objects.length ? 0xFF : objects[areaIndex] & 0xFF;
    }

    int adjacentIndoorRoomIdForBombWall(int wallIndex) {
        return adjacentIndoorRoomIdForBombWall(dependencies.location().activeRoom, wallIndex);
    }

    int adjacentIndoorRoomIdForBombWall(ActiveRoom room, int wallIndex) {
        return dependencies
                .scrollNavigation()
                .adjacentIndoorRoomIdForBombWall(room, dependencies.location().indoorMapPosition, wallIndex);
    }

    int findIndoorMapPosition(int mapId, int roomId) {
        return dependencies.roomState().findIndoorMapPosition(mapId, roomId);
    }

    int indoorMapLayoutOffset(int mapId) {
        return dependencies.roomState().indoorMapLayoutOffset(mapId);
    }

    byte[] indoorStatusTableForMap(int mapId) {
        return dependencies.roomState().indoorStatusTableForMap(mapId);
    }

    int activeRoomStatusFlags() {
        return dependencies.roomState().activeRoomStatusFlags(dependencies.location().activeRoom);
    }

    record Dependencies(
            RoomEntitySessionState entityFrame,
            RoomLocationState location,
            RoomEntitySessionState entityRuntimeState,
            RoomWorldState roomState,
            RoomScrollNavigation scrollNavigation) {
        Dependencies {
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
            java.util.Objects.requireNonNull(roomState, "roomState");
            java.util.Objects.requireNonNull(scrollNavigation, "scrollNavigation");
        }
    }
}
