package linksawakening.gameplay;

import linksawakening.audio.music.AreaMusicResolver;
import linksawakening.audio.music.GameplayMusicController;
import linksawakening.audio.music.MusicCatalog;
import linksawakening.audio.sfx.SoundEffectNamespace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FrameSoundSinkTest {
    @Test
    void windFishMorphSilencesThePreviousScoreAndRestartsItOnItsLastFrame() throws Exception {
        byte[] rom;
        try (var input = getClass().getResourceAsStream("/rom/azle.gbc")) {
            rom = input.readAllBytes();
        }
        FrameSoundSink sound = new FrameSoundSink(rom, (samples, rate) -> {});
        GameplayMusicController music = new GameplayMusicController(
            AreaMusicResolver.fromRom(rom), MusicCatalog.fromRom(rom), sound);
        music.playDirect(0x59);
        sound.finishFrame(music, true);
        assertTrue(sound.isMusicPlaying());

        sound.playRomSound(SoundEffectNamespace.WAVE, 0x1F);
        for (int elapsed = 0; elapsed < 225; elapsed++) {
            sound.finishFrame(music, true);
            assertFalse(sound.isMusicPlaying(), "Elapsed frames " + elapsed);
            assertEquals(0, music.currentTrackId());
        }
        sound.finishFrame(music, true);
        assertTrue(sound.isMusicPlaying());
        assertEquals(0x59, music.currentTrackId());
    }
}
