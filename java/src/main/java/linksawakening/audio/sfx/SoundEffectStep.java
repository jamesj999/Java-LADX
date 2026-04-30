package linksawakening.audio.sfx;

import java.util.List;
import java.util.Objects;

public record SoundEffectStep(
        int durationFrames,
        List<ApuRegisterWrite> registerWrites,
        List<WaveRamWrite> waveRamWrites) {
    public SoundEffectStep {
        if (durationFrames <= 0) {
            throw new IllegalArgumentException("durationFrames must be positive");
        }
        registerWrites = List.copyOf(Objects.requireNonNull(registerWrites, "registerWrites"));
        waveRamWrites = List.copyOf(Objects.requireNonNull(waveRamWrites, "waveRamWrites"));
    }

    public static SoundEffectStep registers(int durationFrames, ApuRegisterWrite... writes) {
        return new SoundEffectStep(durationFrames, List.of(writes), List.of());
    }

    public static SoundEffectStep waveAndRegisters(
            int durationFrames,
            List<WaveRamWrite> waveRamWrites,
            ApuRegisterWrite... writes) {
        return new SoundEffectStep(durationFrames, List.of(writes), waveRamWrites);
    }
}
