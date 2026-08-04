package linksawakening.world;

import java.util.function.IntSupplier;

/**
 * Targeted Java port of bank-0 {@code GetRandomByte}, without emulating the
 * rest of the Game Boy hardware. Call {@link #beginFrame(int, int)} once per
 * game frame before consuming values for that frame.
 */
public final class RomRandomByteSource implements IntSupplier {
    private static final int INITIAL_SEED = 0xA2;

    private final int initialSeed;
    private int frameCounter;
    private int scanline;
    private int randomSeed;

    public RomRandomByteSource() {
        this(INITIAL_SEED);
    }

    public RomRandomByteSource(int initialSeed) {
        if ((initialSeed & ~0xFF) != 0) {
            throw new IllegalArgumentException("Random seed must be an unsigned byte");
        }
        this.initialSeed = initialSeed;
        this.randomSeed = initialSeed;
    }

    /**
     * Supplies the values visible to GetRandomByte for the current frame.
     * The scanline is the renderer's explicit policy for the hardware rLY
     * input; callers that have a more precise display phase can provide it.
     */
    public void beginFrame(int frameCounter, int scanline) {
        if ((frameCounter & ~0xFF) != 0) {
            throw new IllegalArgumentException("Frame counter must be an unsigned byte");
        }
        if ((scanline & ~0xFF) != 0) {
            throw new IllegalArgumentException("Scanline must be an unsigned byte");
        }
        this.frameCounter = frameCounter;
        this.scanline = scanline;
    }

    /** Restores the startup seed while retaining the current frame inputs. */
    public void reset() {
        randomSeed = initialSeed;
    }

    @Override
    public int getAsInt() {
        int sum = (frameCounter + randomSeed + scanline) & 0xFF;
        randomSeed = ((sum >>> 1) | ((sum & 0x01) << 7)) & 0xFF;
        return randomSeed;
    }
}
