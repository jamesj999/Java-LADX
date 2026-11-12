package linksawakening.recording;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import linksawakening.gpu.Framebuffer;

/**
 * Streams the engine's 60 Hz RGBA and mixed PCM to native FFmpeg encoders.
 * Only compressed streams touch disk; memory use is independent of recording length.
 * Calls are synchronous: slow encoders apply backpressure instead of dropping frames.
 */
public final class GameplayRecorder implements AutoCloseable {
    private final RecordingOptions options;
    private final Path output;
    private final Path temporary;
    private Process video;
    private Process audio;
    private final byte[] pcm = new byte[48_000 / 60 * 2 * 2];
    private long frames;
    private boolean closed;
    private boolean failed;

    public GameplayRecorder(RecordingOptions options) {
        this.options = options;
        output = options.output().toAbsolutePath();
        Path directory = null;
        try {
            if (Files.exists(output)) throw new IOException("Recording already exists: " + output);
            Files.createDirectories(output.getParent());
            directory = Files.createTempDirectory(output.getParent(), ".recording-");
            temporary = directory;
            video = start(
                    "video",
                    List.of(
                            "-f",
                            "rawvideo",
                            "-pixel_format",
                            "rgba",
                            "-video_size",
                            "160x144",
                            "-framerate",
                            "60",
                            "-i",
                            "pipe:0",
                            "-an",
                            "-vf",
                            "scale=" + Framebuffer.WIDTH * options.scale() + ":" + Framebuffer.HEIGHT * options.scale()
                                    + ":flags=neighbor,setsar=1",
                            "-c:v",
                            "libx264",
                            "-preset",
                            "medium",
                            "-crf",
                            Integer.toString(options.crf()),
                            "-pix_fmt",
                            "yuv420p",
                            "-threads",
                            "2",
                            temporary.resolve("video.mp4").toString()));
            audio = start(
                    "audio",
                    List.of(
                            "-f",
                            "s16le",
                            "-ar",
                            "48000",
                            "-ac",
                            "2",
                            "-i",
                            "pipe:0",
                            "-vn",
                            "-c:a",
                            "aac",
                            "-b:a",
                            "96k",
                            temporary.resolve("audio.m4a").toString()));
        } catch (IOException e) {
            if (video != null) stop(video);
            if (audio != null) stop(audio);
            removeTemporary(directory);
            throw new UncheckedIOException("Cannot start recording; install FFmpeg with libx264 and AAC support", e);
        }
    }

    private Process start(String name, List<String> arguments) throws IOException {
        var command = new ArrayList<String>();
        command.add(options.ffmpeg());
        command.addAll(List.of("-hide_banner", "-loglevel", "error", "-nostdin", "-n"));
        command.addAll(arguments);
        return new ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(temporary.resolve(name + ".log").toFile())
                .start();
    }

    /** Reset before simulation; paused frames receive silence, not stale audio. */
    public void beginFrame() {
        requireOpen();
        Arrays.fill(pcm, (byte) 0);
    }

    /** Copies borrowed samples before the audio transport reuses its storage. */
    public void captureAudio(short[] samples, int rate) {
        requireOpen();
        if (rate != 48_000 || samples.length * 2 != pcm.length)
            throw new IllegalArgumentException("Recording requires one 48 kHz stereo PCM frame");
        for (int i = 0; i < samples.length; i++) {
            pcm[i * 2] = (byte) samples[i];
            pcm[i * 2 + 1] = (byte) (samples[i] >>> 8);
        }
    }

    public void captureFrame(byte[] rgba) {
        requireOpen();
        if (rgba.length != Framebuffer.WIDTH * Framebuffer.HEIGHT * 4)
            throw new IllegalArgumentException("Recording requires a 160×144 RGBA frame");
        try {
            video.getOutputStream().write(rgba);
            audio.getOutputStream().write(pcm);
            frames++;
        } catch (IOException e) {
            failed = true;
            throw new UncheckedIOException("Recording encoder failed: " + diagnostics(), e);
        }
    }

    private void requireOpen() {
        if (closed || failed) throw new IllegalStateException("Recording is closed or failed");
    }

    public long frameCount() {
        return frames;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try {
            if (failed || frames == 0) return;
            // Both streams must receive EOF before waiting for either encoder.
            try (OutputStream v = video.getOutputStream();
                    OutputStream a = audio.getOutputStream()) {
                v.flush();
                a.flush();
            }
            await(video);
            await(audio);
            Process mux = start(
                    "mux",
                    List.of(
                            "-i",
                            temporary.resolve("video.mp4").toString(),
                            "-i",
                            temporary.resolve("audio.m4a").toString(),
                            "-map",
                            "0:v:0",
                            "-map",
                            "1:a:0",
                            "-c",
                            "copy",
                            "-movflags",
                            "+faststart",
                            temporary.resolve("finished.mp4").toString()));
            try {
                mux.getOutputStream().close();
                await(mux);
            } finally {
                stop(mux);
            }
            // No REPLACE_EXISTING: a concurrent recording must never overwrite another.
            Files.move(temporary.resolve("finished.mp4"), output);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not finalize recording: " + diagnostics(), e);
        } finally {
            stop(video);
            stop(audio);
            removeTemporary(temporary);
        }
    }

    private void await(Process process) throws IOException {
        try {
            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("FFmpeg timed out");
            }
            if (process.exitValue() != 0) throw new IOException("FFmpeg exited with " + process.exitValue());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while finalizing recording", e);
        }
    }

    private String diagnostics() {
        var text = new StringBuilder();
        for (String name : List.of("video", "audio", "mux")) {
            try (var input = Files.newInputStream(temporary.resolve(name + ".log"))) {
                text.append(new String(input.readNBytes(8192), java.nio.charset.StandardCharsets.UTF_8));
            } catch (IOException ignored) {
                // A process that failed to launch may not have created a log.
            }
        }
        return text.toString();
    }

    private static void stop(Process process) {
        process.destroyForcibly();
        try {
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void removeTemporary(Path directory) {
        if (directory == null) return;
        try (var paths = Files.list(directory)) {
            for (Path path : paths.toList()) Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        } catch (IOException ignored) {
            System.err.println("Could not remove recording temporary directory: " + directory);
        }
    }
}
