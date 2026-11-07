package linksawakening.world;

import linksawakening.world.entity.RoomEntityRuntime;

/** Test-only setup for cross-package integration cases. */
public final class RoomSessionTestAccess {
    private RoomSessionTestAccess() {}
    public static void replaceEntityRuntime(RoomSession session, RoomEntityRuntime runtime) {
        session.replaceEntityRuntimeForTest(runtime);
    }
    public static int overworldStatus(RoomSession session, int roomId) {
        return session.overworldRoomStatusForTest(roomId);
    }
}
