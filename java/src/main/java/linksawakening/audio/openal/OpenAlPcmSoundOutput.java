package linksawakening.audio.openal;

import linksawakening.gameplay.PcmSoundOutput;

public final class OpenAlPcmSoundOutput implements PcmSoundOutput {
    private static final int MAX_BUFFERS = 8;
    private final DialogPcmBufferQueue bufferQueue = new DialogPcmBufferQueue(MAX_BUFFERS);
    private final PcmStreamBuffering buffering = new PcmStreamBuffering();
    private PcmAlDevice device;
    private boolean available;
    private boolean closed;
    private String statusMessage;
    private long backpressureCount;
    private long recoveryCount;
    private boolean needsRecovery;
    private Throwable lastFailure;
    private final java.util.Set<Integer> allocatedBuffers = new java.util.LinkedHashSet<>();

    public OpenAlPcmSoundOutput() {
        try {
            device = new NativePcmAlDevice();
            available = true;
            statusMessage = "OpenAL PCM output ready";
        } catch (RuntimeException | LinkageError failure) {
            statusMessage = "OpenAL PCM output unavailable: " + conciseMessage(failure);
        }
    }

    OpenAlPcmSoundOutput(PcmAlDevice device) {
        this.device = java.util.Objects.requireNonNull(device);
        available = true;
        statusMessage = "OpenAL PCM output ready";
    }

    public boolean isAvailable() { return available && !closed; }
    public String statusMessage() { return statusMessage; }
    public long underrunCount() { return buffering.underrunCount(); }
    public long backpressureCount() { return backpressureCount; }
    public long recoveryCount() { return recoveryCount; }
    public Throwable lastFailure() { return lastFailure; }
    int sourceState() { return device.state(); }

    /** Diagnostic queries must not hide the original playback failure with a second AL error. */
    public String diagnostics() {
        return String.format("status=%s available=%s underruns=%d backpressure=%d recoveries=%d state=%04X queued=%d processed=%d trackedFrames=%d interrupted=%s lastFailure=%s",
                statusMessage, isAvailable(), underrunCount(), backpressureCount(), recoveryCount,
                querySafely(() -> device.state()), querySafely(() -> device.queued()), querySafely(() -> device.processed()),
                bufferQueue.queuedFrames(), Thread.currentThread().isInterrupted(),
                lastFailure == null ? "none" : conciseMessage(lastFailure));
    }

    private int querySafely(java.util.function.IntSupplier query) {
        if (device == null || closed) return -1;
        try { return query.getAsInt(); }
        catch (RuntimeException | LinkageError ignored) { return -1; }
    }

    @Override public void play(short[] pcm, int rate) { play(pcm, rate, false, false); }
    @Override public void playReplacing(short[] pcm, int rate) { play(pcm, rate, true, false); }
    @Override public void playFrame(short[] pcm, int rate) { play(pcm, rate, false, true); }

    private void play(short[] pcm, int rate, boolean replace, boolean streaming) {
        if (!isAvailable() || pcm == null || pcm.length == 0 || rate <= 0) return;
        if (needsRecovery && !tryRecover()) return;
        try {
            device.makeCurrent();
            if (!device.connected()) { deviceLost(); return; }
            if (replace) resetQueue();
            else {
                if (streaming) buffering.observePlaying(device.state() == PcmAlDevice.PLAYING);
                else buffering.reset();
                unqueueProcessedBuffers();
                // A STOPPED source reports even newly queued buffers as processed.
                // Rewind to INITIAL so the prefill can accumulate without being discarded.
                if (streaming && device.state() == PcmAlDevice.STOPPED) device.rewind();
                if (bufferQueue.isFull()) {
                    backpressureCount++;
                    // A burst/full queue is an exceptional transport resync. Do not wait
                    // on the render thread or turn a thread interrupt into permanent silence.
                    resetQueue();
                    statusMessage = "OpenAL PCM rebuffering after queue pressure";
                }
            }
            int buffer = device.createBuffer(pcm, rate);
            allocatedBuffers.add(buffer); // Retain ownership even if queue() fails after submission.
            device.queue(buffer);
            bufferQueue.trackQueued(buffer, pcm.length / 2);
            int state = device.state();
            if (streaming && state == PcmAlDevice.STOPPED) {
                // The device can drain between the state query and queue operation.
                buffering.observePlaying(false);
                device.rewind();
                state = PcmAlDevice.INITIAL;
            }
            if (state != PcmAlDevice.PLAYING && (!streaming || buffering.ready(bufferQueue.queuedFrames(), rate))) {
                device.play();
                if (streaming) buffering.started();
                statusMessage = "OpenAL PCM output playing";
            }
        } catch (RuntimeException | LinkageError failure) {
            playbackFailure(failure);
        }
    }

    private void playbackFailure(Throwable failure) {
        lastFailure = failure;
        recoveryCount++;
        needsRecovery = true;
        statusMessage = "OpenAL PCM recovering: " + conciseMessage(failure);
        System.err.println("PCM recovery: " + diagnostics());
        failure.printStackTrace(System.err);
        tryRecover();
    }

    private boolean tryRecover() {
        try {
            if (!device.connected()) { deviceLost(); return false; }
            device.makeCurrent();
            resetQueue();
            needsRecovery = false;
            statusMessage = "OpenAL PCM rebuffering after recovery";
            return true;
        } catch (RuntimeException | LinkageError retryFailure) {
            // Keep retrying on subsequent frames. A failed AL call is not evidence
            // that the physical device was disconnected.
            statusMessage = "OpenAL PCM recovery pending: " + conciseMessage(retryFailure);
            return false;
        }
    }

    private void deviceLost() {
        available = false;
        statusMessage = "OpenAL PCM device disconnected";
        System.err.println(diagnostics());
    }

    private void unqueueProcessedBuffers() {
        int processed = device.processed();
        for (int i = 0; i < processed; i++) {
            int buffer = device.unqueue();
            bufferQueue.markDeleted(buffer);
            device.deleteBuffer(buffer);
            allocatedBuffers.remove(buffer);
        }
    }

    private void resetQueue() {
        // Deleting/recreating the source detaches buffers even in AL_INITIAL,
        // where alSourceStop is a no-op and unqueueing pending buffers is illegal.
        device.resetSource();
        for (var ids = allocatedBuffers.iterator(); ids.hasNext();) {
            device.deleteBuffer(ids.next());
            ids.remove();
        }
        bufferQueue.buffersToDeleteOnClose();
        buffering.reset();
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        available = false;
        if (device == null) return;
        try { device.makeCurrent(); resetQueue(); }
        catch (RuntimeException | LinkageError failure) {
            System.err.println("PCM cleanup: " + conciseMessage(failure));
        } finally {
            try { device.close(); }
            catch (RuntimeException | LinkageError failure) {
                System.err.println("PCM device cleanup: " + conciseMessage(failure));
            }
            allocatedBuffers.clear();
        }
    }

    private static String conciseMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }
}
