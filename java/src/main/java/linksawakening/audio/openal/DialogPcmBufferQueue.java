package linksawakening.audio.openal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Tracks native buffer ownership and frame counts without boxing during playback. */
final class DialogPcmBufferQueue {
    private final int maxBuffers;
    private int[] ids;
    private int[] frames;
    private int size;

    DialogPcmBufferQueue(int maxBuffers) {
        if (maxBuffers <= 0) throw new IllegalArgumentException("maxBuffers must be positive");
        this.maxBuffers = maxBuffers;
        ids = new int[maxBuffers];
        frames = new int[maxBuffers];
    }

    void trackQueued(int buffer) {
        trackQueued(buffer, 0);
    }

    void trackQueued(int buffer, int count) {
        if (size == ids.length) {
            ids = Arrays.copyOf(ids, size * 2);
            frames = Arrays.copyOf(frames, size * 2);
        }
        ids[size] = buffer;
        frames[size++] = count;
    }

    boolean isFull() {
        return size >= maxBuffers;
    }

    int queuedFrames() {
        int sum = 0;
        for (int i = 0; i < size; i++) sum += frames[i];
        return sum;
    }

    void markDeleted(int buffer) {
        for (int i = 0; i < size; i++)
            if (ids[i] == buffer) {
                System.arraycopy(ids, i + 1, ids, i, size - i - 1);
                System.arraycopy(frames, i + 1, frames, i, size - i - 1);
                size--;
                return;
            }
    }

    void clear() {
        size = 0;
    }

    List<Integer> buffersToDeleteOnClose() {
        List<Integer> result = liveBuffers();
        clear();
        return result;
    }

    List<Integer> liveBuffers() {
        List<Integer> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(ids[i]);
        return List.copyOf(result);
    }
}
