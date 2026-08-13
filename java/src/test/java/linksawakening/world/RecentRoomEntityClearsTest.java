package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RecentRoomEntityClearsTest {

    @Test
    void sixthOtherDistinctRoomEvictsTheSourceTransientClearMask() {
        RecentRoomEntityClears clears = new RecentRoomEntityClears();
        clears.visit(0x24);
        clears.addMask(0x24, 0x1F);

        for (int room = 0x30; room < 0x35; room++) {
            clears.visit(room);
        }
        assertEquals(0x1F, clears.mask(0x24));

        clears.visit(0x35);

        assertEquals(0, clears.mask(0x24));
    }

    @Test
    void revisitingARecentRoomDoesNotAdvanceTheRing() {
        RecentRoomEntityClears clears = new RecentRoomEntityClears();
        clears.visit(0x24);
        clears.addMask(0x24, 0x1F);
        clears.visit(0x30);
        clears.visit(0x24);
        clears.visit(0x31);
        clears.visit(0x32);
        clears.visit(0x33);
        clears.visit(0x34);

        assertEquals(0x1F, clears.mask(0x24));
    }
}
