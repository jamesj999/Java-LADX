package linksawakening.world;

import java.util.OptionalInt;

/**
 * Pure countdown decisions for the ordinary bank-$03 bomb handler.
 *
 * <p>The room runtime owns the per-frame countdown storage and decrement. The
 * only countdown write represented here is the handler's one-shot {@code
 * 0x18 -> 0x17} transition that accompanies the explosion sound.</p>
 */
final class BombMotion {
    static final int INITIAL_COUNTDOWN = 0xA0;

    private BombMotion() {
    }

    private static final int WARNING_START = 0x21;
    private static final int EXPLOSION_SOUND_COUNTDOWN = 0x18;

    /** Exact bank-$03 ExplosionSpriteVariantFrames, indexed by countdown. */
    private static final int[] EXPLOSION_VARIANT_FRAMES = {
        0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 1, 1, 1, 1, 1, 1,
        2, 2, 2, 2,
        3, 3, 3, 3
    };

    enum Phase {
        NORMAL,
        WARNING,
        EXPLOSION
    }

    record Decision(Phase phase, OptionalInt explosionVariant, boolean playExplosionSound,
                    OptionalInt countdownOverride, boolean unloadAfterPresentation) {
    }

    /**
     * Selects the source presentation and transition decision for one unsigned
     * transition-countdown value. Counts above the ordinary initial fuse stay
     * in the normal source path, but never enter the explosion table lookup.
     */
    static Decision decide(int countdown) {
        validateUnsignedCountdown(countdown);

        if (countdown < EXPLOSION_SOUND_COUNTDOWN) {
            return new Decision(
                Phase.EXPLOSION,
                OptionalInt.of(EXPLOSION_VARIANT_FRAMES[countdown]),
                false,
                OptionalInt.empty(),
                countdown == 0);
        }

        if (countdown <= WARNING_START) {
            boolean startsExplosion = countdown == EXPLOSION_SOUND_COUNTDOWN;
            return new Decision(
                Phase.WARNING,
                OptionalInt.empty(),
                startsExplosion,
                startsExplosion ? OptionalInt.of(EXPLOSION_SOUND_COUNTDOWN - 1)
                    : OptionalInt.empty(),
                false);
        }

        return new Decision(Phase.NORMAL, OptionalInt.empty(), false, OptionalInt.empty(), false);
    }

    private static void validateUnsignedCountdown(int countdown) {
        if (countdown < 0 || countdown > 0xFF) {
            throw new IllegalArgumentException(
                "Bomb transition countdown must be an unsigned byte: " + countdown);
        }
    }
}
