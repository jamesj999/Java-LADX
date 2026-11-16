package linksawakening.audio.music;

import java.util.Arrays;
import java.util.Objects;
import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.sfx.SoundEffectPlayback;

/** Sample-clock transport for the browser, independent of native audio. */
public final class FrameAudioMixer {
    private static final int BUFFER_FRAMES = 1024;
    private final MusicDriver driver;
    private final GameBoyApu apu;
    private final SoundEffectPlayback soundEffectPlayer;
    private final int sampleRate;
    private int samplesUntilNextTick;
    private int tickSampleRemainder;
    private boolean loopEnabled;
    // No selected track until setTrack; trackSelected is the presence bit used by the sample clock.
    private MusicTrack currentTrack;
    private boolean trackSelected;

    private final short[] pcm = new short[BUFFER_FRAMES * 2];

    public FrameAudioMixer(MusicDriver driver, GameBoyApu apu, SoundEffectPlayback effects) {
        this.driver = Objects.requireNonNull(driver, "driver");
        this.apu = Objects.requireNonNull(apu, "apu");
        soundEffectPlayer = Objects.requireNonNull(effects, "effects");
        sampleRate = apu.sampleRate();
        resetSampleClock();
    }

    public void setTrack(MusicTrack track) {
        currentTrack = track;
        trackSelected = track != null;
    }

    public void setLoopEnabled(boolean enabled) {
        loopEnabled = enabled;
    }

    /** Returned storage is reused by the next call. */
    public short[] renderBufferPcm() {
        Arrays.fill(pcm, (short) 0);
        int framesRendered = 0;

        while (framesRendered < BUFFER_FRAMES && ensureAudioPlaying()) {
            int chunkFrames = Math.min(BUFFER_FRAMES - framesRendered, samplesUntilNextTick);
            apu.renderInto(pcm, framesRendered * 2, chunkFrames);

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
        if (!loopEnabled || !trackSelected) {
            return false;
        }
        driver.start(currentTrack);
        driver.tick60Hz();
        resetSampleClock();
        return driver.isPlaying();
    }

    public void resetSampleClock() {
        tickSampleRemainder = 0;
        samplesUntilNextTick = nextTickFrameCount();
    }

    private void tick60HzClients() {
        if (driver.isPlaying()) {
            driver.tick60Hz();
        } else if (loopEnabled && trackSelected) {
            ensureAudioPlaying();
        }
        if (isSoundEffectPlaying()) {
            soundEffectPlayer.tick60Hz();
        }
    }

    private boolean isSoundEffectPlaying() {
        return soundEffectPlayer.isPlaying();
    }

    private int nextTickFrameCount() {
        tickSampleRemainder += sampleRate;
        int samples = tickSampleRemainder / 60;
        tickSampleRemainder %= 60;
        return Math.max(1, samples);
    }
}
