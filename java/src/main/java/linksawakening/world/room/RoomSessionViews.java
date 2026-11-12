package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import java.util.List;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.physics.OverworldCollision;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.EntityTypeRules;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.event.HeartPiecePickupUpdate;
import linksawakening.world.entity.event.LiftedEntityState;
import linksawakening.world.transition.Warp;

/** RoomSessionViews operations for the live room session. */
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

    int overworldRoomStatusForTest(int roomId) {
        return dependencies.roomState().overworldRoomStatusForTest(roomId);
    }

    int indoorRoomStatusForTest(int mapId, int roomId) {
        return dependencies.roomState().indoorRoomStatusForTest(mapId, roomId);
    }

    void setEnemyDropCountersForTest(int droppedItemCount, int droppedBombArrowCount) {
        dependencies.entityFrame().enemyDropCounters =
                new EnemyDropResolver.CounterState(droppedItemCount, droppedBombArrowCount);
    }

    EnemyDropResolver.CounterState enemyDropCountersForTest() {
        return dependencies.entityFrame().enemyDropCounters;
    }

    void setSwitchButtonPressedForTest(int value) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException("Switch-button state must be an unsigned byte");
        }
        dependencies.puzzles().switchButtonPressed = value;
    }

    int switchButtonPressedForTest() {
        return dependencies.puzzles().switchButtonPressed;
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

    int bombDirectionForTest(int slot) {
        return dependencies.entityBridge().get().entityRuntime == null
                ? 0xFF
                : dependencies.entityBridge().get().entityRuntime.inspection().bombDirection(slot);
    }

    void setColorShellSoundSink(GameplaySoundSink soundSink) {
        dependencies.entityFrame().colorShellSoundSink = soundSink == null ? GameplaySoundSink.none() : soundSink;
    }

    void setColorShellStateForTest(
            int slot, int state, int transitionCountdown, int direction, int speedX, int speedY) {
        if (dependencies.entityBridge().get().entityRuntime != null) {
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .debug()
                    .setColorShellStateForTest(slot, state, transitionCountdown, direction, speedX, speedY);
        }
    }

    int entityGroundStatusForTest(int slot) {
        if (dependencies.entityBridge().get().entityRuntime == null) {
            return 0;
        }
        return dependencies.entityBridge().get().entityRuntime.inspection().groundStatus(slot);
    }

    void setEntityIgnoreHitsCountdownForTest(int slot, int value) {
        if (dependencies.entityBridge().get().entityRuntime != null) {
            dependencies.entityBridge().get().entityRuntime.debug().setEnemyIgnoreHitsCountdownForTest(slot, value);
        }
    }

    void setEntityThrownDirectionForTest(int slot, int value) {
        if (dependencies.entityBridge().get().entityRuntime != null) {
            dependencies.entityBridge().get().entityRuntime.configuration().setThrownDirection(slot, value);
        }
    }

    void setEntityLedgeTimerForTest(int slot, int value) {
        if (dependencies.entityBridge().get().entityRuntime != null) {
            dependencies.entityBridge().get().entityRuntime.configuration().setLedgeTransitionTimer(slot, value);
        }
    }

    void setEntitySwitchBlocksStateForTest(int value) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("Switch-block state must be an unsigned byte: " + value);
        }
        dependencies.terrain().switchBlocksState = value;
        dependencies.roomLoadCoordinator().get().synchronizeSwitchBlockCollisionState();
    }

    void setSwitchableObjectAnimationStageForTest(int value) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("Switch-block animation stage must be an unsigned byte: " + value);
        }
        dependencies.terrain().switchableObjectAnimationStage = value;
    }

    int entitySwitchBlocksStateForTest() {
        return dependencies.terrain().switchBlocksState & 0xFF;
    }

    int switchableObjectAnimationStageForTest() {
        return dependencies.terrain().switchableObjectAnimationStage & 0xFF;
    }

    boolean linkCollisionPointBlockedForTest(int pixelX, int pixelY) {
        return dependencies.overworldCollision().pointBlocked(pixelX, pixelY);
    }

    int entityLedgeTimerForTest(int slot) {
        return dependencies.entityBridge().get().entityRuntime == null
                ? 0
                : dependencies.entityBridge().get().entityRuntime.enemies().ledgeTransitionTimer(slot);
    }

    int entityFallingTargetXForTest(int slot) {
        return dependencies.entityBridge().get().entityRuntime == null
                ? 0
                : dependencies.entityBridge().get().entityRuntime.enemies().fallingTargetX(slot);
    }

    int entityTransitionCountdownForTest(int slot) {
        if (dependencies.entityBridge().get().entityRuntime == null) {
            return 0;
        }
        RoomEntity entity = dependencies.location().activeRoom == null
                ? null
                : dependencies.location().activeRoom.entities().slots().get(slot);
        return entity != null && EntityTypeRules.isDungeonWarpType(entity.type())
                ? dependencies.entityBridge().get().entityRuntime.inspection().warpTransitionCountdownForTest(slot)
                : dependencies.entityBridge().get().entityRuntime.enemies().transitionCountdown(slot);
    }

    int roomTriggerCountForTest() {
        return dependencies.puzzles().roomTriggerCount & 0xFF;
    }

    int activeRoomEventForTest() {
        return dependencies.puzzles().activeRoomEvent & 0xFF;
    }

    boolean hasDungeonInstrumentForTest(int mapId) {
        return mapId >= 0
                && mapId < dependencies.roomState().dungeonProgressFlags.length
                && (dependencies.roomState().dungeonProgressFlags[mapId] & 0x02) != 0;
    }

    int indoorTorchPaletteEffectAddressForTest() {
        return dependencies.indoorTorchPaletteEffect().effectAddress();
    }

    void igniteIndoorTorchPaletteForTest() {
        dependencies.indoorTorchPaletteEffect().igniteTorch();
    }

    void setRoomTriggerCountForTest(int value) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException("Room trigger count must be an unsigned byte");
        }
        dependencies.puzzles().roomTriggerCount = value;
    }

    int entityDropSpeedXForTest(int slot) {
        return dependencies.entityBridge().get().entityRuntime == null
                ? 0
                : dependencies.entityBridge().get().entityRuntime.inspection().dropSpeedX(slot);
    }

    int entityDropSpeedZForTest(int slot) {
        return dependencies.entityBridge().get().entityRuntime == null
                ? 0
                : dependencies.entityBridge().get().entityRuntime.inspection().dropSpeedZ(slot);
    }

    void replaceEntityRuntimeForTest(RoomEntityRuntime runtime) {
        dependencies.entityBridge().get().entityRuntime = runtime;
    }

    int objectAtRoomLocation(int location) {
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        int[] objects = dependencies.location().activeRoom.roomObjectsArea();
        return areaIndex < 0 || areaIndex >= objects.length ? 0xFF : objects[areaIndex] & 0xFF;
    }

    int entityPhysicsFlagsForTest(int slot) {
        return dependencies.entityBridge().get().entityRuntime == null
                ? 0
                : dependencies.entityBridge().get().entityRuntime.enemies().physicsFlags(slot);
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
            RoomPuzzleState puzzles,
            RoomTerrainState terrain,
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            IndoorTorchPaletteEffect indoorTorchPaletteEffect,
            OverworldCollision overworldCollision,
            java.util.function.Supplier<RoomLoadCoordinator> roomLoadCoordinator,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomStoryState> roomStoryState,
            RoomScrollNavigation scrollNavigation) {}
}
