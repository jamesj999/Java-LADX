package linksawakening.audio.apu;

/** Sample-clock approximation of the APU's 256 Hz length timer. */
final class LengthCounter {
    private final int maximum;
    private final int sampleRate;
    private int remaining;
    private final boolean square;
    private double clock;
    private boolean clocksLength = true;

    LengthCounter(int maximum, int sampleRate) {
        this(maximum, sampleRate, false);
    }

    LengthCounter(int maximum, int sampleRate, boolean square) {
        this.square = square;
        this.maximum = maximum;
        this.sampleRate = sampleRate;
    }

    void reset() {
        remaining = 0;
        clock = 0;
        clocksLength = true;
    }

    void load(int value) {
        remaining = maximum - (value & (maximum - 1));
    }

    void trigger() {
        trigger(false);
    }

    void trigger(boolean lengthEnabled) {
        if (remaining == 0) {
            remaining = maximum;
            if (lengthEnabled && !clocksLength) remaining--;
        }
    }

    boolean enableLength() {
        return !clocksLength && clockLength();
    }

    private boolean clockLength() {
        return remaining > 0 && --remaining == 0;
    }

    boolean tick(boolean enabled) {
        clock += (square ? 512.0 : 256.0) / sampleRate;
        boolean expired = false;
        while (clock >= 1.0) {
            clock -= 1.0;
            if (enabled && clocksLength && clockLength()) expired = true;
            if (square) clocksLength = !clocksLength;
        }
        return expired;
    }
}
