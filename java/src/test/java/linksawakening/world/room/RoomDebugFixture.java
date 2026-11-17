package linksawakening.world.room;

import linksawakening.physics.OverworldCollision;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.EntityTypeRules;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityRuntime;

/** Test-only setup and inspection without adding hooks to the production facade. */
final class RoomDebugFixture {
    private final RoomSession session;

    private RoomDebugFixture(RoomSession session) {
        this.session = session;
    }

    static RoomDebugFixture of(RoomSession session) {
        return new RoomDebugFixture(session);
    }

    int overworldRoomStatusForTest(int roomId) {
        return RoomSessionInspection.component(session, RoomWorldState.class).overworldRoomStatusForTest(roomId);
    }

    int indoorRoomStatusForTest(int mapId, int roomId) {
        return RoomSessionInspection.component(session, RoomWorldState.class).indoorRoomStatusForTest(mapId, roomId);
    }

    void setEnemyDropCountersForTest(int droppedItemCount, int droppedBombArrowCount) {
        RoomSessionInspection.component(session, RoomEntitySessionState.class)
                .enemyDropCounters(new EnemyDropResolver.CounterState(droppedItemCount, droppedBombArrowCount));
    }

    EnemyDropResolver.CounterState enemyDropCountersForTest() {
        return RoomSessionInspection.component(session, RoomEntitySessionState.class)
                .enemyDropCounters();
    }

    void setSwitchButtonPressedForTest(int value) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException("Switch-button state must be an unsigned byte");
        }
        RoomSessionInspection.component(session, RoomPuzzleState.class).switchButtonPressed(value);
    }

    int switchButtonPressedForTest() {
        return RoomSessionInspection.component(session, RoomPuzzleState.class).switchButtonPressed();
    }

    int bombDirectionForTest(int slot) {
        return !session.hasRoom()
                ? 0xFF
                : RoomSessionInspection.loadedRoom(session)
                        .entities()
                        .inspection()
                        .bombDirection(slot);
    }

    void setColorShellStateForTest(
            int slot, int state, int transitionCountdown, int direction, int speedX, int speedY) {
        if (session.hasRoom()) {
            RoomSessionInspection.loadedRoom(session)
                    .entities()
                    .debug()
                    .setColorShellState(slot, state, transitionCountdown, direction, speedX, speedY);
        }
    }

    int entityGroundStatusForTest(int slot) {
        if (!session.hasRoom()) {
            return 0;
        }
        return RoomSessionInspection.loadedRoom(session).entities().inspection().groundStatus(slot);
    }

    void setEntityIgnoreHitsCountdownForTest(int slot, int value) {
        if (session.hasRoom()) {
            RoomSessionInspection.loadedRoom(session).entities().debug().setEnemyIgnoreHitsCountdown(slot, value);
        }
    }

    void setEntityThrownDirectionForTest(int slot, int value) {
        if (session.hasRoom()) {
            RoomSessionInspection.loadedRoom(session).entities().configuration().setThrownDirection(slot, value);
        }
    }

    void setEntityLedgeTimerForTest(int slot, int value) {
        if (session.hasRoom()) {
            RoomSessionInspection.loadedRoom(session).entities().configuration().setLedgeTransitionTimer(slot, value);
        }
    }

    void setEntitySwitchBlocksStateForTest(int value) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("Switch-block state must be an unsigned byte: " + value);
        }
        RoomSessionInspection.component(session, RoomTerrainState.class).switchBlocksState(value);
        RoomSessionInspection.component(session, RoomLoadCoordinator.class).synchronizeSwitchBlockCollisionState();
    }

    void setSwitchableObjectAnimationStageForTest(int value) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("Switch-block animation stage must be an unsigned byte: " + value);
        }
        RoomSessionInspection.component(session, RoomTerrainState.class).switchableObjectAnimationStage(value);
    }

    int entitySwitchBlocksStateForTest() {
        return RoomSessionInspection.component(session, RoomTerrainState.class).switchBlocksState() & 0xFF;
    }

    int switchableObjectAnimationStageForTest() {
        return RoomSessionInspection.component(session, RoomTerrainState.class).switchableObjectAnimationStage() & 0xFF;
    }

    boolean linkCollisionPointBlockedForTest(int pixelX, int pixelY) {
        return RoomSessionInspection.component(session, OverworldCollision.class)
                .pointBlocked(pixelX, pixelY);
    }

    int entityLedgeTimerForTest(int slot) {
        return !session.hasRoom()
                ? 0
                : RoomSessionInspection.loadedRoom(session).entities().enemies().ledgeTransitionTimer(slot);
    }

    int entityFallingTargetXForTest(int slot) {
        return !session.hasRoom()
                ? 0
                : RoomSessionInspection.loadedRoom(session).entities().enemies().fallingTargetX(slot);
    }

    int entityTransitionCountdownForTest(int slot) {
        if (!session.hasRoom()) {
            return 0;
        }
        RoomEntity entity = session.rooms().activeRoomOrNull() == null
                ? null
                : session.rooms().activeRoomOrNull().entityView().slots().get(slot);
        return entity != null && EntityTypeRules.isDungeonWarpType(entity.type())
                ? RoomSessionInspection.loadedRoom(session)
                        .entities()
                        .inspection()
                        .warpTransitionCountdown(slot)
                : RoomSessionInspection.loadedRoom(session).entities().enemies().transitionCountdown(slot);
    }

    int roomTriggerCountForTest() {
        return RoomSessionInspection.loadedRoom(session).contents().puzzles().roomTriggerCount() & 0xFF;
    }

    int activeRoomEventForTest() {
        return RoomSessionInspection.loadedRoom(session).contents().puzzles().activeRoomEvent() & 0xFF;
    }

    boolean hasDungeonInstrumentForTest(int mapId) {
        return mapId >= 0
                && mapId
                        < RoomSessionInspection.component(session, RoomWorldState.class)
                                .dungeonProgressFlags()
                                .length
                && (RoomSessionInspection.component(session, RoomWorldState.class)
                                        .dungeonProgressFlags()[mapId]
                                & 0x02)
                        != 0;
    }

    int indoorTorchPaletteEffectAddressForTest() {
        return RoomSessionInspection.component(session, IndoorTorchPaletteEffect.class)
                .effectAddress();
    }

    void igniteIndoorTorchPaletteForTest() {
        RoomSessionInspection.component(session, IndoorTorchPaletteEffect.class).igniteTorch();
    }

    void setRoomTriggerCountForTest(int value) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException("Room trigger count must be an unsigned byte");
        }
        RoomSessionInspection.loadedRoom(session).contents().puzzles().roomTriggerCount(value);
    }

    int entityDropSpeedXForTest(int slot) {
        return !session.hasRoom()
                ? 0
                : RoomSessionInspection.loadedRoom(session)
                        .entities()
                        .inspection()
                        .dropSpeedX(slot);
    }

    int entityDropSpeedZForTest(int slot) {
        return !session.hasRoom()
                ? 0
                : RoomSessionInspection.loadedRoom(session)
                        .entities()
                        .inspection()
                        .dropSpeedZ(slot);
    }

    void replaceEntityRuntimeForTest(RoomEntityRuntime runtime) {
        session.installRoom(new RoomRuntime(
                RoomSessionInspection.loadedRoom(session).contents(),
                runtime,
                RoomSessionInspection.component(session, RoomRuntime.Services.class)));
    }

    int entityPhysicsFlagsForTest(int slot) {
        return !session.hasRoom()
                ? 0
                : RoomSessionInspection.loadedRoom(session).entities().enemies().physicsFlags(slot);
    }
}
