package linksawakening.audio.apu;

/** Sample-clock approximation of the APU's 256 Hz length timer. */
final class LengthCounter {
    private final int maximum;
    private final int sampleRate;
    private int remaining;
    private double clock;

    LengthCounter(int maximum, int sampleRate) {
        this.maximum = maximum;
        this.sampleRate = sampleRate;
    }

    void reset() {
        remaining = 0;
        clock = 0;
    }

    void load(int value) {
        remaining = maximum - (value & (maximum - 1));
    }

    void trigger() {
        if (remaining == 0) {
            remaining = maximum;
        }
    }

    boolean tick(boolean enabled) {
        clock += 256.0 / sampleRate;
        boolean expired = false;
        while (clock >= 1.0) {
            clock -= 1.0;
            if (enabled && remaining > 0 && --remaining == 0) {
                expired = true;
            }
        }
        return expired;
    }
}
