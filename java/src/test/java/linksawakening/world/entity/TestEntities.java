package linksawakening.world.entity;

public final class TestEntities {
    private TestEntities() {}

    public static RoomEntityBuilder active(int type) {
        return new RoomEntityBuilder()
                .type(type)
                .status(EntityStatus.ACTIVE)
                .spriteDefinition(linksawakening.entity.EntitySpriteDefinition.unsupported(type));
    }
}
