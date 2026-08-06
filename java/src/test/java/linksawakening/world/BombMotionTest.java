package linksawakening.world;

import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class BombMotionTest {

    @Test
    void normalBombUsesTheRomFuseBoundaries() {
        assertDecision(BombMotion.decide(0xA0), BombMotion.Phase.NORMAL,
            OptionalInt.empty(), false, OptionalInt.empty(), false);
        assertDecision(BombMotion.decide(0x22), BombMotion.Phase.NORMAL,
            OptionalInt.empty(), false, OptionalInt.empty(), false);
        assertDecision(BombMotion.decide(0x23), BombMotion.Phase.NORMAL,
            OptionalInt.empty(), false, OptionalInt.empty(), false);
    }

    @Test
    void warningPairCoversCountdowns0x18Through0x21Inclusive() {
        assertDecision(BombMotion.decide(0x21), BombMotion.Phase.WARNING,
            OptionalInt.empty(), false, OptionalInt.empty(), false);
        assertDecision(BombMotion.decide(0x1C), BombMotion.Phase.WARNING,
            OptionalInt.empty(), false, OptionalInt.empty(), false);
        assertDecision(BombMotion.decide(0x18), BombMotion.Phase.WARNING,
            OptionalInt.empty(), true, OptionalInt.of(0x17), false);
        assertDecision(BombMotion.decide(0x22), BombMotion.Phase.NORMAL,
            OptionalInt.empty(), false, OptionalInt.empty(), false);
    }

    @Test
    void explosionSoundIsAOneShotTransitionAtCount18() {
        BombMotion.Decision transition = BombMotion.decide(0x18);

        assertDecision(transition, BombMotion.Phase.WARNING, OptionalInt.empty(), true,
            OptionalInt.of(0x17), false);

        BombMotion.Decision firstExplosion = BombMotion.decide(
            transition.countdownOverride().orElseThrow());
        assertDecision(firstExplosion, BombMotion.Phase.EXPLOSION, OptionalInt.of(3), false,
            OptionalInt.empty(), false);
    }

    @Test
    void explosionVariantMatchesEveryEntryAndRendersZeroBeforeUnload() {
        int[] expectedVariants = {
            0, 0, 0, 0, 0, 0, 0, 0,
            1, 1, 1, 1, 1, 1, 1, 1,
            2, 2, 2, 2,
            3, 3, 3, 3
        };

        for (int countdown = 0; countdown < expectedVariants.length; countdown++) {
            assertDecision(BombMotion.decide(countdown), BombMotion.Phase.EXPLOSION,
                OptionalInt.of(expectedVariants[countdown]), false, OptionalInt.empty(),
                countdown == 0);
        }
    }

    @Test
    void nonExplosionCountsCannotIndexTheTwentyFourByteVariantTable() {
        assertDecision(BombMotion.decide(0xFF), BombMotion.Phase.NORMAL,
            OptionalInt.empty(), false, OptionalInt.empty(), false);

        assertThrows(IllegalArgumentException.class, () -> BombMotion.decide(-1));
        assertThrows(IllegalArgumentException.class, () -> BombMotion.decide(0x100));
    }

    private static void assertDecision(BombMotion.Decision decision,
                                       BombMotion.Phase expectedPhase,
                                       OptionalInt expectedVariant,
                                       boolean expectedSound,
                                       OptionalInt expectedCountdownOverride,
                                       boolean expectedUnload) {
        assertEquals(expectedPhase, decision.phase());
        assertEquals(expectedVariant, decision.explosionVariant());
        assertEquals(expectedSound, decision.playExplosionSound());
        assertEquals(expectedCountdownOverride, decision.countdownOverride());
        assertEquals(expectedUnload, decision.unloadAfterPresentation());
    }
}
