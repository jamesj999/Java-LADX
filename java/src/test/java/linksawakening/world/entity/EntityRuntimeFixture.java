package linksawakening.world.entity;

/** Test-side entity setup and inspection; production exposes only compatibility shims. */
public final class EntityRuntimeFixture {
    private EntityRuntimeFixture() {}

    static void disableWithoutPersistence(RoomEntityRuntime runtime, int slot) {
        runtime.lifecycle.disableEntityWithoutPersistence(slot);
    }

    static EntityRoomInspectionFixture inspection(RoomEntityRuntime runtime) {
        return new EntityRoomInspectionFixture(runtime);
    }

    static EntityBossInspectionFixture bosses(RoomEntityRuntime runtime) {
        return new EntityBossInspectionFixture(runtime);
    }

    static EntityEnemyInspectionFixture enemies(RoomEntityRuntime runtime) {
        return new EntityEnemyInspectionFixture(runtime);
    }

    static EntityDebugConfigurationFixture debug(RoomEntityRuntime runtime) {
        return new EntityDebugConfigurationFixture(runtime);
    }
}
