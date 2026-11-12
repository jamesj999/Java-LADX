package linksawakening.world.entity;

/** Inputs for RoomEntityRuntime.resolveCombat. */
record EntityAttackContactFrame(
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
        int swordHeight) {}
