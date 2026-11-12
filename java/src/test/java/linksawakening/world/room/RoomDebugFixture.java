package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

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
        return session.components.entityBridge().entityRuntime == null
                ? 0xFF
                : session.components.entityBridge().entityRuntime.inspection().bombDirection(slot);
    }

    void setColorShellStateForTest(
            int slot, int state, int transitionCountdown, int direction, int speedX, int speedY) {
        if (session.components.entityBridge().entityRuntime != null) {
            session.components
                    .entityBridge()
                    .entityRuntime
                    .debug()
                    .setColorShellState(slot, state, transitionCountdown, direction, speedX, speedY);
        }
    }

    int entityGroundStatusForTest(int slot) {
        if (session.components.entityBridge().entityRuntime == null) {
            return 0;
        }
        return session.components.entityBridge().entityRuntime.inspection().groundStatus(slot);
    }

    void setEntityIgnoreHitsCountdownForTest(int slot, int value) {
        if (session.components.entityBridge().entityRuntime != null) {
            session.components.entityBridge().entityRuntime.debug().setEnemyIgnoreHitsCountdown(slot, value);
        }
    }

    void setEntityThrownDirectionForTest(int slot, int value) {
        if (session.components.entityBridge().entityRuntime != null) {
            session.components.entityBridge().entityRuntime.configuration().setThrownDirection(slot, value);
        }
    }

    void setEntityLedgeTimerForTest(int slot, int value) {
        if (session.components.entityBridge().entityRuntime != null) {
            session.components.entityBridge().entityRuntime.configuration().setLedgeTransitionTimer(slot, value);
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
        return session.components.entityBridge().entityRuntime == null
                ? 0
                : session.components.entityBridge().entityRuntime.enemies().ledgeTransitionTimer(slot);
    }

    int entityFallingTargetXForTest(int slot) {
        return session.components.entityBridge().entityRuntime == null
                ? 0
                : session.components.entityBridge().entityRuntime.enemies().fallingTargetX(slot);
    }

    int entityTransitionCountdownForTest(int slot) {
        if (session.components.entityBridge().entityRuntime == null) {
            return 0;
        }
        RoomEntity entity = session.components.location().activeRoom == null
                ? null
                : session.components.location().activeRoom.entityView().slots().get(slot);
        return entity != null && EntityTypeRules.isDungeonWarpType(entity.type())
                ? session.components.entityBridge().entityRuntime.inspection().warpTransitionCountdown(slot)
                : session.components.entityBridge().entityRuntime.enemies().transitionCountdown(slot);
    }

    int roomTriggerCountForTest() {
        return session.components.puzzles().roomTriggerCount & 0xFF;
    }

    int activeRoomEventForTest() {
        return session.components.puzzles().activeRoomEvent & 0xFF;
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
        session.components.puzzles().roomTriggerCount = value;
    }

    int entityDropSpeedXForTest(int slot) {
        return session.components.entityBridge().entityRuntime == null
                ? 0
                : session.components.entityBridge().entityRuntime.inspection().dropSpeedX(slot);
    }

    int entityDropSpeedZForTest(int slot) {
        return session.components.entityBridge().entityRuntime == null
                ? 0
                : session.components.entityBridge().entityRuntime.inspection().dropSpeedZ(slot);
    }

    void replaceEntityRuntimeForTest(RoomEntityRuntime runtime) {
        session.components.entityBridge().entityRuntime = runtime;
    }

    int entityPhysicsFlagsForTest(int slot) {
        return session.components.entityBridge().entityRuntime == null
                ? 0
                : session.components.entityBridge().entityRuntime.enemies().physicsFlags(slot);
    }
}
