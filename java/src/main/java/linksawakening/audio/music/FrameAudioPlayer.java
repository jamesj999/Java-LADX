package linksawakening.audio.music;

import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.sfx.SoundEffect;
import linksawakening.audio.sfx.SoundEffectPlayer;
import linksawakening.gameplay.PcmSoundOutput;
import java.util.Objects;

/**
 * Device-independent audio transport. Each update is exactly one gameplay frame.
 * Music, SFX, and externally rendered dialog PCM use the same injected output.
 * The caller owns that output and advances this transport at 60 Hz.
 */
public final class FrameAudioPlayer implements FrameMusicPlayer {
    private final GameBoyApu apu;
    private final MusicDriver music;
    private final SoundEffectPlayer effects;
    private final PcmSoundOutput output;
    private final int sampleRate;
    private int remainder;

    public FrameAudioPlayer(byte[] rom, int sampleRate, PcmSoundOutput output) {
        if (sampleRate < 60) throw new IllegalArgumentException("sampleRate must be at least 60");
        this.sampleRate = sampleRate;
        this.output = Objects.requireNonNull(output, "output");
        apu = new GameBoyApu(sampleRate);
        music = new MusicDriver(rom, apu);
        effects = new SoundEffectPlayer(apu, rom);
    }

    @Override public void play(MusicTrack track) {
        apu.reset();
        effects.stop();
        music.start(track);
        music.tick60Hz();
    }

    public void playSoundEffect(SoundEffect effect) { effects.play(effect); }

    /** Dialog synthesis remains external; transport and device selection are shared. */
    public void playDialogPcm(short[] stereoPcm, int rate) {
        output.play(stereoPcm, rate);
    }

    @Override public void update() {
        music.tick60Hz();
        effects.tick60Hz();
        remainder += sampleRate;
        int frames = remainder / 60;
        remainder %= 60;
        output.play(apu.render(frames), sampleRate);
    }

    @Override public boolean isMusicPlaying() { return music.isPlaying(); }
    @Override public void stop() { music.stop(); effects.stop(); }
    @Override public void close() { stop(); }
}
