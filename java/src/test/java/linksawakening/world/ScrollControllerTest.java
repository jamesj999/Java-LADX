package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScrollControllerTest {

    @Test
    void tracksActiveScrollAndClearsPreviousRoomWhenFinished() {
        RoomRenderSnapshot previous = new RoomRenderSnapshot(new int[] { 1 }, new int[] { 2 }, new int[][] {{ 3 }});
        ScrollController scroll = new ScrollController();

        scroll.start(ScrollController.RIGHT, 10, 20, previous, 8);

        assertTrue(scroll.isActive());
        assertEquals(ScrollController.RIGHT, scroll.direction());
        assertEquals(10, scroll.linkScreenX());
        assertEquals(20, scroll.linkScreenY());
        assertSame(previous, scroll.previousRoom());

        assertFalse(scroll.tick(4));
        assertEquals(4, scroll.offset());
        assertTrue(scroll.tick(4));
        assertFalse(scroll.isActive());
        assertEquals(0, scroll.offset());
        assertEquals(ScrollController.NONE, scroll.direction());
    }
}
