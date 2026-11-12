package linksawakening.audio.music;

import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.sfx.SoundEffectPlayer;

/** Sample-clock transport for the browser, independent of native audio. */
public final class FrameAudioMixer {
    private static final int BUFFER_FRAMES = 1024;
    private final MusicDriver driver;
    private final GameBoyApu apu;
    private final SoundEffectPlayer soundEffectPlayer;
    private final int sampleRate;
    private int samplesUntilNextTick;
    private int tickSampleRemainder;
    private boolean loopEnabled;
    private MusicTrack currentTrack;

    public FrameAudioMixer(MusicDriver driver, GameBoyApu apu, SoundEffectPlayer effects) {
        this.driver = driver;
        this.apu = apu;
        soundEffectPlayer = effects;
        sampleRate = apu.sampleRate();
        resetSampleClock();
    }

    public void setTrack(MusicTrack track) {
        currentTrack = track;
    }

    public void setLoopEnabled(boolean enabled) {
        loopEnabled = enabled;
    }

    public short[] renderBufferPcm() {
        short[] pcm = new short[BUFFER_FRAMES * 2];
        int framesRendered = 0;

        while (framesRendered < BUFFER_FRAMES && ensureAudioPlaying()) {
            int chunkFrames = Math.min(BUFFER_FRAMES - framesRendered, samplesUntilNextTick);
            short[] chunk = apu.render(chunkFrames);
            System.arraycopy(chunk, 0, pcm, framesRendered * 2, chunk.length);

            framesRendered += chunkFrames;
            samplesUntilNextTick -= chunkFrames;
            if (samplesUntilNextTick == 0) {
                tick60HzClients();
                samplesUntilNextTick = nextTickFrameCount();
            }
        }

        return pcm;
    }

    public boolean ensureAudioPlaying() {
        if (driver.isPlaying()) {
            return true;
        }
        if (isSoundEffectPlaying()) {
            return true;
        }
        if (!loopEnabled || currentTrack == null) {
            return false;
        }
        driver.start(currentTrack);
        driver.tick60Hz();
        resetSampleClock();
        return driver.isPlaying();
    }

    private void tick60HzClients() {
        if (driver.isPlaying()) {
            driver.tick60Hz();
        } else if (loopEnabled && currentTrack != null) {
            ensureAudioPlaying();
        }
        if (isSoundEffectPlaying()) {
            soundEffectPlayer.tick60Hz();
        }
    }

    private boolean isSoundEffectPlaying() {
        return soundEffectPlayer != null && soundEffectPlayer.isPlaying();
    }

    public void resetSampleClock() {
        tickSampleRemainder = 0;
        samplesUntilNextTick = nextTickFrameCount();
    }

    private int nextTickFrameCount() {
        tickSampleRemainder += sampleRate;
        int samples = tickSampleRemainder / 60;
        tickSampleRemainder %= 60;
        return Math.max(1, samples);
    }
}
