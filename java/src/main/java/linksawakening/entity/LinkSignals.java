package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

/** Link signals state and operations used by ROM {@code ApplyLinkMotionState}. */
final class LinkSignals {
    private final LinkComponentRef<LinkAirborne> linkAirborne;
    private final LinkComponentRef<LinkStateMachine> linkStateMachine;

    LinkSignals(Dependencies dependencies) {
        linkAirborne = dependencies.linkAirborne;
        linkStateMachine = dependencies.linkStateMachine;
    }

    static final class Dependencies {
        LinkComponentRef<LinkAirborne> linkAirborne;
        LinkComponentRef<LinkStateMachine> linkStateMachine;
    }

    boolean pitWarpPending;
    WaterSplash pendingWaterSplash;

    boolean accessRecoveryBlocksInput() {
        return linkStateMachine.get().recoveringInWater || linkAirborne.get().fallingIntoPit || pitWarpPending;
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
}
