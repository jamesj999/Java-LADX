package linksawakening.audio.apu;

final class NoiseChannel {
    private int lengthRegister;
    private int envelopeRegister;
    private int polynomialRegister;
    private int controlRegister;
    private boolean active;
    private int volume;
    private int lfsr;
    private int periodCounter;

    void reset() {
        lengthRegister = 0;
        envelopeRegister = 0;
        polynomialRegister = 0;
        controlRegister = 0;
        active = false;
        volume = 0;
        lfsr = 0x7FFF;
        periodCounter = 0;
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
    }

    double render() {
        if (!active || volume == 0) {
            return 0.0;
        }

        double sample = ((lfsr & 0x01) == 0 ? 1.0 : -1.0) * (volume / 15.0);
        advance();
        return sample;
    }

    private void trigger() {
        active = (envelopeRegister & 0xF8) != 0;
        volume = (envelopeRegister >>> 4) & 0x0F;
        lfsr = 0x7FFF;
        periodCounter = 0;
    }

    private void advance() {
        periodCounter++;
        if (periodCounter < periodSamples()) {
            return;
        }
        periodCounter = 0;

        int feedback = (lfsr ^ (lfsr >>> 1)) & 0x01;
        lfsr = (lfsr >>> 1) | (feedback << 14);
        if ((polynomialRegister & 0x08) != 0) {
            lfsr = (lfsr & ~(1 << 6)) | (feedback << 6);
        }
    }

    private int periodSamples() {
        int divisorCode = polynomialRegister & 0x07;
        int base = divisorCode == 0 ? 1 : divisorCode * 2;
        int shift = (polynomialRegister >>> 4) & 0x0F;
        if (shift >= 8) {
            return 512;
        }
        return Math.max(1, base << shift);
    }
}
