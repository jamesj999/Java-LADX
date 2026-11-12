package linksawakening.audio.apu;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;

/** Deterministic register-driven Game Boy stereo synthesizer. */
public final class GameBoyApu {
    public static final int NR10 = 0x10;
    public static final int NR11 = 0x11;
    public static final int NR12 = 0x12;
    public static final int NR13 = 0x13;
    public static final int NR14 = 0x14;
    public static final int NR21 = 0x16;
    public static final int NR22 = 0x17;
    public static final int NR23 = 0x18;
    public static final int NR24 = 0x19;
    public static final int NR30 = 0x1A;
    public static final int NR31 = 0x1B;
    public static final int NR32 = 0x1C;
    public static final int NR33 = 0x1D;
    public static final int NR34 = 0x1E;
    public static final int NR41 = 0x20;
    public static final int NR42 = 0x21;
    public static final int NR43 = 0x22;
    public static final int NR44 = 0x23;
    public static final int NR50 = 0x24;
    public static final int NR51 = 0x25;
    public static final int NR52 = 0x26;

    private static final int REGISTER_COUNT = NR52 + 1;
    private static final double MIX_AMPLITUDE = 4096.0;

    // CGB capacitor charge factor at the 4,194,304 Hz APU clock (Pan Docs,
    // Audio_details.md, "Obscure Behavior"). Convert it to the output sample rate.
    private final double capacitorCharge;
    private double leftCapacitor;
    private double rightCapacitor;
    private final int sampleRate;
    private final int[] registers = new int[REGISTER_COUNT];
    private final SquareChannel channel1;
    private final SquareChannel channel2;
    private final WaveChannel channel3;
    private final NoiseChannel channel4;

