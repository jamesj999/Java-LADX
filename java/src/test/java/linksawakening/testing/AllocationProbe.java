package linksawakening.testing;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;

/**
 * Measures steady-state heap allocation of a repeated action on the calling thread.
 *
 * <p>Reports the quietest of several measurement windows: JIT compilation and deoptimisation in a
 * busy test JVM add one-off allocations to individual windows, while a real per-iteration
 * allocation shows up in every window.
 */
public final class AllocationProbe {
    private static final int WINDOWS = 5;

    private AllocationProbe() {}

    public static double bytesPerIteration(int warmUpIterations, int iterationsPerWindow, Runnable action) {
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) {
            throw new AssertionError("Thread allocation measurement is not supported on this JVM");
        }
        bean.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        for (int i = 0; i < warmUpIterations; i++) action.run();
        double quietest = Double.MAX_VALUE;
        for (int window = 0; window < WINDOWS; window++) {
            long before = bean.getThreadAllocatedBytes(thread);
            for (int i = 0; i < iterationsPerWindow; i++) action.run();
            long allocated = bean.getThreadAllocatedBytes(thread) - before;
            quietest = Math.min(quietest, allocated / (double) iterationsPerWindow);
        }
        return quietest;
    }
}
