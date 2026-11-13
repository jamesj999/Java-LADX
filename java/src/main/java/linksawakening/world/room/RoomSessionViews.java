package linksawakening.world.room;

import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.world.transition.Warp;

/** Read-only room queries; borrowed projections follow room replacement and owned snapshots preserve history. */
final class RoomSessionViews {
    private final Dependencies dependencies;

    RoomSessionViews(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    long roomLoadSerial() {
        return dependencies.location().roomLoadSerial;
    }

    long roomLoadToken() {
        return (dependencies.location().roomSessionId << 32) ^ dependencies.location().roomLoadSerial;
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

    int activeRoomStatusFlags(ActiveRoom room) {
        return dependencies.roomState().activeRoomStatusFlags(room);
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

    ActiveRoom activeRoom() {
        return dependencies.location().activeRoom;
    }

    boolean hasActiveRoom() {
        return dependencies.location().activeRoom != null;
    }

    int indoorRoomPositionForSave() {
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            return -1;
        }
        return dependencies.location().indoorMapPosition;
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
}
