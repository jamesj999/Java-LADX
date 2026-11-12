package linksawakening.world.room;

import linksawakening.state.LinkMotionState;

/** ApplyLinkGroundMotion register inputs; unknown motion bytes remain available to raw gates. */
public record LinkGroundFrame(
        int pixelX,
        int pixelY,
        boolean airborne,
        boolean interactionBlocked,
        boolean dialogActive,
        boolean roomTransitionActive,
        LinkMotionState motionState,
        int slowWalkingSpeed,
        int itemUsageContext,
        int rawMotionState) {
    public LinkGroundFrame {
        motionState = LinkMotion.stateForRomByte(rawMotionState);
    }

    public LinkGroundFrame(
            int pixelX,
            int pixelY,
            boolean airborne,
            boolean interactionBlocked,
            boolean dialogActive,
            boolean roomTransitionActive,
            LinkMotionState motionState,
            int slowWalkingSpeed,
            int itemUsageContext) {
        this(
                pixelX,
                pixelY,
                airborne,
                interactionBlocked,
                dialogActive,
                roomTransitionActive,
                motionState,
                slowWalkingSpeed,
                itemUsageContext,
                motionState.romValue());
    }
    /** @deprecated Pass LinkMotionState; retained for app/gameplay/LinkEquipmentPhase.java. */
    @Deprecated(forRemoval = true)
    public LinkGroundFrame(
            int pixelX,
            int pixelY,
            boolean airborne,
            boolean interactionBlocked,
            boolean dialogActive,
            boolean roomTransitionActive,
            int motionState,
            int slowWalkingSpeed,
            int itemUsageContext) {
        this(
                pixelX,
                pixelY,
                airborne,
                interactionBlocked,
                dialogActive,
                roomTransitionActive,
                LinkMotion.stateForRomByte(motionState),
                slowWalkingSpeed,
                itemUsageContext,
                motionState);
    }
}
