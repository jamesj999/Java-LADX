package linksawakening.audio.openal;

import static linksawakening.audio.openal.OpenAlStream.*;

import java.util.Objects;
import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.music.FrameAudioMixer;
import linksawakening.audio.music.FrameMusicPlayer;
import linksawakening.audio.music.MusicDriver;
import linksawakening.audio.music.MusicTrack;
import linksawakening.audio.sfx.SoundEffect;
import linksawakening.audio.sfx.SoundEffectPcmRenderer;
import linksawakening.audio.sfx.SoundEffectPlayer;

/** Browser transport controls over a pure sample mixer and native stream. */
public final class OpenAlMusicPlayer implements FrameMusicPlayer {
    private static final int BUFFER_COUNT = 4;
    private static final int BUFFER_FRAMES = 1024;

    private final FrameAudioMixer mixer;
    private final OpenAlStream stream = new OpenAlStream();
    private final MusicDriver driver;
    private final GameBoyApu apu;
    private final SoundEffectPlayer soundEffectPlayer;
    private final int sampleRate;

    private boolean closed;
    private boolean paused;
    private float volume = 1.0f;
    private MusicTrack currentTrack;

    public OpenAlMusicPlayer(MusicDriver driver, GameBoyApu apu) {
        this(driver, apu, null);
    }

    public OpenAlMusicPlayer(MusicDriver driver, GameBoyApu apu, SoundEffectPlayer soundEffectPlayer) {
        this.driver = Objects.requireNonNull(driver, "driver");
        this.apu = Objects.requireNonNull(apu, "apu");
        this.soundEffectPlayer = soundEffectPlayer;
        sampleRate = apu.sampleRate();
        mixer = new FrameAudioMixer(driver, apu, soundEffectPlayer);
        stream.initializeOpenAl();
    }

    public boolean isAvailable() {
        return stream.available && !closed;
    }

    public String statusMessage() {
        return stream.statusMessage;
    }

    /** Returns whether the ROM music driver still has an active channel. */
    public boolean isMusicPlaying() {
        return isAvailable() && driver.isPlaying();
    }

    @Override
    public void play(MusicTrack track) {
        if (!isAvailable()) {
            return;
        }
        Objects.requireNonNull(track, "track");

        try {
            stream.makeContextCurrent();
            stopPlayback(true);
            apu.reset();
            currentTrack = track;
            mixer.setTrack(track);
            driver.start(track);
            driver.tick60Hz();
            paused = false;
            mixer.resetSampleClock();
            fillQueue();
            stream.statusMessage = "Playing " + track.name();
        } catch (RuntimeException e) {
            stopAfterPlaybackFailure(e);
        }
    }

    public void pause() {
        if (!isAvailable() || paused) {
            return;
        }
        stream.makeContextCurrent();
        stream.pause();
        paused = true;
        stream.statusMessage = "Paused";
    }

    public void resume() {
        if (!isAvailable() || !paused) {
            return;
        }
        paused = false;
        stream.makeContextCurrent();
        if (stream.queuedBuffers > 0) {
            stream.play();
        }
        stream.statusMessage = currentTrack == null ? "OpenAL ready" : "Playing " + currentTrack.name();
    }

    @Override
    public void stop() {
        if (!isAvailable()) {
            return;
        }
        stream.makeContextCurrent();
        stopPlayback(true);
        currentTrack = null;
        mixer.setTrack(null);
        stream.statusMessage = "Stopped";
    }

    public boolean isSoundEffectAvailable() {
        return isAvailable() && soundEffectPlayer != null;
    }

    public void playSoundEffect(SoundEffect effect) {
        if (!isSoundEffectAvailable()) {
            return;
        }
        Objects.requireNonNull(effect, "effect");

        try {
            stream.makeContextCurrent();
            stopPlayback(true);
            currentTrack = null;
            mixer.setTrack(null);
            short[] pcm = SoundEffectPcmRenderer.renderOneShot(apu, soundEffectPlayer, effect);
            queueOneShotPcm(pcm);
            stream.statusMessage = "Playing SFX " + effect.name();
        } catch (RuntimeException e) {
            stopAfterPlaybackFailure(e);
        }
    }

