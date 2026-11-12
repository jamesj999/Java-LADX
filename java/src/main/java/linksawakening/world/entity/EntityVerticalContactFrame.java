package linksawakening.world.entity;

/** Inputs for RoomEntityRuntime.resolveCombat. */
record EntityVerticalContactFrame(
        int frameCounter,
        int linkEntityX,
        int linkEntityY,
        boolean linkAirborne,
        boolean linkInteractive,
        boolean swordCollisionActive,
        int swordX,
        int swordWidth,
        int swordY,
        int swordHeight,
        EnemyAttackContext attackContext) {}
