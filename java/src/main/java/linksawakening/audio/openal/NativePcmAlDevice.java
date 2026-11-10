package linksawakening.audio.openal;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

final class NativePcmAlDevice implements PcmAlDevice {
    private long device;
    private long context;
    private int source;
    private boolean supportsDisconnect;

    NativePcmAlDevice() {
        try {
            device = ALC10.alcOpenDevice((ByteBuffer) null);
            if (device == MemoryUtil.NULL) throw new IllegalStateException("could not open device");
            var capabilities = ALC.createCapabilities(device);
            supportsDisconnect = capabilities.ALC_EXT_disconnect;
            context = ALC10.alcCreateContext(device, (IntBuffer) null);
            if (context == MemoryUtil.NULL) throw new IllegalStateException("could not create context");
            makeCurrent();
            AL.createCapabilities(capabilities);
            createSource();
        } catch (RuntimeException | LinkageError failure) {
            close();
            throw failure;
        }
    }

    @Override public void makeCurrent() {
        if (!ALC10.alcMakeContextCurrent(context)) throw new IllegalStateException("could not make PCM context current");
    }
    @Override public int state() { return query(AL10.AL_SOURCE_STATE); }
    @Override public int queued() { return query(AL10.AL_BUFFERS_QUEUED); }
    @Override public int processed() { return query(AL10.AL_BUFFERS_PROCESSED); }
    private int query(int property) {
        int result = AL10.alGetSourcei(source, property);
        check("query PCM source");
        return result;
    }

    @Override public int createBuffer(short[] pcm, int sampleRate) {
        int buffer = AL10.alGenBuffers();
        try {
            check("create PCM buffer");
            var data = BufferUtils.createShortBuffer(pcm.length);
            data.put(pcm).flip();
            AL10.alBufferData(buffer, AL10.AL_FORMAT_STEREO16, data, sampleRate);
            check("fill PCM buffer");
            return buffer;
        } catch (RuntimeException | LinkageError failure) {
            deleteBuffer(buffer);
            throw failure;
        }
    }
    @Override public void queue(int buffer) { AL10.alSourceQueueBuffers(source, buffer); check("queue PCM buffer"); }
    @Override public int unqueue() {
        int buffer = AL10.alSourceUnqueueBuffers(source);
        check("unqueue PCM buffer");
        return buffer;
    }
    @Override public void deleteBuffer(int buffer) {
        if (AL10.alIsBuffer(buffer)) AL10.alDeleteBuffers(buffer);
        check("delete PCM buffer");
    }
    @Override public void play() { AL10.alSourcePlay(source); check("play PCM source"); }
    @Override public void stop() { AL10.alSourceStop(source); check("stop PCM source"); }
    @Override public void rewind() { AL10.alSourceRewind(source); check("rewind PCM source"); }
    @Override public void resetSource() {
        if (source != 0) AL10.alDeleteSources(source); // Detaches every buffer, including partially queued uploads.
        source = 0;
        check("delete PCM source");
        createSource();
    }
    private void createSource() {
        source = AL10.alGenSources();
        check("create PCM source");
        AL10.alSourcef(source, AL10.AL_GAIN, 1.0f);
        check("configure PCM source");
    }
    @Override public boolean connected() {
        return device != MemoryUtil.NULL && (!supportsDisconnect
                || ALC10.alcGetInteger(device, EXTDisconnect.ALC_CONNECTED) != 0);
    }
    @Override public void close() {
        try {
            if (context != MemoryUtil.NULL) {
                makeCurrent();
                if (source != 0) AL10.alDeleteSources(source);
            }
        } finally {
            source = 0;
            if (context != MemoryUtil.NULL) { ALC10.alcDestroyContext(context); context = MemoryUtil.NULL; }
            if (device != MemoryUtil.NULL) { ALC10.alcCloseDevice(device); device = MemoryUtil.NULL; }
        }
    }
    private static void check(String operation) {
        int error = AL10.alGetError();
        if (error != AL10.AL_NO_ERROR) throw new IllegalStateException(operation + " failed with OpenAL error 0x" + Integer.toHexString(error));
    }
}
