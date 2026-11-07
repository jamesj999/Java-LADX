package linksawakening.world.entity;

/** Test-only runtime setup across integration-test packages. */
public final class EntityRuntimeTestAccess {
    private EntityRuntimeTestAccess() {}
    public static int killCount(RoomEntityRuntime runtime) { return runtime.inspection().killCount(); }
    public static void setEnemyHealthForTest(RoomEntityRuntime runtime, int slot, int value) { runtime.debug().setEnemyHealthForTest(slot, value); }
    public static void setEntityRoomIdForTest(RoomEntityRuntime runtime, int roomId) { runtime.debug().setEntityRoomIdForTest(roomId); }
    public static void setGeniePrivateState1ForTest(RoomEntityRuntime runtime, int slot, int value) { runtime.debug().setGeniePrivateState1ForTest(slot, value); }
    public static void setHidingSlimeKeyTransitionCountdownForTest(RoomEntityRuntime runtime, int slot, int value) { runtime.debug().setHidingSlimeKeyTransitionCountdownForTest(slot, value); }
    public static void setTransitionCountdownForTest(RoomEntityRuntime runtime, int slot, int countdown) { runtime.debug().setTransitionCountdownForTest(slot, countdown); }
    public static void setTransitionSequenceCounterForTest(RoomEntityRuntime runtime, int counter) { runtime.debug().setTransitionSequenceCounterForTest(counter); }
}
