package linksawakening.app.gameplay;

import java.util.Objects;
import linksawakening.entity.Link;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/** Advances scrolling before warp transitions and clears platform state at the gate. */
public final class TransitionPhase implements GameplayFrame.Transitions {
    private final ScrollController scroll;
    private final TransitionController transition;
    private final Link link;
    private final int scrollSpeed;

    public TransitionPhase(ScrollController scroll, TransitionController transition, Link link, int scrollSpeed) {
        this.scroll = Objects.requireNonNull(scroll);
        this.transition = Objects.requireNonNull(transition);
        this.link = Objects.requireNonNull(link);
        this.scrollSpeed = scrollSpeed;
    }

    @Override
    public void run() {
        if (scroll.isActive()) scroll.tick(scrollSpeed);
        transition.tick();
        if (scroll.isActive() || transition.isInputBlocked()) {
            link.clearSideViewPlatformState();
        }
    }
}
