package linksawakening.audio.music;

import linksawakening.gameplay.PcmSoundOutput;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FrameAudioPlayerTest {
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
