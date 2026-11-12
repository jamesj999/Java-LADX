package linksawakening.world.room;

public record LinkGroundFrame(
        int pixelX,
        int pixelY,
        boolean airborne,
        boolean interactionBlocked,
        boolean dialogActive,
        boolean roomTransitionActive,
        int motionState,
        int slowWalkingSpeed,
        int itemUsageContext) {}
