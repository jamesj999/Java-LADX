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
    private final MusicCatalog catalog;
    private MusicTrack pendingTrack;
    private boolean trackChangePending;
    private int musicBank = 0x1B;
    private int warpVolume = -1;
    private int menuVolume = 0x77;
    private Runnable effectSilencedMusic = () -> {};
    private java.util.function.IntConsumer effectRequestedMusic;

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
        catalog = MusicCatalog.fromRom(rom);
        effectRequestedMusic = id -> play(catalog.requireTrack(id));
        music.setSoundEffectState(effects::ownsChannel, effects::takeWaveRestoreRequest);
    }

    @Override
    public void play(MusicTrack track) {
        pendingTrack = Objects.requireNonNull(track);
        trackChangePending = true;
    }

    void onEffectMusicRequest(Runnable onSilence, java.util.function.IntConsumer onTrack) {
        effectSilencedMusic = onSilence;
        effectRequestedMusic = onTrack;
    }

    @Override
    public void setWarpVolume(int level) {
        warpVolume = level;
    }

    @Override
    public void setMenuAttenuated(boolean attenuated) {
        menuVolume = attenuated ? 0x33 : 0x77;
    }

    /** Observes the mixed music/SFX register writes without changing audio routing. */
    public void setRegisterObserver(linksawakening.audio.apu.RegisterObserver observer) {
        apu.setPrimitiveRegisterObserver(observer);
    }

    public void playSoundEffect(SoundEffect effect) {
        effects.request(effect);
    }

    public void requestSoundEffect(linksawakening.audio.sfx.SoundEffectNamespace namespace, int id) {
        if (id > 0 && id <= 0xFF) effects.catalog().find(namespace, id).ifPresent(effects::request);
    }

    @Override
    public void update() {
        update(true);
    }

    @Override
    public void update(boolean advanceDriver) {
        if (advanceDriver) tickDriver();
        remainder += sampleRate;
        int frames = remainder / 60;
        remainder %= 60;
        short[] pcm = output instanceof ReusableFramePcmOutput
                ? (frames == sampleRate / 60 ? wholeFramePcm : extraFramePcm)
                : new short[frames * 2];
        apu.renderInto(pcm, 0, frames);
        output.playFrame(pcm, sampleRate);
    }

    private void tickDriver() {
        int requestedTrack = effects.advanceSharedFrame();
        if (effects.silencedMusic()) {
            music.clearScore();
            if (!trackChangePending) effectSilencedMusic.run();
        }
        if (requestedTrack != 0) effectRequestedMusic.accept(requestedTrack);
        if (trackChangePending) {
            if (pendingTrack != null) musicBank = pendingTrack.bank();
            effects.changeMusicTrack(musicBank, pendingTrack == null);
            if (pendingTrack == null) music.clearScore();
            else music.startShared(pendingTrack);
            trackChangePending = false;
        }
        music.tick60Hz();
        int volume = warpVolume < 0 ? menuVolume : warpVolume * 0x11;
        if (apu.readRegister(GameBoyApu.NR50) != volume) {
            apu.writeRegister(GameBoyApu.NR50, volume);
        }
    }

    @Override
    public boolean isMusicPlaying() {
        return trackChangePending ? pendingTrack != null : music.isPlaying();
    }

    @Override
    public void stop() {
        pendingTrack = null;
        trackChangePending = true;
    }

    @Override
    public void close() {
        effects.stop();
        music.stop();
        pendingTrack = null;
        trackChangePending = false;
    }
}
