package linksawakening.world.room;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EnemyDropResolver;

import linksawakening.world.entity.event.LiftedEntityState;

import linksawakening.world.entity.event.HeartPiecePickupUpdate;

import linksawakening.world.entity.EntityTypeRules;

import linksawakening.world.transition.Warp;

import linksawakening.gameplay.GameplaySoundSink;
import java.util.List;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.room.RoomSession.*;

/** RoomSessionViews operations for the live room session. */
final class RoomSessionViews {
    private final RoomSession session;
    RoomSessionViews(RoomSession session) { this.session = session; }

    int entityHealth(int slot) { return session.context.entityBridge.entityRuntime.enemies().enemyHealth(slot); }

    ActiveRoom activeRoom() {
        return session.context.activeRoom;
    }

    long roomLoadSerial() {
        return session.context.roomLoadSerial;
    }

    long roomLoadToken() {
        return (session.context.roomSessionId << 32) ^ session.context.roomLoadSerial;
    }

    boolean hasActiveRoom() {
        return session.context.activeRoom != null;
    }

    int currentRoomId() {
        return session.context.activeRoom.roomId();
    }

    int mapCategory() {
        return session.context.activeRoom.mapCategory();
    }

    int mapId() {
        return session.context.activeRoom.mapId();
    }

    int indoorRoomPositionForSave() {
        if (session.context.activeRoom == null || session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            return -1;
        }
        return session.context.indoorMapPosition;
    }

    byte[] dungeonMinimapRoomStatusSnapshot() { return session.context.roomState.dungeonMinimapRoomStatusSnapshot(session.context.activeRoom); }

    byte[] overworldRoomStatusSnapshot() { return session.context.roomState.overworldRoomStatusSnapshot(); }

    byte[] indoorARoomStatusSnapshot() { return session.context.roomState.indoorARoomStatusSnapshot(); }

    byte[] indoorBRoomStatusSnapshot() { return session.context.roomState.indoorBRoomStatusSnapshot(); }

    byte[] colorDungeonRoomStatusSnapshot() { return session.context.roomState.colorDungeonRoomStatusSnapshot(); }

    byte[] dungeonItemFlagsSnapshot() { return session.context.roomState.dungeonItemFlagsSnapshot(); }

    byte[] colorDungeonItemFlagsSnapshot() { return session.context.roomState.colorDungeonItemFlagsSnapshot(); }

    byte[] currentDungeonItemFlagsSnapshot() { return session.context.roomState.currentDungeonItemFlagsSnapshot(); }

    void restoreDungeonItemFlags(byte[] dungeonFlags, byte[] colorFlags) { session.context.roomState.restoreDungeonItemFlags(dungeonFlags, colorFlags); }

    byte[] dungeonProgressFlagsSnapshot() { return session.context.roomState.dungeonProgressFlagsSnapshot(); }

    void restoreDungeonProgressFlags(byte[] flags) { session.context.roomState.restoreDungeonProgressFlags(flags); }

    int overworldRoomStatusForTest(int roomId) { return session.context.roomState.overworldRoomStatusForTest(roomId); }

    int indoorRoomStatusForTest(int mapId, int roomId) { return session.context.roomState.indoorRoomStatusForTest(mapId, roomId); }

    void setEnemyDropCountersForTest(int droppedItemCount, int droppedBombArrowCount) {
        session.context.enemyDropCounters = new EnemyDropResolver.CounterState(
            droppedItemCount, droppedBombArrowCount);
    }

    EnemyDropResolver.CounterState enemyDropCountersForTest() {
        return session.context.enemyDropCounters;
    }

