package linksawakening.audio;

import static linksawakening.audio.TestPcm.renderPcm;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.MessageDigest;
import java.util.HexFormat;
import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.music.MusicCatalog;
import linksawakening.audio.music.MusicDriver;
import linksawakening.audio.sfx.SoundEffectCatalog;
import linksawakening.audio.sfx.SoundEffectPlayer;
import linksawakening.rom.Rom;
import linksawakening.testing.RequiresRom;
import linksawakening.testing.TestRom;
import org.junit.jupiter.api.Test;

@RequiresRom
final class AudioGoldenTest {
    private static final String[] EXPECTED = {
        "f26ab4a926707ee12b44895b52fca1c3699a7741dbd553c1ffee3d0c75e3df8d:cf5e674272b34112969c516d045adf713b261a4feefe0eb35ac7af8fc16b2fcb",
        "66d1767106234c754331b0623ae9ea19b5db40fa13f146e073a74cf4f228e985:c2807cda2529825013434dcaa7e1a38c2e14bd8aba26fdf29f3df3316bb6c16e",
        "1243be3f843cbeae138001d166221329ba20f3046fe377c5743cff5f197daec5:7d081feeaaf8ed085691f7147853e6292c172a2c8c5da48a4f693e3c5b9d6ae8",
        "1834a3e1fbdf4e060b804868c3760cf675948312190a0adea52f1e064594c4cb:8d7ee4c67b57e38abb49385df90befd79472b354208eac01d79055a695a9cac3",
        "0b249572859f5dda0582192f4b00806fccb8c62b2d0aba0dcd2b638fba902563:b6e8dc18f2ace269ebb22c636a9ab1caf5d737fccee1299b5e744389439b7923",
        "e50398f2993b241a84e9e904f5a0455c89b97ba9123f079ebbb3761297a7847a:6e0bc9e96d64be84d5bb050229bba8fe11ea0dbc3769a27026262319c7ea90a2"
    };

    @Test
    void registerAndPcmStreamsRemainIdentical() throws Exception {
        byte[] rom;
        try (var input = TestRom.open()) {
            rom = input.readAllBytes();
        }
        var tracks = MusicCatalog.fromRom(Rom.of(rom)).tracks();
        var effects = SoundEffectCatalog.fromRom(Rom.of(rom)).effects();
        for (int scenario = 0; scenario < 6; scenario++) {
            var writes = MessageDigest.getInstance("SHA-256");
            var pcm = MessageDigest.getInstance("SHA-256");
            var apu = new GameBoyApu(48000);
            apu.setPrimitiveRegisterObserver((register, value) -> {
                writes.update((byte) (int) register);
                writes.update((byte) (int) value);
            });
            var music = new MusicDriver(Rom.of(rom), apu);
            var sfx = new SoundEffectPlayer(apu, Rom.of(rom));
            music.setSoundEffectState(sfx::ownsChannel, sfx::takeWaveRestoreRequest);
            if (scenario < 3) music.start(tracks.get(new int[] {0, 20, 50}[scenario]));
            else sfx.play(effects.get(new int[] {0, 70, 110}[scenario - 3]));
            for (int frame = 0; frame < 240; frame++) {
                writes.update((byte) frame);
                music.tick60Hz();
                sfx.tick60Hz();
                for (short sample : renderPcm(apu, 800)) {
                    pcm.update((byte) sample);
                    pcm.update((byte) (sample >>> 8));
                }
            }
            String actual = HexFormat.of().formatHex(writes.digest()) + ":"
                    + HexFormat.of().formatHex(pcm.digest());
            assertEquals(EXPECTED[scenario], actual, "scenario " + scenario);
        }
    }
}
