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
        int itemUsageContext)
        implements LinkGroundInputs {
    @Override
    public Optional<LinkMotionState> knownState() {
        return Optional.ofNullable(motionState());
    }
    /** Nullable classification, absent for an unknown dispatch byte. */
    @Override
    public LinkMotionState motionState() {
        return LinkMotion.stateForRomByte(rawMotionState);
    }
}
