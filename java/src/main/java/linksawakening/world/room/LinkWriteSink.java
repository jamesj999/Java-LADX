package linksawakening.world.room;

import linksawakening.world.entity.event.*;

/** Synchronous drains: all streams are removed from the outbox before callbacks start. */
public interface LinkWriteSink {
    interface Positions {
        void platform(SideViewPlatformLinkRequest request);

        void position(LinkFinalPositionRequest request);

        void pot(SideViewPotLinkRequest request);
    }

    interface Attacks {
        void clear(LinkAttackClearRequest request);

        void step(LinkAttackStepRequest request);
    }
}
