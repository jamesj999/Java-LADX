package linksawakening.audio.openal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

final class DialogPcmBufferQueue {

    private final int maxBuffers;
    private final ArrayDeque<Integer> liveBuffers = new ArrayDeque<>();

    DialogPcmBufferQueue(int maxBuffers) {
        if (maxBuffers <= 0) {
            throw new IllegalArgumentException("maxBuffers must be positive");
        }
        this.maxBuffers = maxBuffers;
    }

    void trackQueued(int buffer) {
        liveBuffers.addLast(buffer);
    }

    List<Integer> buffersToDropBeforeQueueing() {
        List<Integer> buffers = new ArrayList<>();
        while (liveBuffers.size() >= maxBuffers) {
            buffers.add(liveBuffers.removeFirst());
        }
        return buffers;
    }

    void markDeleted(int buffer) {
        liveBuffers.remove(buffer);
    }

    List<Integer> buffersToDeleteOnClose() {
        List<Integer> buffers = new ArrayList<>(liveBuffers);
        liveBuffers.clear();
        return buffers;
    }

    List<Integer> liveBuffers() {
        return List.copyOf(liveBuffers);
    }
}
