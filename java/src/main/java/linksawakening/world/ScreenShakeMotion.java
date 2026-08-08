package linksawakening.world;

/** Bank-$14 func_014_54F8's screen-shake countdown and phase tables. */
final class ScreenShakeMotion {
    private static final int[] HORIZONTAL_BY_PHASE = {0, 2, 0, -2, 0, 0, 0, 0};
    private static final int[] VERTICAL_BY_PHASE = {0, 0, 0, 0, 0, 2, 0, -2};

    private int countdown;
    private int phase;
    private int horizontal;
    private int vertical;

    void start(int countdown, int phase) {
        if (countdown < 0 || countdown > 0xFF) {
            throw new IllegalArgumentException("Screen-shake countdown must be an unsigned byte");
        }
        if (phase < 0 || phase > 0x04 || (phase & 0x01) != 0) {
            throw new IllegalArgumentException("Screen-shake phase must be 0, 2, or 4");
        }
        this.countdown = countdown;
        this.phase = phase;
    }

    void tick() {
        if (countdown == 0) {
            return;
        }
        countdown = (countdown - 1) & 0xFF;
        int tableIndex = (countdown & 0x03) + phase;
        horizontal = HORIZONTAL_BY_PHASE[tableIndex];
        vertical = VERTICAL_BY_PHASE[tableIndex];
    }

    int countdown() {
        return countdown;
    }

    int horizontal() {
        return horizontal;
    }

    int vertical() {
        return vertical;
    }
}
