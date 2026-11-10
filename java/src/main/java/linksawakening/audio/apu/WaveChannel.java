package linksawakening.audio.apu;

final class WaveChannel {
    private final int sampleRate;
    private final LengthCounter length;
    private final int[] waveRam = new int[16];
    private int dacRegister;
    private int outputLevelRegister;
    private int frequencyLow;
    private int frequencyHigh;
    private boolean active;
    private double position;

    WaveChannel(int sampleRate) {
        this.sampleRate = sampleRate;
        length = new LengthCounter(256, sampleRate);
        reset();
    }

    void reset() {
        for (int i = 0; i < waveRam.length; i++) {
            waveRam[i] = 0;
        }
        powerOff();
    }

    // NR52 power-off resets the channel but leaves wave RAM intact.
    void powerOff() {
        length.reset();
        dacRegister = 0;
        outputLevelRegister = 0;
        frequencyLow = 0;
        frequencyHigh = 0;
        active = false;
        position = 0.0;
    }

    void writeLength(int value) {
        length.load(value);
    }

    void writeDac(int value) {
        dacRegister = value & 0xFF;
        if ((dacRegister & 0x80) == 0) {
            active = false;
        }
    }

    void writeOutputLevel(int value) {
        outputLevelRegister = value & 0xFF;
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

    void writeWaveRam(int index, int value) {
        if (index < 0 || index >= waveRam.length) {
            throw new IllegalArgumentException("Wave RAM index out of range: " + index);
        }
        waveRam[index] = value & 0xFF;
    }

    int readWaveRam(int index) {
        if (index < 0 || index >= waveRam.length) {
            throw new IllegalArgumentException("Wave RAM index out of range: " + index);
        }
        return waveRam[index];
    }

    boolean isActive() {
        return active;
    }

    void tick() {
        if (length.tick((frequencyHigh & 0x40) != 0)) {
            active = false;
        }
    }

    double render() {
        if (!dacEnabled()) return 0.0;
        if (!active) return 1.0;
        int level = (outputLevelRegister >>> 5) & 3;
        double steps = frequencyHertz() * 32.0 / sampleRate;
        double remaining = steps;
        double digitalSum = 0;
        while (remaining > 0) {
            double span = Math.min(remaining, 1.0 - (position - Math.floor(position)));
            int digital = level == 0 ? 0 : sampleAt((int) position & 31) >>> (level - 1);
            digitalSum += digital * span;
            position = (position + span) % 32.0;
            remaining -= span;
        }
        return 1.0 - digitalSum / steps / 7.5;
    }

    boolean dacEnabled() { return (dacRegister & 0x80) != 0; }

    private void trigger() {
        length.trigger();
        active = (dacRegister & 0x80) != 0;
        position = 0.0;
    }

    private int sampleAt(int sampleIndex) {
        int value = waveRam[sampleIndex >>> 1];
        if ((sampleIndex & 1) == 0) {
            return (value >>> 4) & 0x0F;
        }
        return value & 0x0F;
    }

    private double frequencyHertz() {
        int frequency = frequencyLow | ((frequencyHigh & 0x07) << 8);
        int denominator = 2048 - frequency;
        if (denominator <= 0) {
            return 0.0;
        }
        return 65_536.0 / denominator;
    }
}
