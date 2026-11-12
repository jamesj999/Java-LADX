package linksawakening.audio.apu;

final class NoiseChannel {
    private static final double GAME_BOY_CLOCK_HZ = 4_194_304.0;
    private static final int[] DIVIDING_RATIOS = {8, 16, 32, 48, 64, 80, 96, 112};
    private static final double ENVELOPE_CLOCK_HZ = 64.0;

    private final int sampleRate;
    private final LengthCounter length;
    private int envelopeRegister;
    private int polynomialRegister;
    private int controlRegister;
    private boolean active;
    private int volume;
    private int lfsr;
    private double lfsrStepAccumulator;
    private double envelopeClockAccumulator;

    NoiseChannel(int sampleRate) {
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("sampleRate must be positive");
        }
        this.sampleRate = sampleRate;
        length = new LengthCounter(64, sampleRate);
        reset();
    }

    void reset() {
        length.reset();
        envelopeRegister = 0;
        polynomialRegister = 0;
        controlRegister = 0;
        active = false;
        volume = 0;
        lfsr = 0x7FFF;
        lfsrStepAccumulator = 0.0;
        envelopeClockAccumulator = 0.0;
    }

    void writeLength(int value) {
        length.load(value);
    }

    void writeEnvelope(int value) {
        envelopeRegister = value & 0xFF;
        // NR42 stores the initial volume; NR44 trigger reloads the live envelope.
        if ((envelopeRegister & 0xF8) == 0) {
            active = false;
        }
    }

    void writePolynomial(int value) {
        polynomialRegister = value & 0xFF;
    }

    void writeControl(int value) {
        controlRegister = value & 0xFF;
        if ((controlRegister & 0x80) != 0) {
            trigger();
        }
    }

    boolean isActive() {
        return active;
    }

    void tick() {
        if (length.tick((controlRegister & 0x40) != 0)) {
            active = false;
        }
        if (!active) {
            return;
        }

        int envelopePeriod = envelopeRegister & 0x07;
        if (envelopePeriod == 0) {
            return;
        }

        envelopeClockAccumulator += ENVELOPE_CLOCK_HZ / sampleRate;
        if (envelopeClockAccumulator < envelopePeriod) {
            return;
        }

        envelopeClockAccumulator -= envelopePeriod;
        volume = EnvelopeStep.next(volume, envelopeRegister);
    }

    double render() {
        if (!dacEnabled()) return 0.0;
        if (!active) return 1.0;
        double steps = lfsrStepsPerSample();
        if (steps == 0) return digitalOutput();
        double remaining = steps;
        double sum = 0;
        while (remaining > 0) {
            double span = Math.min(remaining, 1.0 - lfsrStepAccumulator);
            sum += digitalOutput() * span;
            lfsrStepAccumulator += span;
            remaining -= span;
            if (lfsrStepAccumulator >= 1.0) {
                lfsrStepAccumulator = 0;
                advanceLfsr();
            }
        }
        return sum / steps;
    }

    private double digitalOutput() {
        return 1.0 - ((lfsr & 1) == 0 ? volume / 7.5 : 0);
    }

    boolean dacEnabled() {
        return (envelopeRegister & 0xF8) != 0;
    }

    private void trigger() {
        active = (envelopeRegister & 0xF8) != 0;
        volume = (envelopeRegister >>> 4) & 0x0F;
        length.trigger();
        lfsr = 0x7FFF;
        lfsrStepAccumulator = 0.0;
        envelopeClockAccumulator = 0.0;
    }

    private void advanceLfsr() {
        int feedback = (lfsr ^ (lfsr >>> 1)) & 0x01;
        lfsr = (lfsr >>> 1) | (feedback << 14);
        if ((polynomialRegister & 0x08) != 0) {
            lfsr = (lfsr & ~(1 << 6)) | (feedback << 6);
        }
    }

    private double lfsrStepsPerSample() {
        int divisorCode = polynomialRegister & 0x07;
        int ratio = DIVIDING_RATIOS[divisorCode];
        int shift = (polynomialRegister >>> 4) & 0x0F;
        if (shift >= 14) {
            return 0.0;
        }
        double lfsrClockHz = GAME_BOY_CLOCK_HZ / ratio / (1 << shift);
        return lfsrClockHz / sampleRate;
    }
}
