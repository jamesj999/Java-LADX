package linksawakening.audio.apu;

final class WaveChannel {
    private final int sampleRate;
    private final int[] waveRam = new int[16];
    private int dacRegister;
    private int outputLevelRegister;
    private int frequencyLow;
    private int frequencyHigh;
    private boolean active;
    private double position;

    WaveChannel(int sampleRate) {
        this.sampleRate = sampleRate;
        reset();
    }

    void reset() {
        for (int i = 0; i < waveRam.length; i++) {
            waveRam[i] = 0;
        }
        dacRegister = 0;
        outputLevelRegister = 0;
        frequencyLow = 0;
        frequencyHigh = 0;
        active = false;
        position = 0.0;
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
    }

    double render() {
        if (!active) {
            return 0.0;
        }

        int sample = sampleAt((int) position & 0x1F);
        double centered = (sample - 7.5) / 7.5;
        double scaled = centered * outputScale();

        position += frequencyHertz() * 32.0 / sampleRate;
        position %= 32.0;

        return scaled;
    }

    private void trigger() {
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

    private double outputScale() {
        return switch ((outputLevelRegister >>> 5) & 0x03) {
            case 0 -> 0.0;
            case 1 -> 1.0;
            case 2 -> 0.5;
            case 3 -> 0.25;
            default -> throw new IllegalStateException("Unexpected wave output level");
        };
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
