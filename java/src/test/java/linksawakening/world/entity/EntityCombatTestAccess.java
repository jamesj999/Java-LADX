package linksawakening.world.entity;

public final class EntityCombatTestAccess {
    private EntityCombatTestAccess() {}
    public static boolean overlapsSword(RoomEntity entity, int x, int width, int y, int height) {
        return RoomEntityCombatRules.overlapsSword(entity, x, width, y, height);
    }
}
