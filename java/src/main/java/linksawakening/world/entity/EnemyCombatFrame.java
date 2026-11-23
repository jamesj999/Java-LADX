package linksawakening.world.entity;

/** Inputs for EntityCombatResolver.resolveCombat. */
record EnemyCombatFrame(
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
        // Optional attack details; null selects the existing default attack context.
        EnemyAttackContext attackContext)
        implements EnemyCombatInputs {}
