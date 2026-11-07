package linksawakening.audio;

import linksawakening.app.GameSession;
import linksawakening.testing.RequiresRom;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@RequiresRom
class IntroSessionAudioTest {
    @Test void productionIntroEmitsPcmToInjectedHostOutput() {
        int[] calls = {0};
        long[] samples = {0};
        boolean[] audible = {false};
        GameSession session = new GameSession((pcm, rate) -> {
            assertEquals(48000, rate);
            assertEquals(0, pcm.length % 2);
            calls[0]++;
            samples[0] += pcm.length / 2;
            for (short sample : pcm) if (sample != 0) audible[0] = true;
        });
        session.initializeHeadlessIntro();
        for (int i = 0; i < 1450; i++) session.headlessFrame();
        assertEquals(1450, calls[0]);
        assertEquals(1_160_000, samples[0]);
        assertTrue(audible[0], "Production intro must reach the supplied PCM sink");
        assertThrows(IllegalStateException.class, session::initializeHeadlessIntro);
    }
}
