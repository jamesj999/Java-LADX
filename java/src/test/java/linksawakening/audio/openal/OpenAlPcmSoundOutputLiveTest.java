package linksawakening.audio.openal;

import linksawakening.app.GameSession;
import linksawakening.gameplay.PcmSoundOutput;
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
            long[] peakCallNanos = new long[2];
            var session = new GameSession(new PcmSoundOutput() {
                @Override public void play(short[] pcm, int rate) { output.play(pcm, rate); }
                @Override public void playFrame(short[] pcm, int rate) {
                    long pressure = output.backpressureCount();
                    long start = System.nanoTime();
                    try { output.playFrame(pcm, rate); }
                    finally {
                        long elapsed = System.nanoTime() - start;
                        peakCallNanos[0] = Math.max(peakCallNanos[0], elapsed);
                        if (output.backpressureCount() != pressure) peakCallNanos[1] = Math.max(peakCallNanos[1], elapsed);
                    }
                }
            });
            if (intro) session.initializeHeadlessIntro(); else session.initializeHeadlessNewGame();
            long nextFrame = System.nanoTime();
            for (int frame = 0; frame < 360; frame++) {
                // Exercise the run-loop's interrupt re-set for twenty frames, then
                // release the injected flag so the rest of this test is paced again.
                if (frame == 200 && "1".equals(System.getenv("LA_AUDIO_INTERRUPT"))) {
                    assertTrue(Thread.interrupted(), "output must preserve the injected interrupt");
                }
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
            System.out.printf("LIVE max PCM call %.3f ms, max pressure resync %.3f ms%n",
                    peakCallNanos[0] / 1_000_000.0, peakCallNanos[1] / 1_000_000.0);
        } finally {
            Thread.interrupted(); // Do not leak the deliberate interrupt to JUnit.
        }
    }
}
