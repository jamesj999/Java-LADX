package linksawakening.audio.openal;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OpenAlPcmSoundOutputTest {
    private static void frame(OpenAlPcmSoundOutput output) {
        output.playFrame(new short[1600], 48000);
    }

    private static void prefill(OpenAlPcmSoundOutput output) {
        for (int i = 0; i < 3; i++) frame(output);
    }

    @Test
    void underrunRewindsBeforePrefillSoNewBuffersAreNotDiscardedAsProcessed() {
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

    @Test
    void sourceDrainingDuringQueueSubmissionAlsoRecovers() {
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

    @Test
    void steadyStateDoesNotClearRewindOrRestartPlayingSource() {
        var device = new FakePcmAlDevice();
        try (var output = new OpenAlPcmSoundOutput(device)) {
            prefill(output);
            for (int i = 0; i < 120; i++) {
                device.consume(1);
                frame(output);
            }
            assertEquals(0, output.underrunCount());
            assertEquals(0, device.rewinds);
            assertEquals(0, device.stops);
            assertEquals(1, device.plays, "alSourcePlay on a playing source would restart its waveform");
            assertEquals(3, device.queued());
        }
        assertTrue(device.allocated.isEmpty());
        assertTrue(device.deletes.values().stream().allMatch(count -> count == 1));
    }

    @Test
    void pausedSourceResumesWhenItsQueueIsReady() {
        var device = new FakePcmAlDevice();
        try (var output = new OpenAlPcmSoundOutput(device)) {
            prefill(output);
            device.state = PcmAlDevice.PAUSED;
            frame(output);
            assertEquals(PcmAlDevice.PLAYING, device.state);
            assertEquals(0, device.stops);
        }
    }

    @Test
    void fullQueueRebuffersWithoutWaitingOrLosingTheInterruptFlag() {
        var device = new FakePcmAlDevice();
        try (var output = new OpenAlPcmSoundOutput(device)) {
            for (int i = 0; i < 8; i++) frame(output);
            int queries = device.processedQueries;
            Thread.currentThread().interrupt();
            try {
                assertDoesNotThrow(() -> frame(output));
                assertTrue(Thread.currentThread().isInterrupted());
            } finally {
                Thread.interrupted();
            }
            assertTrue(output.isAvailable());
            assertNull(output.lastFailure(), "queue pressure is not an AL exception");
            assertEquals(1, output.backpressureCount());
            assertTrue(device.processedQueries - queries < 10, "no polling/spin wait on the render thread");
            assertEquals(PcmAlDevice.INITIAL, device.state);
            assertEquals(1, device.queued());
            frame(output);
            frame(output);
            assertEquals(PcmAlDevice.PLAYING, device.state);
        }
        assertTrue(device.allocated.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"current", "processed", "create", "queue", "queueAfter", "play", "state"})
    void transientAlFailureRecreatesSourceAndResumesAfterPrefill(String operation) {
        var device = new FakePcmAlDevice();
        try (var output = new OpenAlPcmSoundOutput(device)) {
            if (operation.equals("play")) {
                frame(output);
                frame(output);
            }
            device.failOnce = operation;
            assertDoesNotThrow(() -> frame(output));
            assertTrue(output.isAvailable());
            assertNotNull(output.lastFailure());
            assertEquals(1, output.recoveryCount());
            assertTrue(device.allocated.isEmpty(), "including buffers submitted before queue() failed");
            prefill(output);
            assertEquals(PcmAlDevice.PLAYING, device.state);
            assertEquals(3, device.queued());
        }
        assertTrue(device.deletes.values().stream().allMatch(count -> count == 1));
    }

    @Test
    void failedRecoveryRetriesOnLaterFramesWithoutDisablingTheOutput() {
        var device = new FakePcmAlDevice();
        try (var output = new OpenAlPcmSoundOutput(device)) {
            device.failOnce = "queueAfter";
            device.failAlways = "reset";
            frame(output);
            assertTrue(output.isAvailable());
            assertTrue(output.statusMessage().contains("pending"));
            frame(output);
            assertTrue(output.isAvailable());
            device.failAlways = null;
            prefill(output);
            assertEquals(PcmAlDevice.PLAYING, device.state);
            assertEquals(3, device.allocated.size());
        }
    }

    @Test
    void deleteAndUnqueueFailuresRecoverWithoutLeakingBuffers() {
        for (String operation : new String[] {"delete", "unqueue"}) {
            var device = new FakePcmAlDevice();
            try (var output = new OpenAlPcmSoundOutput(device)) {
                prefill(output);
                device.consume(1);
                device.failOnce = operation;
                frame(output);
                assertTrue(output.isAvailable());
                prefill(output);
                assertEquals(PcmAlDevice.PLAYING, device.state);
                assertEquals(3, device.allocated.size());
            }
            assertTrue(device.allocated.isEmpty());
        }
    }

    @Test
    void initialPrefillCanBeReplacedOrClosedWithoutUnqueueingPendingBuffers() {
        var device = new FakePcmAlDevice();
        try (var output = new OpenAlPcmSoundOutput(device)) {
            frame(output);
            assertEquals(PcmAlDevice.INITIAL, device.state);
            output.playReplacing(new short[160], 48000);
            assertTrue(output.isAvailable());
            assertEquals(PcmAlDevice.PLAYING, device.state);
            assertEquals(1, device.queued());
        }
        var initial = new FakePcmAlDevice();
        var output = new OpenAlPcmSoundOutput(initial);
        frame(output);
        assertDoesNotThrow(output::close);
        assertTrue(initial.allocated.isEmpty());
    }

    @Test
    void confirmedDeviceDisconnectionIsUnavailableButOrdinaryAlErrorsAreNot() {
        var device = new FakePcmAlDevice();
        try (var output = new OpenAlPcmSoundOutput(device)) {
            prefill(output);
            device.connected = false;
            frame(output);
            assertFalse(output.isAvailable());
            assertTrue(output.statusMessage().contains("disconnected"));
        }
    }

    @Test
    void diagnosticQueryFailureCannotMaskPlaybackErrorOrEscape() {
        var device = new FakePcmAlDevice();
        try (var output = new OpenAlPcmSoundOutput(device)) {
            device.failAlways = "state";
            assertDoesNotThrow(() -> frame(output));
            assertDoesNotThrow(output::diagnostics);
            assertTrue(output.isAvailable());
            device.failAlways = null;
            prefill(output);
            assertEquals(PcmAlDevice.PLAYING, device.state);
        }
    }
}