    void setSwitchButtonPressedForTest(int value) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException("Switch-button state must be an unsigned byte");
        }
        session.context.switchButtonPressed = value;
    }

    int switchButtonPressedForTest() {
        return session.context.switchButtonPressed;
    }

    int[][] palettes() {
        return session.context.activeRoom == null ? null : session.context.activeRoom.palettes();
    }

    boolean heartPiecePickupActive() {
        return session.context.entityBridge.entityRuntime != null && session.context.entityBridge.entityRuntime.inspection().heartPiecePickupActive();
    }

    void setHeartPiecesCount(int count) {
        if (session.context.entityBridge.entityRuntime != null) session.context.entityBridge.entityRuntime.setHeartPiecesCount(count);
    }

    List<HeartPiecePickupUpdate> consumeHeartPieceUpdates() {
        if (session.context.entityBridge.entityRuntime == null) return List.of();
        var updates = session.context.entityBridge.entityRuntime.consumeHeartPieceUpdates();
        if (updates.stream().anyMatch(HeartPiecePickupUpdate::complete)) session.markActiveRoomCompleted();
        return updates;
    }

    String instrumentTrace() { return session.context.entityBridge.entityRuntime == null ? "" : session.context.entityBridge.entityRuntime.inspection().instrumentTrace(); }

    String moblinKingTrace() { return session.context.entityBridge.entityRuntime == null ? "-1" : session.context.entityBridge.entityRuntime.inspection().moblinKingTrace(); }

    RoomRenderSnapshot renderSnapshot() {
        if (session.context.activeRoom == null) return null;
        RoomRenderSnapshot snapshot = session.context.activeRoom.renderSnapshot();
        return new RoomRenderSnapshot(snapshot.tileIds(), snapshot.tileAttrs(), snapshot.palettes(),
            snapshot.entities(), session.context.entityBridge.entityRuntime == null ? null : session.context.entityBridge.entityRuntime.inspection().instrumentWarpVisual(),
            session.context.entityBridge.entityRuntime == null ? null : session.context.entityBridge.entityRuntime.inspection().heartPieceVisual(), snapshot.linkGrassPalette());
    }

    LiftedEntityState liftedEntityState() {
        return session.context.entityBridge.entityRuntime == null
            ? LiftedEntityState.none()
            : session.context.entityBridge.entityRuntime.inspection().liftedEntityState();
    }

    int bombDirectionForTest(int slot) {
        return session.context.entityBridge.entityRuntime == null ? 0xFF : session.context.entityBridge.entityRuntime.inspection().bombDirection(slot);
    }

    void setColorShellSoundSink(GameplaySoundSink soundSink) {
        session.context.colorShellSoundSink = soundSink == null ? GameplaySoundSink.none() : soundSink;
    }

    void setColorShellStateForTest(int slot, int state, int transitionCountdown, int direction, int speedX, int speedY) {
        if (session.context.entityBridge.entityRuntime != null) {
            session.context.entityBridge.entityRuntime.debug().setColorShellStateForTest(slot, state, transitionCountdown,
                direction, speedX, speedY);
        }
    }

    int entityGroundStatusForTest(int slot) {
        if (session.context.entityBridge.entityRuntime == null) {
            return 0;
        }
        return session.context.entityBridge.entityRuntime.inspection().groundStatus(slot);
    }

    void setEntityIgnoreHitsCountdownForTest(int slot, int value) {
        if (session.context.entityBridge.entityRuntime != null) {
            session.context.entityBridge.entityRuntime.debug().setEnemyIgnoreHitsCountdownForTest(slot, value);
        }
    }

    void setEntityThrownDirectionForTest(int slot, int value) {
        if (session.context.entityBridge.entityRuntime != null) {
            session.context.entityBridge.entityRuntime.configuration().setThrownDirection(slot, value);
        }
    }

    void setEntityLedgeTimerForTest(int slot, int value) {
        if (session.context.entityBridge.entityRuntime != null) {
            session.context.entityBridge.entityRuntime.configuration().setLedgeTransitionTimer(slot, value);
        }
    }

    void setEntitySwitchBlocksStateForTest(int value) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(
                "Switch-block state must be an unsigned byte: " + value);
        }
        session.context.switchBlocksState = value;
        session.synchronizeSwitchBlockCollisionState();
    }

    void setSwitchableObjectAnimationStageForTest(int value) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(
                "Switch-block animation stage must be an unsigned byte: " + value);
        }
        session.context.switchableObjectAnimationStage = value;
    }

    int entitySwitchBlocksStateForTest() {
        return session.context.switchBlocksState & 0xFF;
    }

    int switchableObjectAnimationStageForTest() {
        return session.context.switchableObjectAnimationStage & 0xFF;
    }

    boolean linkCollisionPointBlockedForTest(int pixelX, int pixelY) {
        return session.context.overworldCollision.pointBlocked(pixelX, pixelY);
    }

    int entityLedgeTimerForTest(int slot) {
        return session.context.entityBridge.entityRuntime == null ? 0 : session.context.entityBridge.entityRuntime.enemies().ledgeTransitionTimer(slot);
    }

    int entityFallingTargetXForTest(int slot) {
        return session.context.entityBridge.entityRuntime == null ? 0 : session.context.entityBridge.entityRuntime.enemies().fallingTargetX(slot);
    }

    int entityTransitionCountdownForTest(int slot) {
        if (session.context.entityBridge.entityRuntime == null) {
            return 0;
        }
        RoomEntity entity = session.context.activeRoom == null ? null : session.context.activeRoom.entities().slots().get(slot);
        return entity != null && EntityTypeRules.isDungeonWarpType(entity.type())
            ? session.context.entityBridge.entityRuntime.inspection().warpTransitionCountdownForTest(slot)
            : session.context.entityBridge.entityRuntime.enemies().transitionCountdown(slot);
    }

    int roomTriggerCountForTest() {
        return session.context.roomTriggerCount & 0xFF;
    }

    int activeRoomEventForTest() {
        return session.context.activeRoomEvent & 0xFF;
    }

    boolean hasDungeonInstrumentForTest(int mapId) {
        return mapId >= 0 && mapId < session.context.roomState.dungeonProgressFlags.length
            && (session.context.roomState.dungeonProgressFlags[mapId] & 0x02) != 0;
    }

    int indoorTorchPaletteEffectAddressForTest() {
        return session.context.indoorTorchPaletteEffect.effectAddress();
    }

    void igniteIndoorTorchPaletteForTest() {
        session.context.indoorTorchPaletteEffect.igniteTorch();
    }

    void setRoomTriggerCountForTest(int value) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException("Room trigger count must be an unsigned byte");
        }
        session.context.roomTriggerCount = value;
    }

    int entityDropSpeedXForTest(int slot) {
        return session.context.entityBridge.entityRuntime == null ? 0 : session.context.entityBridge.entityRuntime.inspection().dropSpeedX(slot);
    }

    int entityDropSpeedZForTest(int slot) {
        return session.context.entityBridge.entityRuntime == null ? 0 : session.context.entityBridge.entityRuntime.inspection().dropSpeedZ(slot);
    }

    void replaceEntityRuntimeForTest(RoomEntityRuntime runtime) {
        session.context.entityBridge.entityRuntime = runtime;
    }

    int objectAtRoomLocation(int location) {
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0)
            + (location & 0x0F);
        int[] objects = session.context.activeRoom.roomObjectsArea();
        return areaIndex < 0 || areaIndex >= objects.length ? 0xFF : objects[areaIndex] & 0xFF;
    }

    int entityPhysicsFlagsForTest(int slot) {
        return session.context.entityBridge.entityRuntime == null ? 0 : session.context.entityBridge.entityRuntime.enemies().physicsFlags(slot);
    }

    int adjacentIndoorRoomIdForBombWall(int wallIndex) { return session.context.scrollNavigation.adjacentIndoorRoomIdForBombWall(session.context.activeRoom, session.context.indoorMapPosition, wallIndex); }

    int findIndoorMapPosition(int mapId, int roomId) { return session.context.roomState.findIndoorMapPosition(mapId, roomId); }

    int indoorMapLayoutOffset(int mapId) { return session.context.roomState.indoorMapLayoutOffset(mapId); }

    byte[] indoorStatusTableForMap(int mapId) { return session.context.roomState.indoorStatusTableForMap(mapId); }

    int activeRoomStatusFlags() { return session.context.roomState.activeRoomStatusFlags(session.context.activeRoom); }
}