    public GameBoyApu(int sampleRate) {
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("sampleRate must be positive");
        }
        this.sampleRate = sampleRate;
        capacitorCharge = Math.pow(0.998943, 4_194_304.0 / sampleRate);
        channel1 = new SquareChannel(sampleRate);
        channel2 = new SquareChannel(sampleRate);
        channel3 = new WaveChannel(sampleRate);
        channel4 = new NoiseChannel(sampleRate);
        reset();
    }

    public void reset() {
        leftCapacitor = rightCapacitor = 0;
        for (int i = 0; i < registers.length; i++) {
            registers[i] = 0;
        }
        channel1.reset();
        channel2.reset();
        channel3.reset();
        channel4.reset();
    }

    public int sampleRate() {
        return sampleRate;
    }

    private RegisterObserver registerObserver;

    /** Optional diagnostic tap; observes writes without changing synthesis. */
    public void setRegisterObserver(BiConsumer<Integer, Integer> observer) {
        Objects.requireNonNull(observer);
        registerObserver = observer::accept;
    }

    /** Primitive diagnostic tap; null disables observation. */
    public void setPrimitiveRegisterObserver(RegisterObserver observer) {
        registerObserver = observer;
    }

    public void writeRegister(int register, int value) {
        validateRegister(register);
        int unsignedValue = value & 0xFF;
        if (registerObserver != null) registerObserver.accept(register, unsignedValue);
        if (register == NR52) {
            if ((unsignedValue & 0x80) == 0) {
                Arrays.fill(registers, 0);
                channel1.reset();
                channel2.reset();
                channel3.powerOff();
                channel4.reset();
            } else {
                registers[NR52] = 0x80;
            }
            return;
        }
        // CGB ignores all sound-register writes while powered off.
        if (!masterEnabled()) {
            return;
        }
        registers[register] = unsignedValue;

        switch (register) {
            case NR10 -> channel1.writeSweep(unsignedValue);
            case NR11 -> channel1.writeDuty(unsignedValue);
            case NR12 -> channel1.writeEnvelope(unsignedValue);
            case NR13 -> channel1.writeFrequencyLow(unsignedValue);
            case NR14 -> channel1.writeFrequencyHigh(unsignedValue);
            case NR21 -> channel2.writeDuty(unsignedValue);
            case NR22 -> channel2.writeEnvelope(unsignedValue);
            case NR23 -> channel2.writeFrequencyLow(unsignedValue);
            case NR24 -> channel2.writeFrequencyHigh(unsignedValue);
            case NR31 -> channel3.writeLength(unsignedValue);
            case NR30 -> channel3.writeDac(unsignedValue);
            case NR32 -> channel3.writeOutputLevel(unsignedValue);
            case NR33 -> channel3.writeFrequencyLow(unsignedValue);
            case NR34 -> channel3.writeFrequencyHigh(unsignedValue);
            case NR41 -> channel4.writeLength(unsignedValue);
            case NR42 -> channel4.writeEnvelope(unsignedValue);
            case NR43 -> channel4.writePolynomial(unsignedValue);
            case NR44 -> channel4.writeControl(unsignedValue);
            default -> {}
        }
    }

    public int readRegister(int register) {
        validateRegister(register);
        if (register == NR52) {
            return (registers[NR52] & 0x80)
                    | (channel1.isActive() ? 0x01 : 0)
                    | (channel2.isActive() ? 0x02 : 0)
                    | (channel3.isActive() ? 0x04 : 0)
                    | (channel4.isActive() ? 0x08 : 0);
        }
        return registers[register];
    }

    public void writeWaveRam(int index, int value) {
        channel3.writeWaveRam(index, value);
    }

    public int readWaveRam(int index) {
        return channel3.readWaveRam(index);
    }

    public short[] render(int frames) {
        if (frames < 0) {
            throw new IllegalArgumentException("frames must be non-negative");
        }
        short[] pcm = new short[frames * 2];
        renderInto(pcm, 0, frames);
        return pcm;
    }

    /** Renders interleaved stereo into caller-owned storage without allocation. */
    public void renderInto(short[] pcm, int sampleOffset, int frames) {
        if (frames < 0) throw new IllegalArgumentException("frames must be non-negative");
        Objects.checkFromIndexSize(sampleOffset, Math.multiplyExact(frames, 2), pcm.length);
        Arrays.fill(pcm, sampleOffset, sampleOffset + frames * 2, (short) 0);
        if (!masterEnabled()) return;

        for (int frame = 0; frame < frames; frame++) {
            channel1.tick();
            channel2.tick();
            channel3.tick();
            channel4.tick();
            double sample1 = channel1.render();
            double sample2 = channel2.render();
            double sample3 = channel3.render();
            double sample4 = channel4.render();

            double left = 0.0;
            double right = 0.0;

            if (routedToLeft(1)) {
                left += sample1;
            }
            if (routedToLeft(2)) {
                left += sample2;
            }
            if (routedToLeft(3)) {
                left += sample3;
            }
            if (routedToLeft(4)) {
                left += sample4;
            }
            if (routedToRight(1)) {
                right += sample1;
            }
            if (routedToRight(2)) {
                right += sample2;
            }
            if (routedToRight(3)) {
                right += sample3;
            }
            if (routedToRight(4)) {
                right += sample4;
            }

            // The capacitors follow NR50, persist across render calls, and disconnect
            // when all DACs are off. Four channels need at most +/-8 after the HPF;
            // the 4096 scale leaves that range representable without routine clipping.
            if (channel1.dacEnabled() || channel2.dacEnabled() || channel3.dacEnabled() || channel4.dacEnabled()) {
                left *= leftVolume();
                right *= rightVolume();
                double leftOut = left - leftCapacitor;
                double rightOut = right - rightCapacitor;
                leftCapacitor = left - leftOut * capacitorCharge;
                rightCapacitor = right - rightOut * capacitorCharge;
                pcm[sampleOffset + frame * 2] = toPcm(leftOut, 1);
                pcm[sampleOffset + frame * 2 + 1] = toPcm(rightOut, 1);
            }
        }
    }

    public boolean isChannelActive(int channel) {
        if (!masterEnabled()) {
            return false;
        }
        return switch (channel) {
            case 1 -> channel1.isActive();
            case 2 -> channel2.isActive();
            case 3 -> channel3.isActive();
            case 4 -> channel4.isActive();
            default -> throw new IllegalArgumentException("Channel out of range: " + channel);
        };
    }

    private boolean masterEnabled() {
        return (registers[NR52] & 0x80) != 0;
    }

    private boolean routedToRight(int channel) {
        return (registers[NR51] & (1 << (channel - 1))) != 0;
    }

    private boolean routedToLeft(int channel) {
        return (registers[NR51] & (1 << (channel + 3))) != 0;
    }

    private double rightVolume() {
        return ((registers[NR50] & 0x07) + 1) / 8.0;
    }

    private double leftVolume() {
        return (((registers[NR50] >>> 4) & 0x07) + 1) / 8.0;
    }

    private static short toPcm(double sample, double volume) {
        int scaled = (int) Math.round(sample * volume * MIX_AMPLITUDE);
        if (scaled > Short.MAX_VALUE) {
            return Short.MAX_VALUE;
        }
        if (scaled < Short.MIN_VALUE) {
            return Short.MIN_VALUE;
        }
        return (short) scaled;
    }

    private static void validateRegister(int register) {
        if (register < 0 || register >= REGISTER_COUNT) {
            throw new IllegalArgumentException("APU register out of range: 0x"
                    + Integer.toHexString(register).toUpperCase());
        }
    }
}