    public void setLoopEnabled(boolean loopEnabled) {
        mixer.setLoopEnabled(loopEnabled);
    }

    public void setVolume(float volume) {
        this.volume = clamp(volume);
        if (isAvailable()) {
            stream.makeContextCurrent();
            stream.setGain(this.volume);
        }
    }

    public void update() {
        if (!isAvailable() || paused) {
            return;
        }

        try {
            stream.makeContextCurrent();
            stream.unqueueProcessedBuffers();
            fillQueue();
            restartSourceIfNeeded();
        } catch (RuntimeException e) {
            stopAfterPlaybackFailure(e);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        RuntimeException cleanupFailure = null;
        try {
            if (stream.hasSourceAndContext()) {
                cleanupFailure = rememberFailure(cleanupFailure, stream::activateForCleanup);
            }
            cleanupFailure = combine(cleanupFailure, stopPlaybackBestEffort(true));
        } finally {
            cleanupFailure = combine(cleanupFailure, stream.releaseNativeResources());
            stream.available = false;
            if (stream.statusMessage == null || !stream.statusMessage.startsWith("OpenAL unavailable:")) {
                stream.statusMessage = cleanupFailure == null
                        ? "Closed"
                        : "Closed after cleanup warning: " + conciseMessage(cleanupFailure);
            }
        }
    }

    private void fillQueue() {
        while (stream.queuedBuffers < BUFFER_COUNT && !stream.freeBuffers.isEmpty() && mixer.ensureAudioPlaying()) {
            short[] pcm = mixer.renderBufferPcm();
            stream.queuePcm(pcm, sampleRate, true);
            checkAlError("queue buffer");
        }
        restartSourceIfNeeded();
    }

    private void queueOneShotPcm(short[] pcm) {
        if (pcm.length == 0) {
            return;
        }
        if (stream.freeBuffers.isEmpty()) {
            throw new IllegalStateException("no OpenAL buffer available for SFX preview");
        }

        stream.queuePcm(pcm, sampleRate, false);
        checkAlError("queue SFX buffer");
        stream.play();
        checkAlError("start SFX source");
    }

    private void restartSourceIfNeeded() {
        if (stream.queuedBuffers == 0 || paused) {
            return;
        }
        if (!stream.isPlaying()) {
            stream.play();
            checkAlError("start source");
        }
    }

    private void stopPlayback(boolean stopDriver) {
        if (stream.hasSource()) {
            stream.stop();
            stream.clearQueuedBuffers();
        }
        if (stopDriver) {
            driver.stop();
        }
        if (soundEffectPlayer != null) {
            soundEffectPlayer.stop();
        }
        paused = false;
        mixer.resetSampleClock();
    }

    private void stopAfterPlaybackFailure(RuntimeException failure) {
        RuntimeException cleanupFailure = stopPlaybackBestEffort(true);
        stream.statusMessage = "Playback failed: " + conciseMessage(failure);
        if (cleanupFailure != null) {
            stream.statusMessage += " (cleanup also failed: " + conciseMessage(cleanupFailure) + ")";
        }
    }

    private RuntimeException stopPlaybackBestEffort(boolean stopDriver) {
        RuntimeException failure = null;
        if (stream.hasSource()) {
            failure = rememberFailure(failure, () -> stream.stop());
            failure = stream.clearQueuedBuffersBestEffort(failure);
        }
        if (stopDriver) {
            failure = rememberFailure(failure, driver::stop);
        }
        if (soundEffectPlayer != null) {
            failure = rememberFailure(failure, soundEffectPlayer::stop);
        }
        paused = false;
        mixer.resetSampleClock();
        return failure;
    }

    private static float clamp(float value) {
        if (Float.isNaN(value) || value < 0.0f) {
            return 0.0f;
        }
        if (value > 1.0f) {
            return 1.0f;
        }
        return value;
    }
}
