package linksawakening.app.state;

/**
 * Byte-sized simulation frame clock, explicitly seedable by headless callers. One instance is retained for
 * the owning session.
 */
public final class FrameClock {
    private int value;

    public int value() {
        return value;
    }

    public void seed(int value) {
        this.value = value;
    }

    public void advance() {
        value = (value + 1) & 0xFF;
    }
}
