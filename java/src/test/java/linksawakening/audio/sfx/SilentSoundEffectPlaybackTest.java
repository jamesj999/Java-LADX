package linksawakening.audio.sfx;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class SilentSoundEffectPlaybackTest {
    @Test
    void disabledTransportIsReusableAndNeverStartsAnEngine() {
        var playback = SoundEffectPlayback.none();
        assertSame(playback, SoundEffectPlayback.none());
        playback.tick60Hz();
        playback.stop();
        assertFalse(playback.available());
        assertFalse(playback.isPlaying());
    }
}
