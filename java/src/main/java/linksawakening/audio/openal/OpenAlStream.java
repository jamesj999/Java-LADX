package linksawakening.audio.openal;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;

/** Owns the browser's native device, context, source and buffer pool. */
final class OpenAlStream {
    static final int BUFFER_COUNT = 4;
    final int[] buffers = new int[BUFFER_COUNT];
    final IntBufferRing freeBuffers = new IntBufferRing(BUFFER_COUNT);
    boolean available;
    String statusMessage;
    long device;
    long context;
    int source;
    int queuedBuffers;

    void makeContextCurrent() {
        if (context != MemoryUtil.NULL && !ALC10.alcMakeContextCurrent(context)) {
            throw new IllegalStateException("could not make music OpenAL context current");
        }
    }

    void initializeOpenAl() {
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

            AL10.alSourcef(source, AL10.AL_GAIN, 1.0f);
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

    RuntimeException releaseNativeResources() {
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

    void unqueueProcessedBuffers() {
        int processedBuffers = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
        for (int i = 0; i < processedBuffers; i++) {
            int buffer = AL10.alSourceUnqueueBuffers(source);
            freeBuffers.addLast(buffer);
            queuedBuffers = Math.max(0, queuedBuffers - 1);
        }
        checkAlError("unqueue processed buffers");
    }

    void clearQueuedBuffers() {
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

    RuntimeException clearQueuedBuffersBestEffort(RuntimeException failure) {
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

    static RuntimeException rememberFailure(RuntimeException first, Runnable operation) {
        try {
            operation.run();
            return first;
        } catch (RuntimeException e) {
            return combine(first, e);
        }
    }

    static RuntimeException combine(RuntimeException first, RuntimeException next) {
        if (next == null) {
            return first;
        }
        if (first == null) {
            return next;
        }
        first.addSuppressed(next);
        return first;
    }

    static void checkAlError(String operation) {
        int error = AL10.alGetError();
        if (error != AL10.AL_NO_ERROR) {
            throw new IllegalStateException("OpenAL " + operation + " failed: 0x"
                    + Integer.toHexString(error).toUpperCase());
        }
    }

    static String conciseMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return message;
    }
}
