package linksawakening.vfx;

/**
 * Supported transient gameplay VFX types.
 *
 * <p>Keep this small and ROM-oriented so the runtime can grow additional
 * short-lived effects without changing the core slot model.
 */
public enum TransientVfxType {

    BUSH_LEAVES(0x00, 0x1F);

    private final int id;
    private final int defaultCountdown;

    TransientVfxType(int id, int defaultCountdown) {
        this.id = id;
        this.defaultCountdown = defaultCountdown;
    }

    public int id() {
        return id;
    }

    public int defaultCountdown() {
        return defaultCountdown;
    }
}
