package linksawakening.app;

import java.util.concurrent.locks.LockSupport;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

/** Monotonic rational 60 Hz deadlines. Presentation time counts toward each frame's budget. */
final class FramePacer {
    private static final long PERIOD = 1_000_000_000L / 60;
    private static final int REMAINDER = (int) (1_000_000_000L % 60);
    private static final long SPIN_NANOS = 100_000;
    private final LongSupplier clock;
    private final LongConsumer park;
    private final Runnable spin;
    private long deadline;
    private int fraction;

    FramePacer() {
        this(System::nanoTime, LockSupport::parkNanos, Thread::onSpinWait);
    }

    FramePacer(LongSupplier clock, LongConsumer park, Runnable spin) {
        this.clock = clock;
        this.park = park;
        this.spin = spin;
        deadline = clock.getAsLong();
    }

    /** False means interrupted; never consumes the interrupt. Overruns preserve pending deadlines. */
    boolean awaitNextFrame() {
        if (Thread.currentThread().isInterrupted()) return false;
        deadline += PERIOD;
        fraction += REMAINDER;
        if (fraction >= 60) {
            deadline++;
            fraction -= 60;
        }
        while (true) {
            if (Thread.currentThread().isInterrupted()) return false;
            long remaining = deadline - clock.getAsLong();
            if (remaining <= 0) return true;
            if (remaining > SPIN_NANOS) park.accept(remaining - SPIN_NANOS);
            else spin.run();
        }
    }
}
