package linksawakening.world.room;

import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkAttackStepRequest;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.SideViewPotLinkRequest;

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
