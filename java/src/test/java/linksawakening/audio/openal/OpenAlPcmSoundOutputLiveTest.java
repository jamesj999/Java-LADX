package linksawakening.audio.openal;

import linksawakening.app.GameSession;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in native audio only. No window, screenshots, recording, or framebuffer exports. */
@linksawakening.testing.RequiresRom
@EnabledIfEnvironmentVariable(named = "LA_LIVE_AUDIO", matches = "1")
class OpenAlPcmSoundOutputLiveTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void headlessSessionSurvivesDesktopPacingStallsBurstsAndInterrupts(boolean intro) throws Exception {
        try (var output = new OpenAlPcmSoundOutput()) {
            assertTrue(output.isAvailable(), output.statusMessage());
            var session = new GameSession(output);
            if (intro) session.initializeHeadlessIntro(); else session.initializeHeadlessNewGame();
            long nextFrame = System.nanoTime();
            for (int frame = 0; frame < 360; frame++) {
                if (!intro && (frame == 200 || frame == 280)) session.headlessButton("start", true);
                if (!intro && (frame == 201 || frame == 281)) session.headlessButton("start", false);
                if (frame == 120) Thread.sleep(200); // Slow present/GC.
                if (frame == 180 && "1".equals(System.getenv("LA_AUDIO_INTERRUPT"))) {
                    Thread.currentThread().interrupt();
                    for (int burst = 0; burst < 24; burst++) session.headlessFrame();
                }
                session.headlessFrame();
                if (frame % 60 == 0 || !output.isAvailable()) {
                    System.out.printf("LIVE intro=%s frame=%d %s%n", intro, frame, output.diagnostics());
                }
                assertTrue(output.isAvailable(), output.diagnostics());
                if (frame == 140 || frame == 340) assertEquals(PcmAlDevice.PLAYING, output.sourceState(), output.diagnostics());
                nextFrame += 1_000_000_000L / 60;
                long sleep = nextFrame - System.nanoTime();
                if (sleep > 0) {
                    try { Thread.sleep(sleep / 1_000_000, (int) (sleep % 1_000_000)); }
                    catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                } else nextFrame = System.nanoTime();
            }
            System.out.println("LIVE final " + output.diagnostics());
        } finally {
            Thread.interrupted(); // Do not leak the deliberate interrupt to JUnit.
        }
    }
}
