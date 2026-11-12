package linksawakening.world.room;

import linksawakening.world.entity.RoomEntityRuntime;

/** Test-only setup for cross-package integration cases. */
public final class RoomSessionTestAccess {
    private RoomSessionTestAccess() {}

    public static void replaceEntityRuntime(RoomSession session, RoomEntityRuntime runtime) {
        session.context.roomSessionViews.replaceEntityRuntimeForTest(runtime);
    }

    public static int crackedFloorCounter(RoomSession session) {
        return session.context.crackedFloorCounter;
    }

    public static int overworldStatus(RoomSession session, int roomId) {
        return session.context.roomSessionViews.overworldRoomStatusForTest(roomId);
    }
}
