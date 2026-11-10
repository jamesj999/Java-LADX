package linksawakening.ui;

import org.junit.jupiter.api.Test;
import static linksawakening.ui.InventoryTransition.State.*;
import static org.junit.jupiter.api.Assertions.*;

final class InventoryTransitionTest {
    @Test void dxHouseRouteMatchesOffscreenRomDispatchAndWindowTiming() {
        // PyBoy 2.9.0, house-route + Start 8/120 twice, no RAM writes.
        // Frame numbers are route-relative; only register/state observations,
        // no ROM bytes or image fixtures. $20:5904 dispatch, $00:02D5 WY copy.
        var transition = new InventoryTransition();
        transition.open(false);
        for (int frame = 2959; frame <= 3011; frame++) {
            transition.tick();
            var expected = frame < 2969 ? INITIAL : frame < 2984 ? MAP_FADE_OUT
                : frame < 2985 ? LOAD1 : frame < 2988 ? LOAD2 : frame < 2989 ? LOAD3
                : frame < 2994 ? LOAD4 : frame < 2995 ? LOAD5 : frame < 3011 ? FADE_IN : INTERACTIVE;
            assertEquals(expected, transition.state(), "frame=" + frame);
            assertEquals(frame < 2995 ? 128 : 0, transition.windowY(), "frame=" + frame);
            assertEquals(frame < 2995 ? 128 : 0, transition.displayedWindowY());
            assertEquals(frame < 2984, transition.appearing());
            assertTrue(transition.blocksWorld());
            assertEquals(frame == 3011, transition.interactive());
            assertEquals(frame < 2984, transition.worldSpritesVisible());
            assertFalse(transition.worldHandlerActive());
        }
        transition.close();
        for (int frame = 3087; frame <= 3137; frame++) {
            transition.tick();
            var expected = frame < 3103 ? FADE_OUT : frame < 3110 ? WORLD_TILESET
                : frame < 3114 ? WORLD_TILEMAP : frame < 3119 ? WORLD_HUD
                : frame < 3120 ? WORLD_BUTTONS : frame < 3121 ? WORLD_FINISH
                : frame < 3137 ? WORLD_FADE_IN : WORLD;
            assertEquals(expected, transition.state(), "frame=" + frame);
            assertEquals(frame < 3103 ? 0 : 128, transition.windowY());
            assertEquals(frame < 3110 ? 0 : 128, transition.displayedWindowY());
            assertEquals(frame < 3137, transition.blocksWorld());
            assertFalse(transition.appearing());
            assertEquals(frame >= 3121, transition.worldHandlerActive());
        }
    }
    @Test void minimapLoadsTwelveVBlanksAndTransitionInputIsDiscarded() {
        var transition = new InventoryTransition();
        transition.open(true);
        for (int frame = 0; frame < 12; frame++) {
            transition.tick();
            transition.open(false);
            transition.close();
            assertEquals(INITIAL, transition.state());
            assertEquals(128, transition.windowY());
        }
        transition.tick();
        assertEquals(MAP_FADE_OUT, transition.state());
        for (int frame = 0; frame < 42; frame++) transition.tick();
        assertTrue(transition.interactive());
        assertEquals(0, transition.windowY());
    }
    @Test void fadeAlternatesBackgroundAndObjectRgb555Arithmetic() {
        var transition = new InventoryTransition();
        transition.open(false);
        for (int i = 0; i <= 10; i++) transition.tick();
        assertEquals(0x212121, transition.color(0, false)); // +4 on BG first.
        assertEquals(0, transition.color(0, true));
        transition.tick();
        assertEquals(0x212121, transition.color(0, true));
        for (int i = 0; i < 14; i++) transition.tick();
        assertEquals(0xFFFFFF, transition.color(0, false));
        assertEquals(0xFFFFFF, transition.color(0, true));
    }
}
