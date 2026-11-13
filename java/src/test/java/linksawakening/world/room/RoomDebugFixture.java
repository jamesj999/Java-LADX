package linksawakening.world.room;

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
        return session.components.roomState().overworldRoomStatusForTest(roomId);
    }

    int indoorRoomStatusForTest(int mapId, int roomId) {
        return session.components.roomState().indoorRoomStatusForTest(mapId, roomId);
    }

    void setEnemyDropCountersForTest(int droppedItemCount, int droppedBombArrowCount) {
        session.components.entityFrame().enemyDropCounters =
                new EnemyDropResolver.CounterState(droppedItemCount, droppedBombArrowCount);
    }

    EnemyDropResolver.CounterState enemyDropCountersForTest() {
        return session.components.entityFrame().enemyDropCounters;
    }

    void setSwitchButtonPressedForTest(int value) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException("Switch-button state must be an unsigned byte");
        }
        session.components.puzzles().switchButtonPressed = value;
    }

    int switchButtonPressedForTest() {
        return session.components.puzzles().switchButtonPressed;
    }

    int bombDirectionForTest(int slot) {
        return !session.hasRoom()
                ? 0xFF
                : session.loadedRoomOrNull().entities.inspection().bombDirection(slot);
    }

    void setColorShellStateForTest(
            int slot, int state, int transitionCountdown, int direction, int speedX, int speedY) {
        if (session.hasRoom()) {
            session.loadedRoomOrNull()
                    .entities
                    .debug()
                    .setColorShellState(slot, state, transitionCountdown, direction, speedX, speedY);
        }
    }

    int entityGroundStatusForTest(int slot) {
        if (!session.hasRoom()) {
            return 0;
        }
        return session.loadedRoomOrNull().entities.inspection().groundStatus(slot);
    }

    void setEntityIgnoreHitsCountdownForTest(int slot, int value) {
        if (session.hasRoom()) {
            session.loadedRoomOrNull().entities.debug().setEnemyIgnoreHitsCountdown(slot, value);
        }
    }

    void setEntityThrownDirectionForTest(int slot, int value) {
        if (session.hasRoom()) {
            session.loadedRoomOrNull().entities.configuration().setThrownDirection(slot, value);
        }
    }

    void setEntityLedgeTimerForTest(int slot, int value) {
        if (session.hasRoom()) {
            session.loadedRoomOrNull().entities.configuration().setLedgeTransitionTimer(slot, value);
        }
    }

    void setEntitySwitchBlocksStateForTest(int value) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("Switch-block state must be an unsigned byte: " + value);
        }
        session.components.terrain().switchBlocksState = value;
        session.components.roomLoadCoordinator().synchronizeSwitchBlockCollisionState();
    }

    void setSwitchableObjectAnimationStageForTest(int value) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("Switch-block animation stage must be an unsigned byte: " + value);
        }
        session.components.terrain().switchableObjectAnimationStage = value;
    }

    int entitySwitchBlocksStateForTest() {
        return session.components.terrain().switchBlocksState & 0xFF;
    }

    int switchableObjectAnimationStageForTest() {
        return session.components.terrain().switchableObjectAnimationStage & 0xFF;
    }

    boolean linkCollisionPointBlockedForTest(int pixelX, int pixelY) {
        return session.components.overworldCollision().pointBlocked(pixelX, pixelY);
    }

    int entityLedgeTimerForTest(int slot) {
        return !session.hasRoom()
                ? 0
                : session.loadedRoomOrNull().entities.enemies().ledgeTransitionTimer(slot);
    }

    int entityFallingTargetXForTest(int slot) {
        return !session.hasRoom()
                ? 0
                : session.loadedRoomOrNull().entities.enemies().fallingTargetX(slot);
    }

    int entityTransitionCountdownForTest(int slot) {
        if (!session.hasRoom()) {
            return 0;
        }
        RoomEntity entity = session.rooms().activeRoom() == null
                ? null
                : session.rooms().activeRoom().entityView().slots().get(slot);
        return entity != null && EntityTypeRules.isDungeonWarpType(entity.type())
                ? session.loadedRoomOrNull().entities.inspection().warpTransitionCountdown(slot)
                : session.loadedRoomOrNull().entities.enemies().transitionCountdown(slot);
    }

    int roomTriggerCountForTest() {
        return session.loadedRoomOrNull().contents.puzzles.roomTriggerCount & 0xFF;
    }

    int activeRoomEventForTest() {
        return session.loadedRoomOrNull().contents.puzzles.activeRoomEvent & 0xFF;
    }

    boolean hasDungeonInstrumentForTest(int mapId) {
        return mapId >= 0
                && mapId < session.components.roomState().dungeonProgressFlags.length
                && (session.components.roomState().dungeonProgressFlags[mapId] & 0x02) != 0;
    }

    int indoorTorchPaletteEffectAddressForTest() {
        return session.components.indoorTorchPaletteEffect().effectAddress();
    }

    void igniteIndoorTorchPaletteForTest() {
        session.components.indoorTorchPaletteEffect().igniteTorch();
    }

    void setRoomTriggerCountForTest(int value) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException("Room trigger count must be an unsigned byte");
        }
        session.loadedRoomOrNull().contents.puzzles.roomTriggerCount = value;
    }

    int entityDropSpeedXForTest(int slot) {
        return !session.hasRoom()
                ? 0
                : session.loadedRoomOrNull().entities.inspection().dropSpeedX(slot);
    }

    int entityDropSpeedZForTest(int slot) {
        return !session.hasRoom()
                ? 0
                : session.loadedRoomOrNull().entities.inspection().dropSpeedZ(slot);
    }

    void replaceEntityRuntimeForTest(RoomEntityRuntime runtime) {
        session.installRoom(new RoomRuntime(
                session.loadedRoomOrNull().contents, runtime, session.components.roomRuntimeServices()));
    }

    int entityPhysicsFlagsForTest(int slot) {
        return !session.hasRoom()
                ? 0
                : session.loadedRoomOrNull().entities.enemies().physicsFlags(slot);
    }
}
