package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LiftedEntityMotionTest {

    @Test
    void phaseZeroPromotesToPhaseOneAndUsesTheRomCarryTables() {
        LiftedEntityMotion.Update update = LiftedEntityMotion.advance(
            0, 2, LiftedEntityMotion.ROM_DIRECTION_RIGHT,
            LiftedEntityMotion.ROM_DIRECTION_DOWN,
            0x50, 0x60, 0x04, 0,
            0x05, false, false, 0x22);

        assertEquals(1, update.phase());
        assertEquals(2, update.transitionCountdown());
        assertEquals(0x37, update.carryState());
        assertEquals(LiftedEntityMotion.ROM_DIRECTION_RIGHT, update.effectiveDirection());
        assertEquals(0x60, update.x());
        assertEquals(0x60, update.y());
        assertEquals(0x04, update.z());
    }

    @Test
    void countdownReloadUsesTheOldPhaseAndThenAdvances() {
        LiftedEntityMotion.Update update = LiftedEntityMotion.advance(
            1, 0, LiftedEntityMotion.ROM_DIRECTION_LEFT,
            LiftedEntityMotion.ROM_DIRECTION_LEFT,
            0x50, 0x60, 0, 0,
            0x05, false, false, 0x22);

        assertEquals(2, update.phase());
        assertEquals(8, update.transitionCountdown());
        assertEquals(0x39, update.carryState());
        assertEquals(0x40, update.x());
        assertEquals(0x60, update.y());
        assertEquals(0x00, update.z());
    }

    @Test
    void fullyHeldPhaseUsesCurrentDirectionAndSideScrollZOffset() {
        LiftedEntityMotion.Update update = LiftedEntityMotion.advance(
            4, 7, LiftedEntityMotion.ROM_DIRECTION_RIGHT,
            LiftedEntityMotion.ROM_DIRECTION_UP,
            0x50, 0x60, 0x20, 0xFD,
            0x05, true, false, 0x22);

        assertEquals(4, update.phase());
        assertEquals(7, update.transitionCountdown());
        assertEquals(0x01, update.carryState());
        assertEquals(LiftedEntityMotion.ROM_DIRECTION_UP, update.effectiveDirection());
        assertEquals(0x50, update.x());
        assertEquals(0x4F, update.y());
        assertEquals(0x22, update.z());
    }

    @Test
    void fastTransitionTableMatchesBombOrPoweredLiftPath() {
        LiftedEntityMotion.Update update = LiftedEntityMotion.advance(
            1, 0, LiftedEntityMotion.ROM_DIRECTION_DOWN,
            LiftedEntityMotion.ROM_DIRECTION_DOWN,
            0x50, 0x60, 0, 0,
            0x02, false, true, 0);

        assertEquals(2, update.phase());
        assertEquals(4, update.transitionCountdown());
        assertEquals(0x3D, update.carryState());
    }
}
