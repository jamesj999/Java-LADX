package linksawakening.audio.music;

import linksawakening.gameplay.PcmSoundOutput;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FrameAudioPlayerTest {
    @Test void scriptedAudioIsDeterministic() throws Exception {
        byte[] rom;
        try (var stream = getClass().getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            assertNotNull(stream);
            rom = stream.readAllBytes();
        }
        assertArrayEquals(capture(rom), capture(rom));
    }

    private short[] capture(byte[] rom) {
        List<short[]> chunks = new ArrayList<>();
        var catalog = new linksawakening.audio.sfx.SoundEffectPlayer(
            new linksawakening.audio.apu.GameBoyApu(48000), rom).catalog();
        try (var player = new FrameAudioPlayer(rom, 48000, (pcm, rate) -> chunks.add(pcm))) {
            player.play(MusicCatalog.fromRom(rom).requireTrack(1));
            for (int frame = 0; frame < 30; frame++) {
                if (frame == 5) player.playSoundEffect(linksawakening.gameplay.DialogSoundEffectMap
                    .fromCatalog(catalog).resolve(linksawakening.dialog.DialogController.SoundEvent.TEXT_PRINT).orElseThrow());
                if (frame == 12) player.playSoundEffect(linksawakening.gameplay.GameplaySoundEffectMap
                    .fromCatalog(catalog).resolve(linksawakening.gameplay.GameplaySoundEvent.SWORD_SWING_A).orElseThrow());
                player.update();
            }
        }
        assertEquals(30, chunks.size());
        short[] result = new short[30 * 800 * 2];
        int offset = 0;
        for (short[] chunk : chunks) {
            System.arraycopy(chunk, 0, result, offset, chunk.length);
            offset += chunk.length;
        }
        assertEquals(result.length, offset);
        assertTrue(java.util.stream.IntStream.range(0, result.length).anyMatch(i -> result[i] != 0));
        return result;
    }

    @Test void sixtyUpdatesEmitExactlyOneSecondAndDialogSharesOutput() throws Exception {
        byte[] rom;
        try (var stream = getClass().getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            assertNotNull(stream);
            rom = stream.readAllBytes();
        }
        List<short[]> chunks = new ArrayList<>();
        PcmSoundOutput output = (pcm, rate) -> {
            assertEquals(44101, rate);
            chunks.add(pcm);
        };
        try (var player = new FrameAudioPlayer(rom, 44101, output)) {
            for (int i = 0; i < 60; i++) player.update();
            assertEquals(60, chunks.size());
            assertEquals(88202, chunks.stream().mapToInt(a -> a.length).sum());
            short[] dialog = {12, -12};
            player.playDialogPcm(dialog, 44101);
            assertSame(dialog, chunks.get(60));
        }
    }
}
