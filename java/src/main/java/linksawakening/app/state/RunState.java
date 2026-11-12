package linksawakening.app.state;

import linksawakening.app.screen.SessionLifecycle;

/**
 * Mutable run-loop lifetime, independent of input routing. One instance is retained for the owning session.
 */
public final class RunState implements SessionLifecycle {
    private boolean running = true;

    public boolean running() {
        return running;
    }

    public void requestStop() {
        running = false;
    }
}
