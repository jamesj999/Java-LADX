package linksawakening.audio.music;

import java.util.Objects;
import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.sfx.SoundEffect;
import linksawakening.audio.sfx.SoundEffectPlayer;
import linksawakening.gameplay.PcmSoundOutput;
import linksawakening.rom.Rom;

/**
 * Device-independent audio transport. Each update is exactly one gameplay frame.
 * Music and SFX use the same injected output.
 * The caller owns that output and advances this transport at 60 Hz.
 */
public final class FrameAudioPlayer implements FrameMusicPlayer {
    private final GameBoyApu apu;
    private final MusicDriver music;
    private final SoundEffectPlayer effects;
    private final PcmSoundOutput output;
    private final int sampleRate;
    private int remainder;
    private final short[] wholeFramePcm;
    private final short[] extraFramePcm;

    public FrameAudioPlayer(Rom rom, int sampleRate, PcmSoundOutput output) {
        if (sampleRate < 60) throw new IllegalArgumentException("sampleRate must be at least 60");
        this.sampleRate = sampleRate;
        wholeFramePcm = new short[(sampleRate / 60) * 2];
        extraFramePcm = sampleRate % 60 == 0 ? wholeFramePcm : new short[(sampleRate / 60 + 1) * 2];
        this.output = Objects.requireNonNull(output, "output");
        apu = new GameBoyApu(sampleRate);
        music = new MusicDriver(rom, apu);
        effects = new SoundEffectPlayer(apu, rom);
        music.setSoundEffectState(effects::ownsChannel, effects::takeWaveRestoreRequest);
    }

    @Override
    public void play(MusicTrack track) {
        apu.reset();
        effects.stop();
        music.start(track);
        music.tick60Hz();
    }

    public void playSoundEffect(SoundEffect effect) {
        effects.play(effect);
    }

    @Override
    public void update() {
        music.tick60Hz();
        effects.tick60Hz();
        remainder += sampleRate;
        int frames = remainder / 60;
        remainder %= 60;
        if (output instanceof ReusableFramePcmOutput) {
            short[] pcm = frames == sampleRate / 60 ? wholeFramePcm : extraFramePcm;
            apu.renderInto(pcm, 0, frames);
            output.playFrame(pcm, sampleRate);
        } else {
            short[] pcm = new short[frames * 2];
            apu.renderInto(pcm, 0, frames);
            output.playFrame(pcm, sampleRate);
        }
    }

    @Override
    public boolean isMusicPlaying() {
        return music.isPlaying();
    }

    @Override
    public void stop() {
        music.stop();
        effects.stop();
    }

    @Override
    public void close() {
        stop();
    }
}
