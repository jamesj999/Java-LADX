package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RichardMotionTest {
    private final RichardMotion motion = new RichardMotion();
    private RoomEntity richard = new RoomEntity(0, 0, 0x95, 0x68, 0x20, EntityStatus.ACTIVE,
        EntitySpriteDefinition.unsupported(0x95), 0);

    private int tick(boolean talk, int leaves, int roomStatus, int choice, int bowWow) {
        if (talk) motion.startConversation(bowWow, leaves, roomStatus);
        else richard = motion.advance(richard, 0, 0x78, 0x20, leaves, choice);
        return motion.takeDialog();
    }

    @Test void offerBranchesAndHandInOpensPassageAfterRichardMoves() {
        tick(false, 0, 0, 0, 0);
        assertEquals(0x2D, tick(true, 0, 0, 0, 1));
        for (int choice = 0; choice < 2; choice++) {
            assertEquals(0x3A, tick(true, 0, 0x10, 0, 0));
            assertEquals(0x3B + choice, tick(false, 0, 0x10, choice, 0));
        }
        assertEquals(0x3F, tick(true, 4, 0x10, 0, 0));
        assertEquals(0x3D, tick(true, 5, 0x10, 0, 0));
        assertTrue(motion.handingInLeaves());
        assertFalse(motion.pushesLink());
        for (int frame = 0; frame < 32; frame++) {
            tick(false, 0xFF, 0x10, 0, 0);
            assertFalse(motion.warpRequested());
        }
        assertEquals(0x58, richard.x());
        assertEquals(0x3D, tick(true, 0xFF, 0x10, 0, 0));
        tick(false, 0xFF, 0x10, 0, 0);
        assertTrue(motion.warpRequested());
        assertEquals(0x3E, tick(true, 6, 0x10, 0, 0));
    }
}
