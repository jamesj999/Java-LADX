package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SideViewPlatformMotionTest {

    @Test
    void firstActiveFrameAcceleratesAfterMovingWithCurrentSpeed() {
        SideViewPlatformMotion motion = new SideViewPlatformMotion();

        SideViewPlatformMotion.Update update = motion.advance(entity(0x40, 0x50), 0, true, true);

        assertEquals(0, update.verticalDelta());
        assertEquals(0x50, update.entity().y());
        assertEquals(1, update.speedY());
        assertEquals(1, update.privateState4());
        assertEquals(0x10, update.privateState2());
        assertFalse(update.rumble());
    }

    @Test
    void fourthActiveFrameRumblesOnceWhenActivationCapsAtFour() {
        SideViewPlatformMotion motion = new SideViewPlatformMotion();
        RoomEntity current = entity(0x40, 0x50);

        SideViewPlatformMotion.Update first = motion.advance(current, 0, true, true);
        SideViewPlatformMotion.Update second = motion.advance(first.entity(), 1, true, true);
        SideViewPlatformMotion.Update third = motion.advance(second.entity(), 2, true, true);
        SideViewPlatformMotion.Update fourth = motion.advance(third.entity(), 3, true, true);
        SideViewPlatformMotion.Update fifth = motion.advance(fourth.entity(), 4, true, true);

        assertFalse(first.rumble());
        assertFalse(second.rumble());
        assertFalse(third.rumble());
        assertTrue(fourth.rumble());
        assertFalse(fifth.rumble());
        assertEquals(4, fifth.privateState4());
    }

    @Test
    void sixteenTicksAtSpeedOneProduceOnePixelUsingSourceOrder() {
        SideViewPlatformMotion motion = new SideViewPlatformMotion();
        RoomEntity current = entity(0x40, 0x50);

        SideViewPlatformMotion.Update first = motion.advance(current, 0, true, true);
        assertEquals(1, first.speedY());
        for (int tick = 0; tick < 16; tick++) {
            // A non-quarter-frame tick leaves the current speed at one.
            first = motion.advance(first.entity(), 1, true, true);
        }

        assertEquals(0x51, first.entity().y());
        assertEquals(1, first.verticalDelta());
        assertEquals(1, first.speedY());
    }

    @Test
    void inactiveFrameResetsSpeedAndStateAndClearValidatesSlot() {
        SideViewPlatformMotion motion = new SideViewPlatformMotion();
        RoomEntity current = entity(0x40, 0x50);
        motion.advance(current, 0, true, true);

        SideViewPlatformMotion.Update inactive = motion.advance(current, 1, false, true);

        assertEquals(0, inactive.speedY());
        assertEquals(0, inactive.privateState4());
        assertEquals(0, inactive.privateState2());
        assertThrows(IllegalArgumentException.class, () -> motion.clear(-1));
        assertThrows(IllegalArgumentException.class, () -> motion.clear(16));
        assertThrows(IllegalArgumentException.class,
            () -> motion.advance(entity(0x40, 0x50, 0xA4), 0, false, false));
    }

    @Test
    void negativeSpeedMovesUpBeforeQuarterFrameAcceleration() {
        SideViewPlatformMotion motion = new SideViewPlatformMotion();
        motion.setSpeedY(0, 0xF0);

        SideViewPlatformMotion.Update update = motion.advance(entity(0x40, 0x50), 0, true, true);

        assertEquals(0x4F, update.entity().y());
        assertEquals(-1, update.verticalDelta());
        assertEquals(0xF1, update.speedY());
    }

    @Test
    void speedFourDoesNotAccelerateOnQuarterFrame() {
        SideViewPlatformMotion motion = new SideViewPlatformMotion();
        motion.setSpeedY(0, 4);

        SideViewPlatformMotion.Update update = motion.advance(entity(0x40, 0x50), 0, true, true);

        assertEquals(4, update.speedY());
    }

    @Test
    void highBitSpeedDoesNotAcceleratePastTheSignedBoundary() {
        SideViewPlatformMotion motion = new SideViewPlatformMotion();
        motion.setSpeedY(0, 0x80);

        SideViewPlatformMotion.Update update = motion.advance(entity(0x40, 0x50), 0, true, true);

        assertEquals(0x80, update.speedY());
    }

    @Test
    void beginFrameMovesHorizontalSpeedAndReportsDelta() {
        SideViewPlatformMotion motion = new SideViewPlatformMotion();
        motion.setSpeedX(0, 0x10);

        SideViewPlatformMotion.Frame frame = motion.beginFrame(entity(0x40, 0x50));

        assertEquals(0x41, frame.entity().x());
        assertEquals(1, frame.horizontalDelta());
    }

    @Test
    void movementUsesOldSpeedBeforeQuarterFrameAcceleration() {
        SideViewPlatformMotion motion = new SideViewPlatformMotion();
        motion.setSpeedY(0, 0xE0);

        SideViewPlatformMotion.Update update = motion.advance(entity(0x40, 0x50), 0, true, true);

        assertEquals(0x4E, update.entity().y());
        assertEquals(-2, update.verticalDelta());
        assertEquals(0xE1, update.speedY());
    }

    private static RoomEntity entity(int x, int y) {
        return entity(x, y, SideViewPlatformMotion.ENTITY_TYPE);
    }

    private static RoomEntity entity(int x, int y, int type) {
        return new RoomEntity(0, 0, type, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(type), -1);
    }
}
