package linksawakening.audio.openal;

import java.util.*;

/** Models AL queue semantics, notably STOPPED treating newly queued buffers as processed. */
final class FakePcmAlDevice implements PcmAlDevice {
    final ArrayDeque<Integer> buffers = new ArrayDeque<>();
    final Set<Integer> allocated = new HashSet<>();
    final Map<Integer, Integer> deletes = new HashMap<>();
    int state = INITIAL, consumed, nextId, plays, stops, rewinds, resets;
    boolean stopDuringQueue, connected = true, closed;
    String failOnce, failAlways;
    int processedQueries;

    private void fail(String operation) {
        if (operation.equals(failAlways) || operation.equals(failOnce)) {
            failOnce = null;
            throw new IllegalStateException("injected AL failure: " + operation);
        }
    }

    @Override
    public void makeCurrent() {
        fail("current");
    }

    @Override
    public int state() {
        fail("state");
        return state;
    }

    @Override
    public int queued() {
        return buffers.size();
    }

    @Override
    public int processed() {
        processedQueries++;
        fail("processed");
        return state == STOPPED ? buffers.size() : state == INITIAL ? 0 : consumed;
    }

    @Override
    public int createBuffer(short[] pcm, int rate) {
        fail("create");
        allocated.add(++nextId);
        return nextId;
    }

    @Override
    public void queue(int buffer) {
        fail("queue");
        buffers.addLast(buffer);
        fail("queueAfter");
        if (stopDuringQueue) {
            stopDuringQueue = false;
            state = STOPPED;
        }
    }

    @Override
    public int unqueue() {
        fail("unqueue");
        if (processed() == 0) throw new IllegalStateException("AL_INVALID_VALUE: no processed buffers");
        consumed = Math.max(0, consumed - 1);
        return buffers.removeFirst();
    }

    @Override
    public void deleteBuffer(int buffer) {
        fail("delete");
        if (buffers.contains(buffer)) throw new IllegalStateException("AL_INVALID_OPERATION: buffer still queued");
        if (allocated.remove(buffer)) deletes.merge(buffer, 1, Integer::sum);
    }

    @Override
    public void play() {
        fail("play");
        plays++;
        state = PLAYING;
        consumed = 0;
    }

    @Override
    public void stop() {
        stops++;
        if (state != INITIAL) state = STOPPED;
    }

    @Override
    public void rewind() {
        rewinds++;
        state = INITIAL;
        consumed = 0;
    }

    @Override
    public void resetSource() {
        fail("reset");
        resets++;
        state = INITIAL;
        consumed = 0;
        buffers.clear();
    }

    @Override
    public boolean connected() {
        return connected;
    }

    @Override
    public void close() {
        closed = true;
        buffers.clear();
        allocated.clear();
    }

    void consume(int count) {
        consumed += count;
        if (consumed >= buffers.size()) state = STOPPED;
    }
}
