package linksawakening.world.entity.motion;

/** Inputs for HookshotChainMotion.copy. */
record HookshotChainSnapshot(
        HookshotChainMotion.State state,
        int x,
        int y,
        int z,
        int direction,
        int speedX,
        int speedY,
        int transitionCountdown,
        int speedXAccumulator,
        int speedYAccumulator,
        int entityState,
        boolean wallCollisionPending) {}
