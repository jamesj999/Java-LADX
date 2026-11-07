package linksawakening.app.gameplay;

import linksawakening.entity.Link;
import linksawakening.world.ScrollController;
import linksawakening.world.TransitionController;

/** Advances scrolling before warp transitions and clears platform state at the gate. */
public final class TransitionPhase implements Runnable {
    private final ScrollController scroll;
    private final TransitionController transition;
    private final Link link;
    private final int scrollSpeed;

    public TransitionPhase(ScrollController scroll, TransitionController transition, Link link, int scrollSpeed) {
        this.scroll = scroll;
        this.transition = transition;
        this.link = link;
        this.scrollSpeed = scrollSpeed;
    }

    @Override public void run() {
        if (scroll.isActive()) scroll.tick(scrollSpeed);
        transition.tick();
        if (link != null && (scroll.isActive() || transition.isInputBlocked())) {
            link.clearSideViewPlatformState();
        }
    }
}
