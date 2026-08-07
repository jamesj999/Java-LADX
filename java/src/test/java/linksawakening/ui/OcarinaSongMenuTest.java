package linksawakening.ui;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OcarinaSongMenuTest {

    @Test
    void rightAndLeftSkipSongsThatAreNotInTheSaveFlags() {
        PlayerState player = new PlayerState();
        player.setOcarinaSongFlags(
            PlayerState.BALLAD_OF_THE_WIND_FISH_FLAG
                | PlayerState.FROGS_SONG_OF_THE_SOUL_FLAG);
        RecordingSoundSink sounds = new RecordingSoundSink();
        OcarinaSongMenu menu = new OcarinaSongMenu(player, sounds);

        assertTrue(menu.requestOpen());
        finishOpening(menu);

        menu.moveSelection(+1);
        assertEquals(2, player.selectedSongIndex());

        menu.moveSelection(-1);
        assertEquals(0, player.selectedSongIndex());
        assertEquals(List.of(GameplaySoundEvent.MENU_VALIDATE,
            GameplaySoundEvent.MENU_VALIDATE), sounds.events);
    }

    @Test
    void openingRequiresAtLeastOneLearnedSong() {
        PlayerState player = new PlayerState();
        OcarinaSongMenu menu = new OcarinaSongMenu(player, GameplaySoundSink.none());

        assertFalse(menu.requestOpen());
        assertFalse(menu.isVisible());
    }

    @Test
    void closingFinishesAfterSixteenFramesAndPreservesCloseRequest() {
        PlayerState player = new PlayerState();
        player.setOcarinaSongFlags(PlayerState.MANBO_MAMBO_FLAG);
        OcarinaSongMenu menu = new OcarinaSongMenu(player, GameplaySoundSink.none());

        menu.requestOpen();
        finishOpening(menu);
        menu.requestClose(true);

        assertFalse(menu.isOpen());
        assertTrue(menu.isVisible());
        for (int frame = 0; frame < 15; frame++) {
            assertFalse(menu.tick());
            assertTrue(menu.isVisible());
        }
        assertTrue(menu.tick());
        assertFalse(menu.isVisible());
        assertTrue(menu.consumeCloseInventoryRequest());
        assertFalse(menu.consumeCloseInventoryRequest());
    }

    private static void finishOpening(OcarinaSongMenu menu) {
        for (int frame = 0; frame < 16; frame++) {
            menu.tick();
        }
        assertTrue(menu.isReady());
    }

    private static final class RecordingSoundSink implements GameplaySoundSink {
        private final List<GameplaySoundEvent> events = new ArrayList<>();

        @Override
        public void play(GameplaySoundEvent event) {
            events.add(event);
        }
    }
}
