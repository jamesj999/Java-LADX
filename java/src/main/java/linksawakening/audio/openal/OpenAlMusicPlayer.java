package linksawakening.audio.openal;

import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.music.MusicDriver;
import linksawakening.audio.music.MusicTrack;
import linksawakening.audio.sfx.SoundEffect;
import linksawakening.audio.sfx.SoundEffectPcmRenderer;
import linksawakening.audio.sfx.SoundEffectPlayer;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayDeque;
import java.util.Objects;

public final class OpenAlMusicPlayer implements AutoCloseable {
    private static final int BUFFER_COUNT = 4;
    private static final int BUFFER_FRAMES = 1024;

    private final MusicDriver driver;
    private final GameBoyApu apu;
    private final SoundEffectPlayer soundEffectPlayer;
    private final int sampleRate;
    private final int[] buffers = new int[BUFFER_COUNT];
    private final ArrayDeque<Integer> freeBuffers = new ArrayDeque<>(BUFFER_COUNT);

    private boolean available;
    private boolean closed;
    private boolean paused;
    private boolean loopEnabled;
    private float volume = 1.0f;
    private String statusMessage;
    private MusicTrack currentTrack;
    private long device;
    private long context;
    private int source;
    private int queuedBuffers;
    private int samplesUntilNextTick;
    private int tickSampleRemainder;

    public OpenAlMusicPlayer(MusicDriver driver, GameBoyApu apu) {
        this(driver, apu, null);
    }

    public OpenAlMusicPlayer(MusicDriver driver, GameBoyApu apu, SoundEffectPlayer soundEffectPlayer) {
        this.driver = Objects.requireNonNull(driver, "driver");
        this.apu = Objects.requireNonNull(apu, "apu");
        this.soundEffectPlayer = soundEffectPlayer;
        sampleRate = apu.sampleRate();
        resetSampleClock();
        initializeOpenAl();
    }

    public boolean isAvailable() {
        return available && !closed;
    }

    public String statusMessage() {
        return statusMessage;
    }

    public void play(MusicTrack track) {
        if (!isAvailable()) {
            return;
        }
        Objects.requireNonNull(track, "track");

        try {
            stopPlayback(true);
            apu.reset();
            currentTrack = track;
            driver.start(track);
            driver.tick60Hz();
            paused = false;
            resetSampleClock();
            fillQueue();
            statusMessage = "Playing " + track.name();
        } catch (RuntimeException e) {
            stopAfterPlaybackFailure(e);
        }
    }

    public void pause() {
        if (!isAvailable() || paused) {
            return;
        }
        AL10.alSourcePause(source);
        paused = true;
        statusMessage = "Paused";
    }

    public void resume() {
        if (!isAvailable() || !paused) {
            return;
        }
        paused = false;
        if (queuedBuffers > 0) {
            AL10.alSourcePlay(source);
        }
        statusMessage = currentTrack == null ? "OpenAL ready" : "Playing " + currentTrack.name();
    }

