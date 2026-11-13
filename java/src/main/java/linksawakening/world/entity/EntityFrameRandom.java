package linksawakening.world.entity;

import java.util.function.IntSupplier;

/** GetRandomByte source and frame latch shared by entity ticks and sword responses. */
final class EntityFrameRandom {
    // An absent configured source selects the room-owned ROM-compatible fallback.
    private final IntSupplier configured;
    private final RomRandomByteSource fallback;

    EntityFrameRandom(IntSupplier configured) {
        this.configured = configured;
        this.fallback = new RomRandomByteSource();
    }

    IntSupplier forFrame(int frameCounter) {
        if (configured != null) return configured;
        fallback.beginFrame(frameCounter & 0xFF, 0);
        return fallback;
    }
}
