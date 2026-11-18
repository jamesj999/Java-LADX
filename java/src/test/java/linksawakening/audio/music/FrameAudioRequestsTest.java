package linksawakening.audio.music;

import static linksawakening.audio.sfx.SoundEffectNamespace.JINGLE;
import static linksawakening.audio.sfx.SoundEffectNamespace.NOISE;
import static linksawakening.audio.sfx.SoundEffectNamespace.WAVE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.sfx.SoundEffectPlayer;
import linksawakening.rom.Rom;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import org.junit.jupiter.api.Test;

@RequiresRom
final class FrameAudioRequestsTest {
    @Test
    void finalRequestInEachSlotWinsAndTreasureSuppressesWave() throws IOException {
        Rom rom = loadRom();
        var frames = new ArrayList<short[]>();
        var referenceFrames = new ArrayList<short[]>();
        try (var player = new FrameAudioPlayer(rom, 48000, (pcm, rate) -> frames.add(pcm));
                var reference = new FrameAudioPlayer(rom, 48000, (pcm, rate) -> referenceFrames.add(pcm))) {
            player.requestSoundEffect(NOISE, 3);
            player.requestSoundEffect(WAVE, 1);
            player.requestSoundEffect(NOISE, 2);
            reference.requestSoundEffect(WAVE, 1);
            reference.requestSoundEffect(NOISE, 2);
            assertTrue(frames.isEmpty());
            for (int frame = 0; frame < 2; frame++) {
                player.update();
                reference.update();
                assertArrayEquals(referenceFrames.get(frame), frames.get(frame));
            }
        }
        frames.clear();
        referenceFrames.clear();
        try (var player = new FrameAudioPlayer(rom, 48000, (pcm, rate) -> frames.add(pcm));
                var reference = new FrameAudioPlayer(rom, 48000, (pcm, rate) -> referenceFrames.add(pcm))) {
            player.requestSoundEffect(WAVE, 1);
            player.requestSoundEffect(JINGLE, 1);
            reference.requestSoundEffect(JINGLE, 1);
            player.update();
            reference.update();
            assertArrayEquals(referenceFrames.getFirst(), frames.getFirst());
        }
    }

    @Test
    void bothMusicBanksReleaseWaveAndRetainBombNoise() throws IOException {
        Rom rom = loadRom();
        for (int bank : new int[] {0x1B, 0x1E}) {
            var effects = new SoundEffectPlayer(new GameBoyApu(48000), rom);
            effects.request(effects.catalog().find(WAVE, 1).orElseThrow());
            effects.request(effects.catalog().find(NOISE, 0x0C).orElseThrow());
            effects.advanceSharedFrame();
            assertTrue(effects.ownsChannel(3));
            assertTrue(effects.ownsChannel(4));
            effects.changeMusicTrack(bank, false);
            assertFalse(effects.ownsChannel(3));
            assertTrue(effects.ownsChannel(4));
            effects.changeMusicTrack(bank, true);
            assertTrue(effects.ownsChannel(4));
            effects.request(effects.catalog().find(NOISE, 2).orElseThrow());
            effects.advanceSharedFrame();
            effects.changeMusicTrack(bank, false);
            assertFalse(effects.ownsChannel(4));
        }
    }

    @Test
    void windFishEffectSilencesAndRestartsTheScore() throws IOException {
        Rom rom = loadRom();
        try (var player = new FrameAudioPlayer(rom, 48000, (pcm, rate) -> {})) {
            var music = new GameplayMusicController(AreaMusicResolver.fromRom(rom), MusicCatalog.fromRom(rom), player);
            music.playDirect(0x59);
            player.update();
            assertTrue(player.isMusicPlaying());
            player.requestSoundEffect(WAVE, 0x1F);
            for (int elapsed = 0; elapsed < 225; elapsed++) {
                player.update();
                assertFalse(player.isMusicPlaying(), "Elapsed frames " + elapsed);
                assertEquals(0, music.currentTrackId());
            }
            player.update();
            assertTrue(player.isMusicPlaying());
            assertEquals(0x59, music.currentTrackId());
        }
    }

    @Test
    void menuVolumeAppliesBeforeRenderingAndRestoresOnExit() throws IOException {
        try (var player = new FrameAudioPlayer(loadRom(), 48000, (pcm, rate) -> {})) {
            List<Integer> volume = new ArrayList<>();
            player.setRegisterObserver((register, value) -> {
                if (register == GameBoyApu.NR50) volume.add(value);
            });
            player.setMenuAttenuated(true);
            player.update();
            assertEquals(0x33, volume.getLast());
            player.setMenuAttenuated(false);
            player.update();
            assertEquals(0x77, volume.getLast());
        }
    }

    private static Rom loadRom() throws IOException {
        try (var stream = TestRom.open()) {
            return Rom.of(stream.readAllBytes());
        }
    }
}
