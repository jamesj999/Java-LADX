package linksawakening.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import linksawakening.gameplay.*;
import linksawakening.input.*;
import linksawakening.rom.Rom;
import org.junit.jupiter.api.Test;

class GameOverMenuTest {
    @Test
    void edgesWrapAndBothDirectionsCancelWithOneJingle() {
        byte[] bytes = new byte[0x8000];
        bytes[0x4336] = 80;
        bytes[0x4337] = 96;
        bytes[0x4338] = 112;
        var menu = new GameOverMenu(new Rom(bytes));
        var input = new InputState();
        var config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        var sounds = new ArrayList<GameplaySoundEvent>();
        input.onKeyEvent(2, 1);
        assertEquals(-1, menu.tick(input, config, sounds::add));
        assertEquals(2, menu.selected());
        assertEquals(96, menu.cursor().y());
        assertEquals(28, menu.cursor().x());
        assertEquals(0xBE, menu.cursor().tileIndex());
        assertEquals(16, menu.cursor().height());
        input.tickEdges();
        menu.tick(input, config, sounds::add);
        assertEquals(2, menu.selected(), "holding does not repeat");
        input.onKeyEvent(2, 0);
        input.tickEdges();
        input.onKeyEvent(3, 1);
        menu.tick(input, config, sounds::add);
        assertEquals(0, menu.selected());
        input.onKeyEvent(3, 0);
        input.tickEdges();
        input.onKeyEvent(3, 1);
        input.onKeyEvent(2, 1);
        menu.tick(input, config, sounds::add);
        assertEquals(0, menu.selected());
        assertEquals(
                java.util.List.of(
                        GameplaySoundEvent.MENU_MOVE, GameplaySoundEvent.MENU_MOVE, GameplaySoundEvent.MENU_MOVE),
                sounds);
        input.tickEdges();
        input.onKeyEvent(6, 1);
        assertEquals(0, menu.tick(input, config, sounds::add));
        input.tickEdges();
        assertEquals(-1, menu.tick(input, config, sounds::add));
        input.onKeyEvent(7, 1);
        assertEquals(0, menu.tick(input, config, sounds::add));
        input.tickEdges();
        input.onKeyEvent(1, 1);
        assertEquals(0, menu.tick(input, config, sounds::add));
    }
}
