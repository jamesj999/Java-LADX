package linksawakening.app.host;

import java.util.ArrayDeque;

/** Startup ownership chain; cleanup is idempotent, ordered and best effort. */
final class HostResources implements AutoCloseable {
    private final ArrayDeque<Runnable> cleanup = new ArrayDeque<>();
    private boolean closed;

    void add(Runnable action) {
        if (closed) throw new IllegalStateException("Host resources already closed");
        cleanup.addFirst(action);
    }

    void initialize(Runnable startup) {
        try {
            startup.run();
        } catch (RuntimeException | Error failure) {
            try {
                close();
            } catch (RuntimeException | Error cleanup) {
                if (cleanup != failure) failure.addSuppressed(cleanup);
            }
            throw failure;
        }
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        Throwable first = null;
        while (!cleanup.isEmpty())
            try {
                cleanup.removeFirst().run();
            } catch (RuntimeException | Error failure) {
                if (first == null) first = failure;
                else if (first != failure) first.addSuppressed(failure);
            }
        if (first instanceof Error error) throw error;
        if (first != null) throw (RuntimeException) first;
    }
}
