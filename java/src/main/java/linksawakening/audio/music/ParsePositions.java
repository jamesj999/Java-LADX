package linksawakening.audio.music;

import java.util.Arrays;

/** Reusable exact tuple set for zero-time command-cycle detection. */
final class ParsePositions {
    private int[] values = new int[256];
    private int size;

    void clear() {
        size = 0;
    }

    boolean add(int stream, int program, int loop, int remaining) {
        for (int i = 0; i < size; i += 4)
            if (values[i] == stream && values[i + 1] == program && values[i + 2] == loop && values[i + 3] == remaining)
                return false;
        if (size == values.length) values = Arrays.copyOf(values, values.length * 2);
        values[size++] = stream;
        values[size++] = program;
        values[size++] = loop;
        values[size++] = remaining;
        return true;
    }
}
