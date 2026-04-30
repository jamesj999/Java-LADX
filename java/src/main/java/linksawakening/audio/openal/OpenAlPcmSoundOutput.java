package linksawakening.audio.openal;

import linksawakening.gameplay.PcmSoundOutput;
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

public final class OpenAlPcmSoundOutput implements PcmSoundOutput {

    private static final int MAX_BUFFERS = 8;

    private final DialogPcmBufferQueue bufferQueue = new DialogPcmBufferQueue(MAX_BUFFERS);

    private boolean available;
    private boolean closed;
    private String statusMessage;
    private long device;
    private long context;
    private int source;

    public OpenAlPcmSoundOutput() {
        initializeOpenAl();
    }

    public boolean isAvailable() {
        return available && !closed;
    }

    public String statusMessage() {
        return statusMessage;
    }

    @Override
    public void play(short[] stereoPcm, int sampleRate) {
        if (!isAvailable() || stereoPcm == null || stereoPcm.length == 0 || sampleRate <= 0) {
            return;
        }
        PendingOpenAlBuffer pendingBuffer = null;
        try {
            ALC10.alcMakeContextCurrent(context);
            unqueueProcessedBuffers();
            dropOldestBuffersBeforeQueueing();

            pendingBuffer = new PendingOpenAlBuffer(AL10.alGenBuffers());
            checkAlError("create dialog buffer");
            ShortBuffer pcmBuffer = BufferUtils.createShortBuffer(stereoPcm.length);
            pcmBuffer.put(stereoPcm).flip();
            AL10.alBufferData(pendingBuffer.id(), AL10.AL_FORMAT_STEREO16, pcmBuffer, sampleRate);
            checkAlError("fill dialog buffer");
            AL10.alSourceQueueBuffers(source, pendingBuffer.id());
            checkAlError("queue dialog buffer");
            bufferQueue.trackQueued(pendingBuffer.id());
            pendingBuffer.markTracked();

            if (AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
                AL10.alSourcePlay(source);
                checkAlError("play dialog source");
            }
        } catch (RuntimeException | LinkageError e) {
            deletePendingBuffer(pendingBuffer);
            available = false;
            statusMessage = "OpenAL dialog output unavailable: " + conciseMessage(e);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        releaseNativeResources();
        available = false;
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
            checkAlError("create dialog source");
            AL10.alSourcef(source, AL10.AL_GAIN, 1.0f);
            checkAlError("configure dialog source");
            available = true;
            statusMessage = "OpenAL dialog output ready";
        } catch (RuntimeException | LinkageError e) {
            available = false;
            statusMessage = "OpenAL dialog output unavailable: " + conciseMessage(e);
            releaseNativeResources();
        }
    }

    private void unqueueProcessedBuffers() {
        int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
        for (int i = 0; i < processed; i++) {
            int buffer = AL10.alSourceUnqueueBuffers(source);
            bufferQueue.markDeleted(buffer);
            AL10.alDeleteBuffers(buffer);
        }
        checkAlError("unqueue dialog buffers");
    }

    private void dropOldestBuffersBeforeQueueing() {
        for (int ignored : bufferQueue.buffersToDropBeforeQueueing()) {
            AL10.alSourceStop(source);
            while (AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED) > 0) {
                int buffer = AL10.alSourceUnqueueBuffers(source);
                AL10.alDeleteBuffers(buffer);
                bufferQueue.markDeleted(buffer);
            }
            break;
        }
        checkAlError("drop old dialog buffers");
    }

    private void releaseNativeResources() {
        try {
            if (context != MemoryUtil.NULL) {
                ALC10.alcMakeContextCurrent(context);
            }
            if (source != 0) {
                AL10.alSourceStop(source);
                while (AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED) > 0) {
                    int buffer = AL10.alSourceUnqueueBuffers(source);
                    AL10.alDeleteBuffers(buffer);
                    bufferQueue.markDeleted(buffer);
                }
                for (int buffer : bufferQueue.buffersToDeleteOnClose()) {
                    AL10.alDeleteBuffers(buffer);
                }
                AL10.alDeleteSources(source);
                source = 0;
            }
        } catch (RuntimeException | LinkageError ignored) {
            bufferQueue.buffersToDeleteOnClose();
        } finally {
            if (context != MemoryUtil.NULL) {
                ALC10.alcDestroyContext(context);
                context = MemoryUtil.NULL;
            }
            if (device != MemoryUtil.NULL) {
                ALC10.alcCloseDevice(device);
                device = MemoryUtil.NULL;
            }
        }
    }

    private void deletePendingBuffer(PendingOpenAlBuffer pendingBuffer) {
        if (pendingBuffer == null) {
            return;
        }
        int buffer = pendingBuffer.releaseUntracked();
        if (buffer != 0) {
            AL10.alDeleteBuffers(buffer);
        }
    }

    private static void checkAlError(String operation) {
        int error = AL10.alGetError();
        if (error != AL10.AL_NO_ERROR) {
            throw new IllegalStateException(operation + " failed with OpenAL error 0x" + Integer.toHexString(error));
        }
    }

    private static String conciseMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
