package linksawakening.app;

import static linksawakening.input.KeyCodes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import linksawakening.recording.RecordingOptions;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@RequiresRom
class RecordingShortcutTest {
    @TempDir
    Path directory;

    @Test
    void toggleStopsAndRestartsWithFreshFilesAndIgnoresRepeatAndRelease() throws Exception {
        requireEncoder();
        try (var rom = TestRom.open()) {
            assertTrue(rom.read() >= 0);
        }
        Path first = directory.resolve("first.mp4");
        try (var game = new GameSession()) {
            game.configureRecording(RecordingOptions.defaults(first));
            game.initializeHeadlessNewGame();
            for (int i = 0; i < 12; i++) game.headlessFrame();
            game.onKeyEvent(KEY_F9, PRESS);
            assertNull(game.recorder);
            assertTrue(Files.size(first) > 0);
            game.onKeyEvent(KEY_F9, REPEAT);
            game.onKeyEvent(KEY_F9, RELEASE);
            assertNull(game.recorder);
            game.headlessFrame(); // Detached audio must not write to the closed recorder.
            game.onKeyEvent(KEY_F9, PRESS);
            var restarted = game.recorder;
            assertNotNull(restarted);
            game.onKeyEvent(KEY_F9, REPEAT);
            game.onKeyEvent(KEY_F9, RELEASE);
            assertSame(restarted, game.recorder);
            for (int i = 0; i < 18; i++) game.headlessFrame();
            assertEquals(18, restarted.frameCount());
            // A recording remains stoppable after moving to another screen.
            game.router().startTitleScreenWithoutIntro();
            game.onKeyEvent(KEY_F9, PRESS);
            assertNull(game.recorder);
            game.onKeyEvent(KEY_F9, PRESS);
            assertNull(game.recorder, "Do not start new recordings outside gameplay");
            game.headlessFrame();
        }
        try (var paths = Files.list(directory)) {
            var files = paths.toList();
            assertEquals(2, files.size());
            for (var file : files) assertTrue(Files.size(file) > 0);
        }
    }

    private static void requireEncoder() throws InterruptedException {
        try {
            assumeTrue(new ProcessBuilder("ffmpeg", "-version")
                            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                            .redirectError(ProcessBuilder.Redirect.DISCARD)
                            .start()
                            .waitFor()
                    == 0);
        } catch (IOException e) {
            assumeTrue(false, "FFmpeg is not installed");
        }
    }
}
