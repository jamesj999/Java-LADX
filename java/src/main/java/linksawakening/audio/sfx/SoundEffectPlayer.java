package linksawakening.audio.sfx;

import linksawakening.audio.apu.GameBoyApu;

import java.util.Map;
import java.util.Objects;

public final class SoundEffectPlayer {
    private final GameBoyApu apu;
    private final Map<SoundEffectSequenceLibrary.SequenceKey, SoundEffectSequence> sequences;

    private SoundEffectSequence activeSequence;
    private int stepIndex;
    private int framesUntilNextStep;

    public SoundEffectPlayer(GameBoyApu apu) {
        this(apu, SoundEffectCatalog.firstPass());
    }

    public SoundEffectPlayer(GameBoyApu apu, SoundEffectCatalog catalog) {
        this.apu = Objects.requireNonNull(apu, "apu");
        sequences = SoundEffectSequenceLibrary.firstPassSequences(Objects.requireNonNull(catalog, "catalog"));
    }

    public void play(SoundEffect effect) {
        Objects.requireNonNull(effect, "effect");
        SoundEffectSequence sequence = sequences.get(new SoundEffectSequenceLibrary.SequenceKey(
                effect.namespace(),
                effect.id()));
        if (sequence == null) {
            throw new IllegalArgumentException("Unsupported sound effect: " + effect.name());
        }

        activeSequence = sequence;
        stepIndex = 0;
        framesUntilNextStep = 0;
        enableApu();
        startNextStep();
    }

    public void tick60Hz() {
        if (!isPlaying()) {
            return;
        }
        framesUntilNextStep--;
        if (framesUntilNextStep <= 0) {
            startNextStep();
        }
    }

    public boolean isPlaying() {
        return activeSequence != null;
    }

    public SoundEffect currentEffect() {
        return activeSequence == null ? null : activeSequence.effect();
    }

    public void stop() {
        activeSequence = null;
        stepIndex = 0;
        framesUntilNextStep = 0;
    }

    private void enableApu() {
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0xFF);
    }

    private void startNextStep() {
        if (activeSequence == null || stepIndex >= activeSequence.steps().size()) {
            stop();
            return;
        }

        SoundEffectStep step = activeSequence.steps().get(stepIndex);
        stepIndex++;
        for (WaveRamWrite write : step.waveRamWrites()) {
            apu.writeWaveRam(write.index(), write.value());
        }
        for (ApuRegisterWrite write : step.registerWrites()) {
            apu.writeRegister(write.register(), write.value());
        }
        framesUntilNextStep = step.durationFrames();
    }
}
