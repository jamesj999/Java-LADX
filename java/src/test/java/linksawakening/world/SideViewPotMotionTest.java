package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SideViewPotMotionTest {

    @Test
    void sideScrollThrowUsesBank14DirectionWindow() {
        SideViewPotMotion motion = new SideViewPotMotion();

        motion.start(0, ThrownEntityMotion.ROM_DIRECTION_RIGHT);
        assertEquals(2, motion.state(0));
        assertEquals(0x30, motion.speedX(0));
        assertEquals(0xF4, motion.speedY(0));

        motion.start(0, ThrownEntityMotion.ROM_DIRECTION_LEFT);
        assertEquals(0xD0, motion.speedX(0));
        assertEquals(0xF4, motion.speedY(0));

        motion.start(0, ThrownEntityMotion.ROM_DIRECTION_UP);
        assertEquals(0x00, motion.speedX(0));
        assertEquals(0xD0, motion.speedY(0));

        motion.start(0, ThrownEntityMotion.ROM_DIRECTION_DOWN);
        assertEquals(0x00, motion.speedX(0));
        assertEquals(0x00, motion.speedY(0));
    }

    @Test
    void motionAdvancesBothAxesWithSixteenFrameFixedPoint() {
        SideViewPotMotion motion = new SideViewPotMotion();
        motion.start(0, ThrownEntityMotion.ROM_DIRECTION_RIGHT);

        RoomEntity entity = entity(0x50, 0x60);
        SideViewPotMotion.Update update = motion.advance(entity);

        assertEquals(0x53, update.entity().x());
        assertEquals(0x5F, update.entity().y());
        assertEquals(0xF6, motion.speedY(0));
    }

    @Test
    void negativeSpeedAcceleratesBeforeUnsignedFortyCheck() {
        SideViewPotMotion motion = new SideViewPotMotion();
        motion.setSpeedY(0, 0xF0);

        SideViewPotMotion.Update update = motion.advance(entity(0x50, 0x60));

        assertEquals(0x5F, update.entity().y());
        assertEquals(0xF2, update.speedY());
    }

    @Test
    void speedThreeFBecomesFortyOneButFortyStaysForty() {
        SideViewPotMotion motion = new SideViewPotMotion();
        motion.setSpeedY(0, 0x3F);
        assertEquals(0x41, motion.advance(entity(0x50, 0x60)).speedY());

        motion.setSpeedY(0, 0x40);
        assertEquals(0x40, motion.advance(entity(0x50, 0x60)).speedY());
    }

    @Test
    void clearDropsMotionStateBeforeSlotReuse() {
        SideViewPotMotion motion = new SideViewPotMotion();
        motion.start(0, ThrownEntityMotion.ROM_DIRECTION_RIGHT);
        motion.advance(entity(0x50, 0x60));
        motion.clear(0);

        assertEquals(0, motion.state(0));
        assertEquals(0, motion.speedX(0));
        assertEquals(0, motion.speedY(0));

        motion.start(0, ThrownEntityMotion.ROM_DIRECTION_LEFT);
        assertEquals(2, motion.state(0));
        assertEquals(0xD0, motion.speedX(0));
        assertEquals(0xF4, motion.speedY(0));
    }

    private static RoomEntity entity(int x, int y) {
        return new RoomEntity(0, 0, 0xD6, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(0xD6), -1);
    }
}
