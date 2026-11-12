package linksawakening.recording;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import linksawakening.app.HeadlessGameDriver;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@RequiresRom
class SessionRecordingTest {
    @TempDir
    Path directory;

    @Test
    void recordingPreservesEveryFramebufferAndGameplayCheckpoint() throws Exception {
        GameplayRecorderTest.requireEncoder();
        try (var rom = TestRom.open()) {
            assertTrue(rom.read() >= 0);
        }
        Path output = directory.resolve("session.mp4");
        try (var reference = new HeadlessGameDriver();
                var recorded = new HeadlessGameDriver(RecordingOptions.defaults(output))) {
            reference.initializeHeadlessNewGame();
            recorded.initializeHeadlessNewGame();
            for (int frame = 0; frame < 660; frame++) {
                if (frame > 400) {
                    reference.headlessButton("a", frame % 24 < 8);
                    recorded.headlessButton("a", frame % 24 < 8);
                }
                assertArrayEquals(reference.headlessFrame(), recorded.headlessFrame(), "frame " + frame);
                assertEquals(reference.headlessState(), recorded.headlessState(), "state " + frame);
            }
        }
        assertTrue(Files.size(output) > 0);
    }
}
