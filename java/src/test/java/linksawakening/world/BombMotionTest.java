package linksawakening.world;

import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BombMotionTest {

    @Test
    void normalBombUsesTheRomFuseBoundaries() {
        assertEquals(BombMotion.Phase.NORMAL, BombMotion.decide(0xA0).phase());
        assertEquals(BombMotion.Phase.NORMAL, BombMotion.decide(0x22).phase());
        assertEquals(BombMotion.Phase.NORMAL, BombMotion.decide(0x23).phase());
    }

    @Test
    void warningPairOccupiesTheLastEightFuseCounts() {
        assertEquals(BombMotion.Phase.WARNING, BombMotion.decide(0x21).phase());
        assertEquals(BombMotion.Phase.WARNING, BombMotion.decide(0x18).phase());
        assertEquals(BombMotion.Phase.NORMAL, BombMotion.decide(0x22).phase());
    }

    @Test
    void explosionSoundIsAOneShotTransitionAtCount18() {
        BombMotion.Decision transition = BombMotion.decide(0x18);

        assertTrue(transition.playExplosionSound());
        assertEquals(OptionalInt.of(0x17), transition.countdownOverride());

        BombMotion.Decision firstExplosion = BombMotion.decide(
            transition.countdownOverride().orElseThrow());
        assertFalse(firstExplosion.playExplosionSound());
        assertEquals(OptionalInt.empty(), firstExplosion.countdownOverride());
        assertEquals(BombMotion.Phase.EXPLOSION, firstExplosion.phase());
    }

    @Test
    void explosionVariantUsesEveryRomTableRangeAndRendersZeroBeforeUnload() {
        assertExplosionVariant(0x17, 3);
        assertExplosionVariant(0x14, 3);
        assertExplosionVariant(0x13, 2);
        assertExplosionVariant(0x10, 2);
        assertExplosionVariant(0x0F, 1);
        assertExplosionVariant(0x08, 1);
        assertExplosionVariant(0x07, 0);

        BombMotion.Decision finalExplosion = BombMotion.decide(0x00);
        assertEquals(BombMotion.Phase.EXPLOSION, finalExplosion.phase());
        assertEquals(0, finalExplosion.explosionVariant());
        assertTrue(finalExplosion.unloadAfterPresentation());
    }

    @Test
    void nonExplosionCountsCannotIndexTheTwentyFourByteVariantTable() {
        assertEquals(-1, BombMotion.decide(0x18).explosionVariant());
        assertEquals(-1, BombMotion.decide(0x22).explosionVariant());
        assertEquals(BombMotion.Phase.NORMAL, BombMotion.decide(0xFF).phase());
        assertEquals(-1, BombMotion.decide(0xFF).explosionVariant());

        assertThrows(IllegalArgumentException.class, () -> BombMotion.decide(-1));
        assertThrows(IllegalArgumentException.class, () -> BombMotion.decide(0x100));
    }

    private static void assertExplosionVariant(int countdown, int expectedVariant) {
        BombMotion.Decision decision = BombMotion.decide(countdown);
        assertEquals(BombMotion.Phase.EXPLOSION, decision.phase());
        assertEquals(expectedVariant, decision.explosionVariant());
        assertFalse(decision.playExplosionSound());
        assertFalse(decision.unloadAfterPresentation());
    }
}
