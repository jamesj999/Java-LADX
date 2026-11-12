package linksawakening.recording;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GameplayRecorderTest {
    @TempDir
    Path directory;

    static void requireEncoder() {
        try {
            assumeTrue(new ProcessBuilder("ffmpeg", "-version")
                            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                            .redirectError(ProcessBuilder.Redirect.DISCARD)
                            .start()
                            .waitFor()
                    == 0);
            assumeTrue(new ProcessBuilder("ffprobe", "-version")
                            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                            .redirectError(ProcessBuilder.Redirect.DISCARD)
                            .start()
                            .waitFor()
                    == 0);
        } catch (IOException e) {
            assumeTrue(false, "FFmpeg/ffprobe not installed; encoding integration test skipped");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    @Test
    void encodesSynchronizedBrowserCompatibleStreamsAndFastStart() throws Exception {
        requireEncoder();
        Path output = directory.resolve("clip with spaces.mp4");
        byte[] pixels = new byte[160 * 144 * 4];
        short[] tone = new short[1600];
        try (var recorder = new GameplayRecorder(RecordingOptions.defaults(output))) {
            for (int frame = 0; frame < 90; frame++) {
                recorder.beginFrame();
                for (int i = 0; i < 800; i++) {
                    short sample = (short) (8000 * Math.sin(2 * Math.PI * 440 * (frame * 800 + i) / 48000));
                    tone[i * 2] = sample;
                    tone[i * 2 + 1] = sample;
                }
                if (frame < 60) recorder.captureAudio(tone, 48000);
                java.util.Arrays.fill(tone, (short) 0); // Borrowed storage may immediately be reused.
                for (int i = 0; i < pixels.length; i += 4) {
                    pixels[i] = (byte) (frame < 45 ? 240 : 16);
                    pixels[i + 1] = 32;
                    pixels[i + 2] = (byte) (frame < 45 ? 16 : 240);
                    pixels[i + 3] = (byte) 255;
                }
                recorder.captureFrame(pixels);
            }
            assertEquals(90, recorder.frameCount());
        }
        String video = probe(output, "v:0", "codec_name,pix_fmt,width,height,r_frame_rate,nb_frames,duration");
        for (String expected : List.of(
                "codec_name=h264",
                "pix_fmt=yuv420p",
                "width=160",
                "height=144",
                "r_frame_rate=60/1",
                "nb_frames=90",
                "duration=1.500000")) assertTrue(video.contains(expected), video);
        String audio = probe(output, "a:0", "codec_name,sample_rate,channels,duration");
        for (String expected : List.of("codec_name=aac", "sample_rate=48000", "channels=2", "duration=1.500000"))
            assertTrue(audio.contains(expected), audio);
        String bytes = Files.readString(output, StandardCharsets.ISO_8859_1);
        assertTrue(bytes.indexOf("moov") < bytes.indexOf("mdat"));
        assertTrue(Files.size(output) < 100_000, "Simple 1.5 second clip should remain compact");
        Path decoded = directory.resolve("decoded.pcm");
        run(List.of("ffmpeg", "-v", "error", "-i", output.toString(), "-vn", "-f", "s16le", decoded.toString()));
        byte[] pcm = Files.readAllBytes(decoded);
        assertTrue(energy(pcm, 4800, 9600) > 1000, "Captured tone must survive caller buffer reuse");
        assertTrue(energy(pcm, 62400, 67200) < 10, "Paused frames must be silent");
        try (var files = Files.list(directory)) {
            assertFalse(files.anyMatch(p -> p.getFileName().toString().startsWith(".recording-")));
        }
    }

    private static double energy(byte[] pcm, int start, int end) {
        long sum = 0;
        for (int i = start; i < end; i++) sum += Math.abs((short) ((pcm[i * 4] & 255) | (pcm[i * 4 + 1] << 8)));
        return (double) sum / (end - start);
    }

    @Test
    void supportsIntegerScalingAndIdempotentClose() throws Exception {
        requireEncoder();
        Path output = directory.resolve("scaled.mp4");
        var recorder = new GameplayRecorder(new RecordingOptions(output, 2, 18, "ffmpeg"));
        recorder.beginFrame();
        recorder.captureFrame(new byte[160 * 144 * 4]);
        recorder.close();
        recorder.close();
        String metadata = probe(output, "v:0", "width,height,nb_frames");
        assertTrue(
                metadata.contains("width=320") && metadata.contains("height=288") && metadata.contains("nb_frames=1"));
        assertThrows(IllegalStateException.class, recorder::beginFrame);
    }

    @Test
    void missingEncoderAndExistingOutputLeaveNoTemporaryFiles() throws Exception {
        Path output = directory.resolve("existing.mp4");
        Files.writeString(output, "keep me");
        assertThrows(UncheckedIOException.class, () -> new GameplayRecorder(RecordingOptions.defaults(output)));
        assertEquals("keep me", Files.readString(output));
        assertThrows(
                UncheckedIOException.class,
                () -> new GameplayRecorder(new RecordingOptions(
                        directory.resolve("new.mp4"),
                        1,
                        18,
                        directory.resolve("no-encoder").toString())));
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void emptyRecordingAndEncoderFailureAreCleanedUp() throws Exception {
        requireEncoder();
        Path output = directory.resolve("empty.mp4");
        try (var ignored = new GameplayRecorder(RecordingOptions.defaults(output))) {}
        assertFalse(Files.exists(output));
        // Java is an executable available on every test host, but rejects FFmpeg's arguments.
        String executable = Path.of(
                        System.getProperty("java.home"),
                        "bin",
                        System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java")
                .toString();
        assertThrows(UncheckedIOException.class, () -> {
            try (var recorder = new GameplayRecorder(new RecordingOptions(output, 1, 18, executable))) {
                recorder.captureFrame(new byte[160 * 144 * 4]);
            }
        });
        assertFalse(Files.exists(output));
        try (var files = Files.list(directory)) {
            assertEquals(0, files.count());
        }
    }

    private static String probe(Path output, String selector, String entries) throws Exception {
        return run(List.of(
                "ffprobe",
                "-v",
                "error",
                "-select_streams",
                selector,
                "-show_entries",
                "stream=" + entries,
                "-of",
                "default=noprint_wrappers=1",
                output.toString()));
    }

    private static String run(List<String> command) throws Exception {
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String result = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, p.waitFor(), result);
        return result;
    }
}
