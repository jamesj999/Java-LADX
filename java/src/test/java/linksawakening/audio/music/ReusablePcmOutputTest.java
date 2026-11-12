package linksawakening.audio.music;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import linksawakening.gameplay.PcmSoundOutput;
import linksawakening.rom.Rom;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import org.junit.jupiter.api.Test;

@RequiresRom
final class ReusablePcmOutputTest {
    @Test
    void synchronousOutputMatchesLegacyPcmWhileLegacyRetainsOwnedArrays() throws Exception {
        byte[] rom;
        try (var input = TestRom.open()) {
            rom = input.readAllBytes();
        }
        List<short[]> legacySamples = new ArrayList<>();
        List<short[]> synchronousSamples = new ArrayList<>();
        PcmSoundOutput legacy = (pcm, rate) -> legacySamples.add(pcm);
        ReusableFramePcmOutput synchronous = (pcm, rate) -> synchronousSamples.add(pcm.clone());
        var first = new FrameAudioPlayer(Rom.of(rom), 48001, legacy);
        var second = new FrameAudioPlayer(Rom.of(rom), 48001, synchronous);
        var track = MusicCatalog.fromRom(Rom.of(rom)).tracks().getFirst();
        first.play(track);
        second.play(track);
        for (int i = 0; i < 65; i++) {
            first.update();
            second.update();
        }
        for (int i = 0; i < 65; i++) assertArrayEquals(legacySamples.get(i), synchronousSamples.get(i));
        assertNotSame(legacySamples.get(0), legacySamples.get(1));
    }
}
