package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RoosterMotionTest {

    @Test
    void liftedHandlerUsesTheRomDpadTableAndCollisionClampedSpeeds() {
        RoosterMotion.Update update = RoosterMotion.advanceLifted(
            0, 0x02, 0x04, 0x03, 0x10, 0x20,
            LiftedEntityMotion.ROM_DIRECTION_DOWN);

        assertEquals(LiftedEntityMotion.ROM_DIRECTION_UP, update.romDirection());
        assertEquals(0x04, update.spriteVariant());
        assertEquals(0x03, update.positionZ());
        assertEquals(0x00, update.speedY());
        assertEquals(0x10, update.speedX());
        assertEquals(0x00, update.velocityZ());
    }

    @Test
    void neutralDpadKeepsTheCurrentLinkDirection() {
        RoosterMotion.Update update = RoosterMotion.advanceLifted(
            4, 0x14, 0x00, 0x00, 0x00, 0xF0,
            LiftedEntityMotion.ROM_DIRECTION_LEFT);

        assertEquals(LiftedEntityMotion.ROM_DIRECTION_LEFT, update.romDirection());
        assertEquals(0x03, update.spriteVariant());
        assertEquals(0x14, update.positionZ());
        assertEquals(0x00, update.speedX());
        assertEquals(0xF0, update.speedY());
    }
}
