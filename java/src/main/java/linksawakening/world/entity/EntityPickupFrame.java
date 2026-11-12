package linksawakening.world.entity;

/** Inputs for EntitySlotLifecycle.collectIfNeeded. */
record EntityPickupFrame(
        int frameCounter,
        int linkEntityX,
        int linkEntityY,
        boolean linkAirborne,
        boolean linkInteractive,
        int linkDirection,
        int linkZ,
        boolean swordActive,
        int swordX,
        int swordWidth,
        int swordY,
        int swordHeight)
        implements EntityPickupFrameInputs {}
