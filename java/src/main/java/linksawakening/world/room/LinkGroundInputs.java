package linksawakening.world.room;

import java.util.Optional;
import linksawakening.state.LinkMotionState;

/** Synchronous ground probes; borrowed implementations expire at the next capture. */
public interface LinkGroundInputs {
    int pixelX();

    int pixelY();

    boolean airborne();

    boolean interactionBlocked();

    boolean dialogActive();

    boolean roomTransitionActive();

    int rawMotionState();

    int slowWalkingSpeed();

    int itemUsageContext();

    default LinkMotionState motionState() {
        return LinkMotion.stateForRomByte(rawMotionState());
    }

    default Optional<LinkMotionState> knownState() {
        return Optional.ofNullable(motionState());
    }
}
