package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ScreenShakeMotionTest {

    @Test
    void followsTheRomPhaseTableForAnArmosLanding() {
        ScreenShakeMotion motion = new ScreenShakeMotion();
        motion.start(0x30, 0x04);

        motion.tick();
        assertEquals(0x2F, motion.countdown());
        assertEquals(0, motion.horizontal());
        assertEquals(-2, motion.vertical());

        motion.tick();
        assertEquals(0, motion.horizontal());
        assertEquals(0, motion.vertical());

        motion.tick();
        assertEquals(0, motion.horizontal());
        assertEquals(2, motion.vertical());

        motion.tick();
        assertEquals(0, motion.horizontal());
        assertEquals(0, motion.vertical());

        for (int frame = 4; frame < 0x30; frame++) {
            motion.tick();
        }
        assertEquals(0, motion.countdown());
        assertEquals(0, motion.horizontal());
        assertEquals(0, motion.vertical());

        motion.tick();
        assertEquals(0, motion.countdown());
        assertEquals(0, motion.horizontal());
        assertEquals(0, motion.vertical());
    }
}
