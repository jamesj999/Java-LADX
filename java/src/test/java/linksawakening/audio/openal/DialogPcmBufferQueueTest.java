package linksawakening.audio.openal;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DialogPcmBufferQueueTest {

    @Test
    void trimsOldestQueuedBuffersWhileSourceIsPlaying() {
        DialogPcmBufferQueue queue = new DialogPcmBufferQueue(3);
        queue.trackQueued(10);
        queue.trackQueued(11);
        queue.trackQueued(12);

        assertEquals(List.of(10), queue.buffersToDropBeforeQueueing());
        queue.trackQueued(13);

        assertEquals(List.of(11, 12, 13), queue.liveBuffers());
    }

    @Test
    void removesProcessedBuffersBeforeDelete() {
        DialogPcmBufferQueue queue = new DialogPcmBufferQueue(3);
        queue.trackQueued(10);
        queue.trackQueued(11);
        queue.trackQueued(12);

        queue.markDeleted(11);

        assertEquals(List.of(10, 12), queue.liveBuffers());
    }

    @Test
    void closeDeletesEachTrackedBufferOnce() {
        DialogPcmBufferQueue queue = new DialogPcmBufferQueue(3);
        queue.trackQueued(10);
        queue.trackQueued(11);
        List<Integer> deleted = new ArrayList<>();

        for (int buffer : queue.buffersToDeleteOnClose()) {
            deleted.add(buffer);
        }
        for (int buffer : queue.buffersToDeleteOnClose()) {
            deleted.add(buffer);
        }

        assertEquals(List.of(10, 11), deleted);
        assertEquals(List.of(), queue.liveBuffers());
    }

    @Test
    void generatedBufferIsDeletedOnceIfFailureHappensBeforeTracking() {
        PendingOpenAlBuffer pending = new PendingOpenAlBuffer(42);

        assertEquals(42, pending.releaseUntracked());
        assertEquals(0, pending.releaseUntracked());
    }

    @Test
    void generatedBufferIsNotDeletedByFailureCleanupAfterTracking() {
        PendingOpenAlBuffer pending = new PendingOpenAlBuffer(42);

        pending.markTracked();

        assertEquals(0, pending.releaseUntracked());
    }
}
