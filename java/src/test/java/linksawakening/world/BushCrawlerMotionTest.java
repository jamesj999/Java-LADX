package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BushCrawlerMotionTest {

    @Test
    void stateZeroUsesTheRomDirectionToLinkAndStartsItsThirtyFrameCrawl() {
        BushCrawlerMotion motion = new BushCrawlerMotion();
        RoomEntity entity = entity(0, 0x40, 0x40, 0);

        BushCrawlerMotion.Update update = motion.advance(entity, 0, 0x40, 0x47,
            () -> 0, null, 0, false, false, false, 0, 0);

        assertEquals(1, update.state());
        assertEquals(0x30, update.transitionCountdown());
        assertEquals(0x10, motion.speedY(0));
        assertEquals(0, motion.speedX(0));
        assertEquals(0x40, update.entity().x());
        assertEquals(0x40, update.entity().y());
        assertEquals(0, update.entity().spriteVariant());
        assertEquals(0, update.visualYOffset());
        assertTrue(!update.liftRequested());
    }

    @Test
    void crawlingMovesWithTheBankSevenFixedPointPositionHelperAndStopsOnWall() {
        BushCrawlerMotion motion = new BushCrawlerMotion();
        motion.forceStateForTest(0, 1, 2, 0, 0, 0, 0x10);
        RoomEntity entity = entity(0, 0x40, 0x40, 0);

        BushCrawlerMotion.Update moved = motion.advance(entity, 8, 0x00, 0x00,
            () -> 0, null, 2, false, false, false, 0, 0);
        assertEquals(0x41, moved.entity().y());
        assertEquals(1, moved.state());
        assertEquals(-4, moved.visualYOffset());
        assertEquals(0, moved.entity().spriteVariant());

        motion.forceStateForTest(0, 1, 2, 0, 0, 0, 0x10);
        BushCrawlerMotion.Update blocked = motion.advance(entity, 8, 0x00, 0x00,
            () -> 0, (candidate, direction, nextX, nextY) -> direction == 3,
            2, false, false, false, 0, 0);
        assertEquals(0x40, blocked.entity().x());
        assertEquals(0, blocked.state());
        assertEquals(0x20, blocked.transitionCountdown());
    }

    @Test
    void hiddenCrawlerRequestsThePowerBraceletLiftWhenLinkIsAboveIt() {
        BushCrawlerMotion motion = new BushCrawlerMotion();
        motion.forceStateForTest(0, 0, 0, 1, 1, 0, 0);
        RoomEntity entity = entity(0, 0x40, 0x40, 0);

        BushCrawlerMotion.Update update = motion.advance(entity, 0, 0x40, 0x38,
            () -> 0, null, 0, true, true, false, 0x03, 0x00);

        assertTrue(update.liftRequested());
        assertEquals(0, update.state());
        assertEquals(1, update.privateState4());
    }

    @Test
    void liftedReplacementUsesTheConcatenatedBankSevenSpeedTables() {
        BushCrawlerMotion motion = new BushCrawlerMotion();
        motion.forcePrivateStateForTest(0, 2, 0x01);
        RoomEntity entity = entity(0, 0x40, 0x40, 0);

        BushCrawlerMotion.Update update = motion.advance(entity, 0, 0x00, 0x00,
            () -> 0x02, null, 0, false, false, false, 0, 0);

        assertEquals(0x22, update.transitionCountdown());
        assertEquals(0x10, motion.speedX(0));
        assertEquals(0, motion.speedY(0));
        assertEquals(2, update.privateState1());
        assertEquals(0, update.visualYOffset());
    }

    private static RoomEntity entity(int slot, int x, int y, int variant) {
        return new RoomEntity(slot, 0, BushCrawlerMotion.ENTITY_BUSH_CRAWLER, x, y,
            EntityStatus.ACTIVE, EntitySpriteDefinition.unsupported(
                BushCrawlerMotion.ENTITY_BUSH_CRAWLER), variant);
    }
}
