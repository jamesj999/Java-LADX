package linksawakening.cutscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IntroPlaybackTest {
    @Test
    void unloadedPlaybackNeedsNoRomAndPublishesExplicitEmptyPresentation() {
        var playback = new IntroPlayback(scene -> {
            throw new AssertionError(scene);
        });
        playback.setNoiseSound(sound -> {
            throw new AssertionError(sound);
        });
        assertFalse(playback.active());
        assertFalse(playback.tick());
        assertFalse(playback.skip());
        assertEquals(0, playback.frame().scrollX());
        assertEquals(0, playback.frame().scrollY());
        assertEquals(7, playback.frame().titleRevealRows());
        assertTrue(playback.frame().sprites().isEmpty());
        assertNull(playback.snapshotOrNull());
    }
}
