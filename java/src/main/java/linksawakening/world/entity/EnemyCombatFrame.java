package linksawakening.world.entity;

/** Inputs for EntityCombatResolver.resolveCombat. */
public record EnemyCombatFrame(
        int frameCounter,
        int linkEntityX,
        int linkEntityY,
        boolean linkAirborne,
        int linkVerticalVelocity,
        boolean linkInteractive,
        boolean swordCollisionActive,
        int swordX,
        int swordWidth,
        int swordY,
        int swordHeight,
        EnemyAttackContext attackContext) {}
