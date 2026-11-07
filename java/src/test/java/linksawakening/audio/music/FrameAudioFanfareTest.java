package linksawakening.audio.music;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@linksawakening.testing.RequiresRom
final class FrameAudioFanfareTest {
    @Test
    void acquisitionFanfareEndsWithoutAnAudioDeviceOrWallClockWait() throws Exception {
        byte[] rom;
        try (var in = linksawakening.testing.TestRom.open()) {
            rom = in.readAllBytes();
        }
        try (var player = new FrameAudioPlayer(rom, 48_000, (pcm, rate) -> {})) {
            player.play(MusicCatalog.fromRom(rom).requireTrack(0x1B));
            assertTrue(player.isMusicPlaying());
            int frames = 0;
            while (player.isMusicPlaying() && frames++ < 1200) player.update();
            assertFalse(player.isMusicPlaying(), "Finite fanfare must release gameplay acquisition gate");
            assertTrue(frames > 1);
        }
    }
}
