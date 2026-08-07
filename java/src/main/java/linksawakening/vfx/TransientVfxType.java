package linksawakening.vfx;

/**
 * Supported transient gameplay VFX types.
 *
 * <p>Keep this small and ROM-oriented so the runtime can grow additional
 * short-lived effects without changing the core slot model.
 */
public enum TransientVfxType {

    BUSH_LEAVES(0x00, 0x1F),
    WATER_SPLASH(0x01, 0x0F),
    POOF(0x02, 0x0F),
    SWORD_POKE(0x05, 0x0F),
    LASER_BEAM(0x06, 0x10),
    SMOKE(0x08, 0x0F),
    SWORD_BEAM(0x0D, 0x08);

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
