package linksawakening;

import java.util.function.BiConsumer;

/** Window-free host used by replay sessions. */
public final class HeadlessHost implements Host {
    @Override public void open(BiConsumer<Integer, Integer> keys) {}
    @Override public boolean shouldClose() { return false; }
    @Override public void pollEvents() {}
    @Override public void present(byte[] pixels) {}
    @Override public void close() {}
}
