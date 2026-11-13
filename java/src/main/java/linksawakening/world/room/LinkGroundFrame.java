package linksawakening.world.room;

import java.util.Optional;
import linksawakening.state.LinkMotionState;

/** ApplyLinkGroundMotion register inputs; arbitrary dispatch bytes gate numerically. */
public record LinkGroundFrame(
        int pixelX,
        int pixelY,
        boolean airborne,
        boolean interactionBlocked,
        boolean dialogActive,
        boolean roomTransitionActive,
        int rawMotionState,
        int slowWalkingSpeed,
        int itemUsageContext) {
    public Optional<LinkMotionState> knownState() {
        return Optional.ofNullable(motionState());
    }
    /** Legacy nullable classification, absent for an unknown dispatch byte. */
    public LinkMotionState motionState() {
        return LinkMotion.stateForRomByte(rawMotionState);
    }
    /** Compatibility: rawMotionState has always been authoritative. */
    @Deprecated(forRemoval = true)
    public LinkGroundFrame(
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
        this(
                pixelX,
                pixelY,
                airborne,
                interactionBlocked,
                dialogActive,
                roomTransitionActive,
                rawMotionState,
                slowWalkingSpeed,
                itemUsageContext);
    }
    /** Compatibility: use the raw motion dispatch value. */
    @Deprecated(forRemoval = true)
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
                motionState.romValue(),
                slowWalkingSpeed,
                itemUsageContext);
    }
}
