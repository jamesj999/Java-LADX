package linksawakening.app;

import linksawakening.app.screen.SessionLifecycle;

/** Mutable run-loop lifetime, independent of input routing. */
public final class RunState implements SessionLifecycle {
    private boolean running = true;

    public boolean running() {
        return running;
    }

    public void requestStop() {
        running = false;
    }
}
