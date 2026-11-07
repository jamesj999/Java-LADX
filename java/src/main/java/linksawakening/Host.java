package linksawakening;

import java.util.function.BiConsumer;

/** Native lifecycle boundary; game frames do not depend on a window. */
public interface Host extends AutoCloseable {
    void open(BiConsumer<Integer, Integer> keys);
    boolean shouldClose();
    void pollEvents();
    void present(byte[] pixels);
    default void releasePresentation() {}
    @Override void close();
}