    public void stop() {
        if (!isAvailable()) {
            return;
        }
        stopPlayback(true);
        currentTrack = null;
        statusMessage = "Stopped";
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
            stopPlayback(true);
            currentTrack = null;
            short[] pcm = SoundEffectPcmRenderer.renderOneShot(apu, soundEffectPlayer, effect);
            queueOneShotPcm(pcm);
            statusMessage = "Playing SFX " + effect.name();
        } catch (RuntimeException e) {
            stopAfterPlaybackFailure(e);
        }
    }

    public void setLoopEnabled(boolean loopEnabled) {
        this.loopEnabled = loopEnabled;
    }

    public void setVolume(float volume) {
        this.volume = clamp(volume);
        if (isAvailable()) {
            AL10.alSourcef(source, AL10.AL_GAIN, this.volume);
        }
    }

    public void update() {
        if (!isAvailable() || paused) {
            return;
        }

        try {
            unqueueProcessedBuffers();
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
            if (source != 0 && context != MemoryUtil.NULL) {
                cleanupFailure = rememberFailure(cleanupFailure,
                        () -> ALC10.alcMakeContextCurrent(context));
            }
            cleanupFailure = combine(cleanupFailure, stopPlaybackBestEffort(true));
        } finally {
            cleanupFailure = combine(cleanupFailure, releaseNativeResources());
            available = false;
            if (statusMessage == null || !statusMessage.startsWith("OpenAL unavailable:")) {
                statusMessage = cleanupFailure == null
                        ? "Closed"
                        : "Closed after cleanup warning: " + conciseMessage(cleanupFailure);
            }
        }
    }

    private void initializeOpenAl() {
        try {
            device = ALC10.alcOpenDevice((ByteBuffer) null);
            if (device == MemoryUtil.NULL) {
                throw new IllegalStateException("could not open device");
            }

            ALCCapabilities deviceCapabilities = ALC.createCapabilities(device);
            context = ALC10.alcCreateContext(device, (IntBuffer) null);
            if (context == MemoryUtil.NULL) {
                throw new IllegalStateException("could not create context");
            }
            if (!ALC10.alcMakeContextCurrent(context)) {
                throw new IllegalStateException("could not make context current");
            }
            AL.createCapabilities(deviceCapabilities);

            source = AL10.alGenSources();
            checkAlError("create source");
            AL10.alGenBuffers(buffers);
            checkAlError("create buffers");
            for (int buffer : buffers) {
                freeBuffers.addLast(buffer);
            }

            AL10.alSourcef(source, AL10.AL_GAIN, volume);
            checkAlError("configure source");
            available = true;
            statusMessage = "OpenAL ready";
        } catch (RuntimeException | LinkageError e) {
            available = false;
            statusMessage = "OpenAL unavailable: " + conciseMessage(e);
            RuntimeException cleanupFailure = releaseNativeResources();
            if (cleanupFailure != null) {
                statusMessage += " (cleanup also failed: " + conciseMessage(cleanupFailure) + ")";
            }
        }
    }

    private void fillQueue() {
        while (queuedBuffers < BUFFER_COUNT && !freeBuffers.isEmpty() && ensureAudioPlaying()) {
            short[] pcm = renderBufferPcm();
            int buffer = freeBuffers.removeFirst();
            ShortBuffer pcmBuffer = BufferUtils.createShortBuffer(pcm.length);
            pcmBuffer.put(pcm).flip();
            AL10.alBufferData(buffer, AL10.AL_FORMAT_STEREO16, pcmBuffer, sampleRate);
            AL10.alSourceQueueBuffers(source, buffer);
            queuedBuffers++;
            checkAlError("queue buffer");
        }
        restartSourceIfNeeded();
    }

    private void queueOneShotPcm(short[] pcm) {
        if (pcm.length == 0) {
            return;
        }
        if (freeBuffers.isEmpty()) {
            throw new IllegalStateException("no OpenAL buffer available for SFX preview");
        }

        int buffer = freeBuffers.removeFirst();
        ShortBuffer pcmBuffer = BufferUtils.createShortBuffer(pcm.length);
        pcmBuffer.put(pcm).flip();
        AL10.alBufferData(buffer, AL10.AL_FORMAT_STEREO16, pcmBuffer, sampleRate);
        AL10.alSourceQueueBuffers(source, buffer);
        queuedBuffers++;
        checkAlError("queue SFX buffer");
        AL10.alSourcePlay(source);
        checkAlError("start SFX source");
    }

    private short[] renderBufferPcm() {
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

    private boolean ensureAudioPlaying() {
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

    private void unqueueProcessedBuffers() {
        int processedBuffers = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
        for (int i = 0; i < processedBuffers; i++) {
            int buffer = AL10.alSourceUnqueueBuffers(source);
            freeBuffers.addLast(buffer);
            queuedBuffers = Math.max(0, queuedBuffers - 1);
        }
        checkAlError("unqueue processed buffers");
    }

    private void restartSourceIfNeeded() {
        if (queuedBuffers == 0 || paused) {
            return;
        }
        int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
        if (state != AL10.AL_PLAYING) {
            AL10.alSourcePlay(source);
            checkAlError("start source");
        }
    }

    private void stopPlayback(boolean stopDriver) {
        if (source != 0) {
            AL10.alSourceStop(source);
            clearQueuedBuffers();
        }
        if (stopDriver) {
            driver.stop();
        }
        if (soundEffectPlayer != null) {
            soundEffectPlayer.stop();
        }
        paused = false;
        resetSampleClock();
    }

    private void clearQueuedBuffers() {
        if (source == 0) {
            return;
        }
        int queued = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
        for (int i = 0; i < queued; i++) {
            int buffer = AL10.alSourceUnqueueBuffers(source);
            freeBuffers.addLast(buffer);
        }
        queuedBuffers = 0;
        checkAlError("clear queued buffers");
    }

    private void stopAfterPlaybackFailure(RuntimeException failure) {
        RuntimeException cleanupFailure = stopPlaybackBestEffort(true);
        statusMessage = "Playback failed: " + conciseMessage(failure);
        if (cleanupFailure != null) {
            statusMessage += " (cleanup also failed: " + conciseMessage(cleanupFailure) + ")";
        }
    }

    private RuntimeException stopPlaybackBestEffort(boolean stopDriver) {
        RuntimeException failure = null;
        if (source != 0) {
            failure = rememberFailure(failure, () -> AL10.alSourceStop(source));
            failure = clearQueuedBuffersBestEffort(failure);
        }
        if (stopDriver) {
            failure = rememberFailure(failure, driver::stop);
        }
        if (soundEffectPlayer != null) {
            failure = rememberFailure(failure, soundEffectPlayer::stop);
        }
        paused = false;
        resetSampleClock();
        return failure;
    }

    private RuntimeException clearQueuedBuffersBestEffort(RuntimeException failure) {
        if (source == 0) {
            return failure;
        }

        int queued;
        try {
            queued = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
        } catch (RuntimeException e) {
            queuedBuffers = 0;
            return combine(failure, e);
        }

        for (int i = 0; i < queued; i++) {
            try {
                int buffer = AL10.alSourceUnqueueBuffers(source);
                freeBuffers.addLast(buffer);
            } catch (RuntimeException e) {
                failure = combine(failure, e);
            }
        }
        queuedBuffers = 0;
        return rememberFailure(failure, () -> checkAlError("clear queued buffers"));
    }

    private RuntimeException releaseNativeResources() {
        RuntimeException failure = null;
        if (context != MemoryUtil.NULL) {
            failure = rememberFailure(failure, () -> ALC10.alcMakeContextCurrent(context));
        }
        if (source != 0) {
            int sourceToDelete = source;
            try {
                AL10.alDeleteSources(sourceToDelete);
            } catch (RuntimeException e) {
                failure = combine(failure, e);
            } finally {
                source = 0;
            }
        }
        if (hasGeneratedBuffers()) {
            int[] buffersToDelete = buffers.clone();
            try {
                AL10.alDeleteBuffers(buffersToDelete);
            } catch (RuntimeException e) {
                failure = combine(failure, e);
            } finally {
                for (int i = 0; i < buffers.length; i++) {
                    buffers[i] = 0;
                }
            }
        }
        freeBuffers.clear();
        queuedBuffers = 0;
        if (context != MemoryUtil.NULL) {
            long contextToDestroy = context;
            try {
                ALC10.alcMakeContextCurrent(MemoryUtil.NULL);
            } catch (RuntimeException e) {
                failure = combine(failure, e);
            }
            try {
                ALC10.alcDestroyContext(contextToDestroy);
            } catch (RuntimeException e) {
                failure = combine(failure, e);
            } finally {
                context = MemoryUtil.NULL;
            }
        }
        if (device != MemoryUtil.NULL) {
            long deviceToClose = device;
            try {
                ALC10.alcCloseDevice(deviceToClose);
            } catch (RuntimeException e) {
                failure = combine(failure, e);
            } finally {
                device = MemoryUtil.NULL;
            }
        }
        return failure;
    }

    private boolean hasGeneratedBuffers() {
        for (int buffer : buffers) {
            if (buffer != 0) {
                return true;
            }
        }
        return false;
    }

    private void resetSampleClock() {
        tickSampleRemainder = 0;
        samplesUntilNextTick = nextTickFrameCount();
    }

    private int nextTickFrameCount() {
        tickSampleRemainder += sampleRate;
        int samples = tickSampleRemainder / 60;
        tickSampleRemainder %= 60;
        return Math.max(1, samples);
    }

    private static RuntimeException rememberFailure(RuntimeException first, Runnable operation) {
        try {
            operation.run();
            return first;
        } catch (RuntimeException e) {
            return combine(first, e);
        }
    }

    private static RuntimeException combine(RuntimeException first, RuntimeException next) {
        if (next == null) {
            return first;
        }
        if (first == null) {
            return next;
        }
        first.addSuppressed(next);
        return first;
    }

    private static void checkAlError(String operation) {
        int error = AL10.alGetError();
        if (error != AL10.AL_NO_ERROR) {
            throw new IllegalStateException("OpenAL " + operation + " failed: 0x"
                    + Integer.toHexString(error).toUpperCase());
        }
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

    private static String conciseMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return message;
    }
}
