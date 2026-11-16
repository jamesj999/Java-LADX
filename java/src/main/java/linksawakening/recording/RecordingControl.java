package linksawakening.recording;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import linksawakening.app.debug.DebugLog;
import linksawakening.app.host.Host;

/** Owns session recording, including encoder lifetime, frame capture and host status updates. */
public final class RecordingControl implements AutoCloseable {
    private Capture capture = Inactive.INSTANCE;
    private RecordingOptions settings = RecordingOptions.defaults(Path.of("recordings/gameplay.mp4"));
    // The host subscribes when session wiring creates it, after optional startup recording.
    private Consumer<Boolean> status = ignored -> {};
    private boolean closed;

    /** Configure before the session is initialized; starts its initial recording immediately. */
    public void configure(RecordingOptions options) {
        if (active() || closed) throw new IllegalStateException("Session already initialized or closed");
        capture = new Active(new GameplayRecorder(options));
        settings = options;
    }

    /** Installs the audio tap; status is first published after the host opens. */
    public Host decorateHost(Host host) {
        Objects.requireNonNull(host);
        status = host::recordingStatus;
        return new RecordingHost(host, this::currentRecorder);
    }

    /** Toggle from the input callback, between complete simulation frames. */
    public void toggle() {
        if (closed) return;
        try {
            if (active()) {
                Capture finished = capture;
                capture = Inactive.INSTANCE; // Detach audio before finalizing, including on encoder failure.
                finished.close();
                DebugLog.log("Recording stopped");
            } else {
                var directory = settings.output().toAbsolutePath().getParent();
                var output = directory.resolve("gameplay-"
                        + LocalDateTime.now(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                        + "-" + UUID.randomUUID() + ".mp4");
                capture = new Active(new GameplayRecorder(
                        new RecordingOptions(output, settings.scale(), settings.crf(), settings.ffmpeg())));
                DebugLog.log("Recording started: " + output);
            }
        } catch (UncheckedIOException failure) {
            DebugLog.warning("Recording failed: " + failure.getMessage() + ": "
                    + failure.getCause().getMessage());
        } finally {
            notifyHost();
        }
    }

    public boolean active() {
        return capture != Inactive.INSTANCE;
    }

    /** Nullable encoder identity for the host's existing audio-tap protocol. */
    public GameplayRecorder currentRecorder() {
        return capture.recorder();
    }

    public void notifyHost() {
        status.accept(active());
    }

    /** Called before simulation so paused frames receive silence rather than stale audio. */
    public void beginFrame() {
        capture.beginFrame();
    }

    /** Called after composition with the borrowed display buffer. */
    public void captureFrame(byte[] buffer) {
        capture.captureFrame(buffer);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        capture.close();
    }

    private interface Capture {
        GameplayRecorder recorder();

        void beginFrame();

        void captureFrame(byte[] buffer);

        void close();
    }

    private enum Inactive implements Capture {
        INSTANCE;

        @Override
        public GameplayRecorder recorder() {
            return null;
        }

        @Override
        public void beginFrame() {}

        @Override
        public void captureFrame(byte[] buffer) {}

        @Override
        public void close() {}
    }

    private record Active(GameplayRecorder recorder) implements Capture {
        private Active {
            Objects.requireNonNull(recorder);
        }

        @Override
        public void beginFrame() {
            recorder.beginFrame();
        }

        @Override
        public void captureFrame(byte[] buffer) {
            recorder.captureFrame(buffer);
        }

        @Override
        public void close() {
            recorder.close();
        }
    }
}
