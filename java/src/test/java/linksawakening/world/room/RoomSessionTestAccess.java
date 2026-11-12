package linksawakening.world.room;

import linksawakening.world.entity.RoomEntityRuntime;

/** Test-only setup for cross-package integration cases. */
public final class RoomSessionTestAccess {
    private RoomSessionTestAccess() {}

    public static void replaceEntityRuntime(RoomSession session, RoomEntityRuntime runtime) {
        RoomDebugFixture.of(session).replaceEntityRuntimeForTest(runtime);
    }

    public static int crackedFloorCounter(RoomSession session) {
        return session.components.terrain().crackedFloorCounter;
    }

    public static int overworldStatus(RoomSession session, int roomId) {
        return RoomDebugFixture.of(session).overworldRoomStatusForTest(roomId);
    }
}
