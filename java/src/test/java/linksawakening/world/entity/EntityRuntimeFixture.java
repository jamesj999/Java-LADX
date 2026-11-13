package linksawakening.world.entity;

/** Test-side entity setup and inspection using real ROM-free runtime collaborators. */
public final class EntityRuntimeFixture {
    private EntityRuntimeFixture() {}

    static RoomEntityRuntime emptyRuntime() {
        RoomEntity[] slots = new RoomEntity[RoomEntity.SLOT_COUNT];
        java.util.Arrays.setAll(slots, RoomEntity::disabled);
        return RoomEntityRuntime.builder(new RoomEntitySnapshot(java.util.Arrays.asList(slots)))
                .randomByteSupplier(() -> 0)
                .build();
    }

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
