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
    int sourceState() { return device.state(); }

    public String diagnostics() {
        return String.format("status=%s available=%s underruns=%d backpressure=%d state=%04X queued=%d processed=%d trackedFrames=%d interrupted=%s",
                statusMessage, isAvailable(), underrunCount(), backpressureCount(),
                device.state(), device.queued(), device.processed(), bufferQueue.queuedFrames(), Thread.currentThread().isInterrupted());
    }

    @Override public void play(short[] pcm, int rate) { play(pcm, rate, false, false); }
    @Override public void playReplacing(short[] pcm, int rate) { play(pcm, rate, true, false); }
    @Override public void playFrame(short[] pcm, int rate) { play(pcm, rate, false, true); }

    private void play(short[] pcm, int rate, boolean replace, boolean streaming) {
        if (!isAvailable() || pcm == null || pcm.length == 0 || rate <= 0) return;
        PendingOpenAlBuffer pending = null;
        try {
            device.makeCurrent();
            if (replace) clearQueuedBuffers();
            else {
                if (streaming) buffering.observePlaying(device.state() == PcmAlDevice.PLAYING);
                else buffering.reset();
                unqueueProcessedBuffers();
                // A STOPPED source reports even newly queued buffers as processed.
                // Rewind to INITIAL so the prefill can accumulate without being discarded.
                if (streaming && device.state() == PcmAlDevice.STOPPED) device.rewind();
                waitForQueueSpace(rate);
            }
            pending = new PendingOpenAlBuffer(device.createBuffer(pcm, rate));
            device.queue(pending.id());
            bufferQueue.trackQueued(pending.id(), pcm.length / 2);
            pending.markTracked();
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
            }
        } catch (RuntimeException | LinkageError failure) {
            System.err.println("PCM FAILURE " + diagnostics());
            failure.printStackTrace(System.err);
            if (pending != null) {
                int id = pending.releaseUntracked();
                if (id != 0) device.deleteBuffer(id);
            }
            available = false;
            statusMessage = "OpenAL PCM output unavailable: " + conciseMessage(failure);
        }
    }

    private void unqueueProcessedBuffers() {
        int processed = device.processed();
        for (int i = 0; i < processed; i++) {
            int buffer = device.unqueue();
            bufferQueue.markDeleted(buffer);
            device.deleteBuffer(buffer);
        }
    }

    private void waitForQueueSpace(int rate) {
        if (!bufferQueue.isFull()) return;
        backpressureCount++;
        if (device.state() != PcmAlDevice.PLAYING) { device.play(); buffering.started(); }
        long deadline = System.nanoTime() + 1_000_000_000L + bufferQueue.queuedFrames() * 1_000_000_000L / rate;
        while (bufferQueue.isFull()) {
            java.util.concurrent.locks.LockSupport.parkNanos(1_000_000);
            unqueueProcessedBuffers();
            if (System.nanoTime() > deadline) throw new IllegalStateException("PCM device stopped consuming buffers");
            if (Thread.currentThread().isInterrupted()) throw new IllegalStateException("PCM queue wait interrupted");
        }
    }

    private void clearQueuedBuffers() {
        buffering.reset();
        device.stop();
        while (device.queued() > 0) {
            int buffer = device.unqueue();
            device.deleteBuffer(buffer);
            bufferQueue.markDeleted(buffer);
        }
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        available = false;
        if (device == null) return;
        try { device.makeCurrent(); clearQueuedBuffers(); }
        finally { device.close(); }
    }

    private static String conciseMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }
}
