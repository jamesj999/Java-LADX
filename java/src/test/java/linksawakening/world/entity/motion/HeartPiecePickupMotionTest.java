package linksawakening.world.entity.motion;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class HeartPiecePickupMotionTest {
    @Test
    void locksMeterAnimationAndAwardsExactlyOnceAtFrame56() {
        var motion = new HeartPiecePickupMotion();
        assertEquals(-1, motion.advance(1, false, 0).dialogId());
        motion.advance(0, false, 0);
        assertEquals(3, motion.advance(0, false, 0).tilesCommand());
        assertEquals(4, motion.advance(0, false, 0).tilesCommand());
        var open = motion.advance(0, false, 0);
        assertEquals(0x4F, open.dialogId());
        assertTrue(open.interactionLocked());
        for (int frame = 1; frame <= 168; frame++) {
            var update = motion.advance(0, true, frame > 56 ? 1 : 0);
            assertEquals(frame == 56, update.incrementPiece());
            assertTrue(update.interactionLocked());
            assertFalse(update.complete());
        }
        assertFalse(motion.advance(0, true, 1).interactionLocked());
        assertTrue(motion.active());
        assertEquals(-1, motion.advance(0, false, 1).dialogId());
        assertEquals(5, motion.advance(0, false, 1).tilesCommand());
        var done = motion.advance(0, false, 1);
        assertEquals(6, done.tilesCommand());
        assertTrue(done.complete());
        assertFalse(motion.active());
        assertFalse(motion.advance(0, false, 1).complete());
    }

    @Test
    void fourthPieceWaitsForFirstDialogBeforeAwardingContainerAndSecondDialog() {
        var motion = new HeartPiecePickupMotion();
        for (int i = 0; i < 4; i++) motion.advance(0, false, 3);
        for (int i = 0; i < 168; i++) motion.advance(0, true, 4);
        assertFalse(motion.advance(0, true, 4).newContainer());
        var reward = motion.advance(0, false, 4);
        assertTrue(reward.newContainer());
        assertEquals(0x50, reward.dialogId());
        assertEquals(0, motion.advance(0, true, 0).tilesCommand());
        assertEquals(5, motion.advance(0, false, 0).tilesCommand());
        assertTrue(motion.advance(0, false, 0).complete());
    }
}
