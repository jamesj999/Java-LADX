package linksawakening.entity;

import java.util.Objects;
import linksawakening.entity.Link.WaterSplash;

/** Link signals state and operations used by ROM {@code ApplyLinkMotionState}. */
final class LinkSignals {
    private final LinkComponentRef<LinkAirborne> linkAirborne;
    private final LinkComponentRef<LinkStateMachine> linkStateMachine;

    boolean pitWarpPending;
    // At most one queued splash value; consume returns null when the event slot is empty.
    WaterSplash pendingWaterSplash;

    LinkSignals(Dependencies dependencies) {
        linkAirborne = Objects.requireNonNull(dependencies.linkAirborne, "linkAirborne");
        linkStateMachine = Objects.requireNonNull(dependencies.linkStateMachine, "linkStateMachine");
    }

    boolean accessRecoveryBlocksInput() {
        return linkStateMachine.get().status().recoveringInWater()
                || linkAirborne.get().pit().fallingIntoPit()
                || pitWarpPending;
    }

    boolean accessConsumePitWarp() {
        boolean result = pitWarpPending;
        pitWarpPending = false;
        return result;
    }

    boolean accessPitWarpPending() {
        return pitWarpPending;
    }

    WaterSplash accessConsumeWaterSplash() {
        WaterSplash result = pendingWaterSplash;
        pendingWaterSplash = null;
        return result;
    }

    static final class Dependencies {
        LinkComponentRef<LinkAirborne> linkAirborne;
        LinkComponentRef<LinkStateMachine> linkStateMachine;
    }
}
