package linksawakening.audio.apu;

final class SquareChannel {
    private static final int[][] DUTY_PATTERNS = {
            {0, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 1, 1, 1},
            {0, 1, 1, 1, 1, 1, 1, 0}
    };

    private final int sampleRate;
    private int dutyRegister;
    private int envelopeRegister;
    private int frequencyLow;
    private int frequencyHigh;
    private boolean active;
    private int volume;
    private double phase;

    SquareChannel(int sampleRate) {
        this.sampleRate = sampleRate;
        reset();
    }

    void reset() {
        dutyRegister = 0;
        envelopeRegister = 0;
        frequencyLow = 0;
        frequencyHigh = 0;
        active = false;
        volume = 0;
        phase = 0.0;
    }

    void writeDuty(int value) {
        dutyRegister = value & 0xFF;
    }

    void writeEnvelope(int value) {
        envelopeRegister = value & 0xFF;
        volume = (envelopeRegister >>> 4) & 0x0F;
        if ((envelopeRegister & 0xF8) == 0) {
            active = false;
        }
    }

    void writeFrequencyLow(int value) {
        frequencyLow = value & 0xFF;
    }

    void writeFrequencyHigh(int value) {
        frequencyHigh = value & 0xFF;
        if ((frequencyHigh & 0x80) != 0) {
            trigger();
        }
    }

    boolean isActive() {
        return active;
    }

    void tick() {
    }

    double render() {
        if (!active || volume == 0) {
            return 0.0;
        }

        int duty = (dutyRegister >>> 6) & 0x03;
        int dutyStep = (int) phase & 0x07;
        int patternValue = DUTY_PATTERNS[duty][dutyStep];
        double sample = patternValue == 0 ? -1.0 : 1.0;

        phase += frequencyHertz() * 8.0 / sampleRate;
        phase %= 8.0;

        return sample * (volume / 15.0);
    }

    private void trigger() {
        active = (envelopeRegister & 0xF8) != 0;
        volume = (envelopeRegister >>> 4) & 0x0F;
        phase = 0.0;
    }

    private double frequencyHertz() {
        int frequency = frequencyLow | ((frequencyHigh & 0x07) << 8);
        int denominator = 2048 - frequency;
        if (denominator <= 0) {
            return 0.0;
        }
        return 131_072.0 / denominator;
    }
}
