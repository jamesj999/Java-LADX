package linksawakening.world.entity;

/** Test-only runtime setup across integration-test packages. */
public final class EntityRuntimeTestAccess {
    private EntityRuntimeTestAccess() {}

    public static int killCount(RoomEntityRuntime runtime) {
        return runtime.inspection().killCount();
    }

    public static void setEnemyHealthForTest(RoomEntityRuntime runtime, int slot, int value) {
        EntityRuntimeFixture.debug(runtime).setEnemyHealthForTest(slot, value);
    }

    public static void setEntityRoomIdForTest(RoomEntityRuntime runtime, int roomId) {
        EntityRuntimeFixture.debug(runtime).setEntityRoomIdForTest(roomId);
    }

    public static void setGeniePrivateState1ForTest(RoomEntityRuntime runtime, int slot, int value) {
        EntityRuntimeFixture.debug(runtime).setGeniePrivateState1ForTest(slot, value);
    }

    public static void setHidingSlimeKeyTransitionCountdownForTest(RoomEntityRuntime runtime, int slot, int value) {
        EntityRuntimeFixture.debug(runtime).setHidingSlimeKeyTransitionCountdownForTest(slot, value);
    }

    public static void setTransitionCountdownForTest(RoomEntityRuntime runtime, int slot, int countdown) {
        EntityRuntimeFixture.debug(runtime).setTransitionCountdownForTest(slot, countdown);
    }

    public static void setTransitionSequenceCounterForTest(RoomEntityRuntime runtime, int counter) {
        EntityRuntimeFixture.debug(runtime).setTransitionSequenceCounterForTest(counter);
    }
}
