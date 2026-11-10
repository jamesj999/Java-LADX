package linksawakening.audio.openal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

final class DialogPcmBufferQueue {

    private final int maxBuffers;
    private final java.util.Map<Integer, Integer> frames = new java.util.HashMap<>();
    private final ArrayDeque<Integer> liveBuffers = new ArrayDeque<>();

    DialogPcmBufferQueue(int maxBuffers) {
        if (maxBuffers <= 0) {
            throw new IllegalArgumentException("maxBuffers must be positive");
        }
        this.maxBuffers = maxBuffers;
    }

    void trackQueued(int buffer) { trackQueued(buffer, 0); }

    void trackQueued(int buffer, int frameCount) {
        liveBuffers.addLast(buffer);
        frames.put(buffer, frameCount);
    }

    boolean isFull() { return liveBuffers.size() >= maxBuffers; }

    int queuedFrames() { return frames.values().stream().mapToInt(Integer::intValue).sum(); }

    void markDeleted(int buffer) {
        liveBuffers.remove(buffer);
        frames.remove(buffer);
    }

    List<Integer> buffersToDeleteOnClose() {
        List<Integer> buffers = new ArrayList<>(liveBuffers);
        liveBuffers.clear();
        frames.clear();
        return buffers;
    }

    List<Integer> liveBuffers() {
        return List.copyOf(liveBuffers);
    }
}
