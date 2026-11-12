package linksawakening.world.entity;

/** Inputs for SlimeEyeController.resolveCombat. */
public record SlimeEyeCombatFrame(
        RoomEntity e,
        int frame,
        int lx,
        int ly,
        boolean airborne,
        boolean interactive,
        boolean sword,
        int sx,
        int sw,
        int sy,
        int sh,
        EnemyAttackContext attack,
        java.util.List<linksawakening.world.entity.event.EntityCombatEvent> events) {}
