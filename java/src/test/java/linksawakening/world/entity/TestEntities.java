package linksawakening.world.entity;

public final class TestEntities {
    private TestEntities() {}

    public static RoomEntitySnapshot room(RoomEntity... loaded) {
        RoomEntity[] slots = new RoomEntity[RoomEntity.SLOT_COUNT];
        java.util.Arrays.setAll(slots, RoomEntity::disabled);
        for (var entity : loaded) slots[entity.slot()] = entity;
        return new RoomEntitySnapshot(java.util.Arrays.asList(slots));
    }

    public static RoomEntityBuilder active(int type) {
        return new RoomEntityBuilder()
                .type(type)
                .status(EntityStatus.ACTIVE)
                .spriteDefinition(linksawakening.entity.EntitySpriteDefinition.unsupported(type));
    }
}
