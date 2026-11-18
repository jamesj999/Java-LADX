package linksawakening.app.state;

/**
 * Frame-local input consumption shared by gameplay routing and completion. One instance is retained for the
 * owning session.
 */
public final class GameplayInputState {
    private boolean consumed;

    public boolean consumed() {
        return consumed;
    }

    public void consume() {
        consumed = true;
    }

    public void complete() {
        consumed = false;
    }
}
