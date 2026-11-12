package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import java.util.List;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.world.entity.event.HeartPiecePickupUpdate;
import linksawakening.world.entity.event.LiftedEntityState;
import linksawakening.world.transition.Warp;

/** Live room queries and owned save/render projections of hMapRoom and the room status tables. */
final class RoomSessionViews {
    private final Dependencies dependencies;

    RoomSessionViews(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    int entityHealth(int slot) {
        return dependencies.entityBridge().get().entityRuntime.enemies().enemyHealth(slot);
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
        return dependencies.entityBridge().get().entityRuntime != null
                && dependencies.entityBridge().get().entityRuntime.inspection().heartPiecePickupActive();
    }

    void setHeartPiecesCount(int count) {
        if (dependencies.entityBridge().get().entityRuntime != null)
            dependencies.entityBridge().get().entityRuntime.setHeartPiecesCount(count);
    }

    List<HeartPiecePickupUpdate> consumeHeartPieceUpdates() {
        if (dependencies.entityBridge().get().entityRuntime == null) return List.of();
        var updates = dependencies.entityBridge().get().entityRuntime.consumeHeartPieceUpdates();
        for (int i = 0; i < updates.size(); i++) {
            if (updates.get(i).complete()) {
                dependencies.roomStoryState().get().markActiveRoomCompleted();
                break;
            }
        }
        return updates;
    }

    String instrumentTrace() {
        return dependencies.entityBridge().get().entityRuntime == null
                ? ""
                : dependencies.entityBridge().get().entityRuntime.inspection().instrumentTrace();
    }

    String moblinKingTrace() {
        return dependencies.entityBridge().get().entityRuntime == null
                ? "-1"
                : dependencies.entityBridge().get().entityRuntime.inspection().moblinKingTrace();
    }

    RoomRenderSnapshot renderSnapshot() {
        if (dependencies.location().activeRoom == null) return null;
        RoomRenderSnapshot snapshot = dependencies.location().activeRoom.renderSnapshot();
        return new RoomRenderSnapshot(
                snapshot.tileIds(),
                snapshot.tileAttrs(),
                snapshot.palettes(),
                snapshot.entities(),
                dependencies.entityBridge().get().entityRuntime == null
                        ? null
                        : dependencies
                                .entityBridge()
                                .get()
                                .entityRuntime
                                .inspection()
                                .instrumentWarpVisual(),
                dependencies.entityBridge().get().entityRuntime == null
                        ? null
                        : dependencies
                                .entityBridge()
                                .get()
                                .entityRuntime
                                .inspection()
                                .heartPieceVisual(),
                snapshot.linkGrassPalette());
    }

    LiftedEntityState liftedEntityState() {
        return dependencies.entityBridge().get().entityRuntime == null
                ? LiftedEntityState.none()
                : dependencies.entityBridge().get().entityRuntime.inspection().liftedEntityState();
    }

    void setColorShellSoundSink(GameplaySoundSink soundSink) {
        dependencies.entityFrame().colorShellSoundSink = soundSink == null ? GameplaySoundSink.none() : soundSink;
    }

    int objectAtRoomLocation(int location) {
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        int[] objects = dependencies.location().activeRoom.roomObjectsArea();
        return areaIndex < 0 || areaIndex >= objects.length ? 0xFF : objects[areaIndex] & 0xFF;
    }

    int adjacentIndoorRoomIdForBombWall(int wallIndex) {
        return dependencies
                .scrollNavigation()
                .adjacentIndoorRoomIdForBombWall(
                        dependencies.location().activeRoom, dependencies.location().indoorMapPosition, wallIndex);
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
            RoomEntityFrameState entityFrame,
            RoomLocationState location,
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomStoryState> roomStoryState,
            RoomScrollNavigation scrollNavigation) {}
}
