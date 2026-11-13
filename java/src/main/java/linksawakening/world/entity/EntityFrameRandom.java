package linksawakening.world.entity;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** GetRandomByte source and frame latch shared by entity ticks and sword responses. */
final class EntityFrameRandom {
    /** Spawn operations that historically required an explicitly configured room RNG. */
    static final IntSupplier REQUIRED_SOURCE = () -> {
        throw new IllegalStateException("Shovel drops require a ROM random-byte supplier");
    };

    private static final IntConsumer EXTERNALLY_CLOCKED = frame -> {};
    private final IntSupplier source;
    private final IntSupplier spawnSource;
    private final IntConsumer beginFrame;

    EntityFrameRandom(IntSupplier configured) {
        if (configured == null) {
            RomRandomByteSource fallback = new RomRandomByteSource();
            source = fallback;
            spawnSource = REQUIRED_SOURCE;
            beginFrame = frame -> fallback.beginFrame(frame & 0xFF, 0);
        } else {
            source = spawnSource = configured;
            beginFrame = EXTERNALLY_CLOCKED;
        }
    }

    IntSupplier spawnSource() {
        return spawnSource;
    }

    IntSupplier forFrame(int frameCounter) {
        beginFrame.accept(frameCounter);
        return source;
    }
}
