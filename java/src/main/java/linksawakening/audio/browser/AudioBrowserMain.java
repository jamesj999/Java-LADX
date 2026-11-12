package linksawakening.audio.browser;

import java.io.IOException;
import java.io.InputStream;
import javax.swing.SwingUtilities;
import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.music.MusicCatalog;
import linksawakening.audio.music.MusicDriver;
import linksawakening.audio.openal.OpenAlMusicPlayer;
import linksawakening.audio.sfx.SoundEffectCatalog;
import linksawakening.audio.sfx.SoundEffectPlayer;
import linksawakening.rom.Rom;

/** Standalone entry point for the Swing audio audition tool. */
public final class AudioBrowserMain {
    private static final int SAMPLE_RATE = 48_000;
    private static final String ROM_RESOURCE = "rom/azle.gbc";

    private AudioBrowserMain() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AudioBrowserMain::start);
    }

    private static void start() {
        OpenAlMusicPlayer player = null;
        try {
            Rom romData = loadRom();
            MusicCatalog catalog = MusicCatalog.fromRom(romData);
            GameBoyApu apu = new GameBoyApu(SAMPLE_RATE);
            MusicDriver driver = new MusicDriver(romData, apu);
            SoundEffectCatalog soundEffectCatalog = SoundEffectCatalog.fromRom(romData);
            SoundEffectPlayer soundEffectPlayer = new SoundEffectPlayer(apu, romData, soundEffectCatalog);
            player = new OpenAlMusicPlayer(driver, apu, soundEffectPlayer);
            AudioBrowserFrame frame = new AudioBrowserFrame(catalog, soundEffectCatalog, player, driver);
            frame.setVisible(true);
            player = null;
        } catch (RuntimeException e) {
            if (player != null) {
                player.close();
            }
            AudioBrowserFrame.showStartupError(e.getMessage());
        }
    }

    private static Rom loadRom() {
        try (InputStream stream = AudioBrowserMain.class.getClassLoader().getResourceAsStream(ROM_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing: " + ROM_RESOURCE);
            }
            return Rom.of(stream.readAllBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load ROM: " + e.getMessage(), e);
        }
    }
}
