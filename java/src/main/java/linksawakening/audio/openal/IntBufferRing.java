package linksawakening.audio.openal;
/** Fixed-capacity FIFO of native buffer identifiers. */
final class IntBufferRing {
    private final int[] values;
    private int head;
    private int size;

    IntBufferRing(int capacity) {
        values = new int[capacity];
    }

    boolean isEmpty() {
        return size == 0;
    }

    void addLast(int value) {
        if (size == values.length) throw new IllegalStateException("buffer pool overflow");
        values[(head + size) % values.length] = value;
        size++;
    }

    int removeFirst() {
        if (size == 0) throw new IllegalStateException("empty buffer pool");
        int value = values[head];
        head = (head + 1) % values.length;
        size--;
        return value;
    }

    void clear() {
        head = size = 0;
    }
}
