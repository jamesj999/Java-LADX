package linksawakening.audio.apu;

final class NoiseChannel {
    private static final double GAME_BOY_CLOCK_HZ = 4_194_304.0;
    private static final int[] DIVIDING_RATIOS = { 4, 8, 16, 24, 32, 40, 48, 56 };
    private static final double ENVELOPE_CLOCK_HZ = 64.0;

    private final int sampleRate;
    private int lengthRegister;
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
        reset();
    }

    void reset() {
        lengthRegister = 0;
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
        lengthRegister = value & 0xFF;
    }

    void writeEnvelope(int value) {
        envelopeRegister = value & 0xFF;
        volume = (envelopeRegister >>> 4) & 0x0F;
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
        if ((envelopeRegister & 0x08) != 0) {
            if (volume < 0x0F) {
                volume++;
            }
        } else if (volume > 0) {
            volume--;
        }
    }

    double render() {
        if (!active || volume == 0) {
            return 0.0;
        }

        double sample = ((lfsr & 0x01) == 0 ? 1.0 : -1.0) * (volume / 15.0);
        advanceForOneOutputSample();
        return sample;
    }

    private void trigger() {
        active = (envelopeRegister & 0xF8) != 0;
        volume = (envelopeRegister >>> 4) & 0x0F;
        lfsr = 0x7FFF;
        lfsrStepAccumulator = 0.0;
        envelopeClockAccumulator = 0.0;
    }

    private void advanceForOneOutputSample() {
        lfsrStepAccumulator += lfsrStepsPerSample();
        while (lfsrStepAccumulator >= 1.0) {
            lfsrStepAccumulator -= 1.0;
            advanceLfsr();
        }
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
        double lfsrClockHz = GAME_BOY_CLOCK_HZ / ratio / (1 << shift);
        return lfsrClockHz / sampleRate;
    }
}
