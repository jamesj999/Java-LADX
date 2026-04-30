package linksawakening.audio.sfx;

import linksawakening.audio.apu.GameBoyApu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class SoundEffectSequenceLibrary {
    private SoundEffectSequenceLibrary() {
    }

    static Map<SequenceKey, SoundEffectSequence> firstPassSequences(SoundEffectCatalog catalog) {
        return Map.ofEntries(
                sequence(catalog, SoundEffectNamespace.NOISE, 0x02, List.of(
                        // NOISE_SFX_SWORD_SWING_A: Data_01F_6564, then Data_01F_6569..6582.
                        noise(0x00, 0x40, 0x21, 0x80, 1),
                        noise(0x00, 0x50, 0x22, 0x80, 1),
                        noise(0x00, 0x60, 0x23, 0x80, 1),
                        noise(0x00, 0x70, 0x24, 0x80, 1),
                        noise(0x00, 0x80, 0x25, 0x80, 1),
                        noise(0x00, 0x90, 0x26, 0x80, 1),
                        noise(0x3C, 0xA0, 0x27, 0xC0, 1))),
                sequence(catalog, SoundEffectNamespace.NOISE, 0x05, List.of(
                        // NOISE_SFX_CUT_GRASS: Data_01F_6659, then Data_01F_665E..6668.
                        noise(0x30, 0x49, 0x60, 0xC0, 6),
                        noise(0x30, 0x49, 0x40, 0xC0, 6),
                        noise(0x30, 0x49, 0x20, 0xC0, 6),
                        noise(0x30, 0x49, 0x00, 0xC0, 6))),
                sequence(catalog, SoundEffectNamespace.NOISE, 0x09, List.of(
                        // NOISE_SFX_POT_SMASHED: Data_01F_674F, then Data_01F_6754..675D.
                        noise(0x33, 0xA0, 0x72, 0xC0, 4),
                        noise(0x33, 0xA0, 0x52, 0xC0, 4),
                        noise(0x33, 0xA0, 0x50, 0xC0, 4),
                        noise(0x33, 0xA0, 0x30, 0xC0, 4),
                        noise(0x33, 0xA0, 0x10, 0xC0, 4))),
                sequence(catalog, SoundEffectNamespace.NOISE, 0x0C, List.of(
                        // NOISE_SFX_EXPLOSION: Data_01F_67E4, then Data_01F_67E9..67F2.
                        noise(0x00, 0xC6, 0x6A, 0x80, 4),
                        noiseFrequency(0x6B, 0x00, 5),
                        noiseFrequency(0x6C, 0x00, 6),
                        noiseFrequency(0x6D, 0x00, 7),
                        noiseFrequency(0x6E, 0x00, 0x38))),
                sequence(catalog, SoundEffectNamespace.WAVE, 0x03, List.of(
                        // WAVE_SFX_LINK_HURT: wave pattern Data_01F_63CC, Data_01F_555E, Data_01F_5558.
                        wave(0x80, 0xEF, 0x20, 0x00, 0xC6, 1, wavePattern63Cc()),
                        wave(0x80, 0xEF, 0x20, 0x80, 0xC6, 1),
                        wave(0x80, 0xEF, 0x20, 0xF0, 0xC6, 1),
                        wave(0x00, 0xEF, 0x00, 0x00, 0x00, 1))),
                sequence(catalog, SoundEffectNamespace.WAVE, 0x04, List.of(
                        // WAVE_SFX_LOW_HEARTS: Data_01F_55A4, Data_01F_55AA, Data_01F_55AD.
                        wave(0x80, 0xFA, 0x40, 0xC0, 0xC7, 4, wavePattern63Cc()),
                        wave(0x80, 0xFA, 0x40, 0xE0, 0xC7, 4),
                        wave(0x80, 0xFA, 0x60, 0xC0, 0xC7, 4),
                        wave(0x00, 0xFA, 0x00, 0x00, 0x00, 1))),
                sequence(catalog, SoundEffectNamespace.WAVE, 0x05, List.of(
                        // WAVE_SFX_RUPEE: Data_01F_55F0, Data_01F_55F3, Data_01F_55F6.
                        wave(0x80, 0xFA, 0x20, 0xDA, 0xC7, 2, wavePattern639C()),
                        waveFrequency(0xEA, 0xC7, 2),
                        waveFrequency(0xDA, 0xC7, 2, 0x40),
                        waveFrequency(0xEA, 0xC7, 2),
                        waveFrequency(0xDA, 0xC7, 2, 0x60),
                        waveFrequency(0xEA, 0xC7, 2),
                        wave(0x00, 0xFA, 0x00, 0x00, 0x00, 1))),
                sequence(catalog, SoundEffectNamespace.WAVE, 0x0F, List.of(
                        // WAVE_SFX_TEXT_PRINT: Data_01F_5A38, Data_01F_5A3E.
                        wave(0x80, 0xFB, 0x60, 0xD2, 0xC7, 1, wavePattern63Cc()),
                        wave(0x80, 0xFB, 0x40, 0xD2, 0xC7, 2),
                        wave(0x00, 0xFB, 0x00, 0x00, 0x00, 1))));
    }

    private static Map.Entry<SequenceKey, SoundEffectSequence> sequence(
            SoundEffectCatalog catalog,
            SoundEffectNamespace namespace,
            int id,
            List<SoundEffectStep> steps) {
        SoundEffect effect = catalog.find(namespace, id).orElseThrow();
        return Map.entry(new SequenceKey(namespace, id), new SoundEffectSequence(effect, steps));
    }

    private static SoundEffectStep noise(int nr41, int nr42, int nr43, int nr44, int durationFrames) {
        return SoundEffectStep.registers(
                durationFrames,
                new ApuRegisterWrite(GameBoyApu.NR41, nr41),
                new ApuRegisterWrite(GameBoyApu.NR42, nr42),
                new ApuRegisterWrite(GameBoyApu.NR43, nr43),
                new ApuRegisterWrite(GameBoyApu.NR44, nr44 | 0x80));
    }

    private static SoundEffectStep noiseFrequency(int nr43, int nr44, int durationFrames) {
        return SoundEffectStep.registers(
                durationFrames,
                new ApuRegisterWrite(GameBoyApu.NR43, nr43),
                new ApuRegisterWrite(GameBoyApu.NR44, nr44));
    }

    private static SoundEffectStep wave(
            int nr30,
            int nr31,
            int nr32,
            int nr33,
            int nr34,
            int durationFrames,
            List<WaveRamWrite> waveRamWrites) {
        return SoundEffectStep.waveAndRegisters(
                durationFrames,
                waveRamWrites,
                new ApuRegisterWrite(GameBoyApu.NR30, nr30),
                new ApuRegisterWrite(GameBoyApu.NR31, nr31),
                new ApuRegisterWrite(GameBoyApu.NR32, nr32),
                new ApuRegisterWrite(GameBoyApu.NR33, nr33),
                new ApuRegisterWrite(GameBoyApu.NR34, nr34 | 0x80));
    }

    private static SoundEffectStep wave(int nr30, int nr31, int nr32, int nr33, int nr34, int durationFrames) {
        return wave(nr30, nr31, nr32, nr33, nr34, durationFrames, List.of());
    }

    private static SoundEffectStep waveFrequency(int nr33, int nr34, int durationFrames) {
        return SoundEffectStep.registers(
                durationFrames,
                new ApuRegisterWrite(GameBoyApu.NR33, nr33),
                new ApuRegisterWrite(GameBoyApu.NR34, nr34));
    }

    private static SoundEffectStep waveFrequency(int nr33, int nr34, int durationFrames, int nr32) {
        return SoundEffectStep.registers(
                durationFrames,
                new ApuRegisterWrite(GameBoyApu.NR32, nr32),
                new ApuRegisterWrite(GameBoyApu.NR33, nr33),
                new ApuRegisterWrite(GameBoyApu.NR34, nr34));
    }

    private static List<WaveRamWrite> wavePattern63Cc() {
        int[] bytes = {
                0xFF, 0xFF, 0xEE, 0xDD, 0xEE, 0xDD, 0xEE, 0xFF,
                0xFF, 0xC9, 0x63, 0x21, 0x00, 0x00, 0x04, 0x8C
        };
        return wavePattern(bytes);
    }

    private static List<WaveRamWrite> wavePattern639C() {
        int[] bytes = {
                0x00, 0x22, 0x44, 0x66, 0x88, 0xAA, 0xCC, 0xCC,
                0x00, 0x22, 0x44, 0x66, 0x88, 0xAA, 0xCC, 0xCC
        };
        return wavePattern(bytes);
    }

    private static List<WaveRamWrite> wavePattern(int[] bytes) {
        List<WaveRamWrite> writes = new ArrayList<>(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            writes.add(new WaveRamWrite(i, bytes[i]));
        }
        return writes;
    }

    record SequenceKey(SoundEffectNamespace namespace, int id) {
        SequenceKey {
            Objects.requireNonNull(namespace, "namespace");
            id &= 0xFF;
        }
    }
}
