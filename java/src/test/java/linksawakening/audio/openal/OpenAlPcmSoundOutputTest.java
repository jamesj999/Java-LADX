package linksawakening.audio.openal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OpenAlPcmSoundOutputTest {
    private static void frame(OpenAlPcmSoundOutput output) { output.playFrame(new short[1600], 48000); }
    private static void prefill(OpenAlPcmSoundOutput output) { for (int i = 0; i < 3; i++) frame(output); }

    @Test void underrunRewindsBeforePrefillSoNewBuffersAreNotDiscardedAsProcessed() {
        var device = new FakePcmAlDevice();
        try (var output = new OpenAlPcmSoundOutput(device)) {
            prefill(output);
            assertEquals(PcmAlDevice.PLAYING, device.state);
            device.consume(3); // A slow present/GC drains the entire native queue.
            frame(output);
            assertEquals(PcmAlDevice.INITIAL, device.state);
            assertEquals(0, device.processed());
            frame(output);
            assertEquals(2, device.queued());
            frame(output);
            assertEquals(PcmAlDevice.PLAYING, device.state);
            assertEquals(1, output.underrunCount());
            assertEquals(1, device.rewinds);
            assertEquals(2, device.plays);
            assertTrue(output.isAvailable());
        }
    }

    @Test void sourceDrainingDuringQueueSubmissionAlsoRecovers() {
        var device = new FakePcmAlDevice();
        try (var output = new OpenAlPcmSoundOutput(device)) {
            prefill(output);
            device.consume(2);
            device.stopDuringQueue = true;
            frame(output);
            assertEquals(PcmAlDevice.INITIAL, device.state);
            assertEquals(0, device.processed());
            frame(output);
            assertEquals(PcmAlDevice.PLAYING, device.state);
            assertEquals(1, output.underrunCount());
        }
    }

    @Test void steadyStateDoesNotClearRewindOrRestartPlayingSource() {
        var device = new FakePcmAlDevice();
        try (var output = new OpenAlPcmSoundOutput(device)) {
            prefill(output);
            for (int i = 0; i < 120; i++) { device.consume(1); frame(output); }
            assertEquals(0, output.underrunCount());
            assertEquals(0, device.rewinds);
            assertEquals(0, device.stops);
            assertEquals(1, device.plays, "alSourcePlay on a playing source would restart its waveform");
            assertEquals(3, device.queued());
        }
        assertTrue(device.allocated.isEmpty());
        assertTrue(device.deletes.values().stream().allMatch(count -> count == 1));
    }

    @Test void pausedSourceResumesWhenItsQueueIsReady() {
        var device = new FakePcmAlDevice();
        try (var output = new OpenAlPcmSoundOutput(device)) {
            prefill(output);
            device.state = PcmAlDevice.PAUSED;
            frame(output);
            assertEquals(PcmAlDevice.PLAYING, device.state);
            assertEquals(0, device.stops);
        }
    }
}
