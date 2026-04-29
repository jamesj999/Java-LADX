package linksawakening.audio.browser;

import linksawakening.audio.music.MusicTrack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AudioBrowserFrameTest {
    @Test
    void filterTracksByNameOrHexId() {
        AudioBrowserFrame.BrowserModel model = new AudioBrowserFrame.BrowserModel(List.of(
                new MusicTrack(0x01, "MUSIC_TITLE_SCREEN", 0x1B, 0x4077, 0x5000, 0x6C000),
                new MusicTrack(0x04, "MUSIC_MABE_VILLAGE", 0x1B, 0x407D, 0x5100, 0x6C100),
                new MusicTrack(0x11, "MUSIC_FILE_SELECT", 0x1E, 0x407F, 0x5200, 0x78200)));

        assertEquals(1, model.filteredTracks("mabe").size());
        assertEquals(1, model.filteredTracks("0x11").size());
        assertEquals(3, model.filteredTracks("").size());
    }

    @Test
    void selectedTrackMetadataIncludesBankAndHeader() {
        AudioBrowserFrame.BrowserModel model = new AudioBrowserFrame.BrowserModel(List.of());
        MusicTrack track = new MusicTrack(0x04, "MUSIC_MABE_VILLAGE", 0x1B, 0x407D, 0x5100, 0x6C100);

        String metadata = model.metadata(track);

        assertTrue(metadata.contains("0x04"));
        assertTrue(metadata.contains("MUSIC_MABE_VILLAGE"));
        assertTrue(metadata.contains("bank 0x1B"));
        assertTrue(metadata.contains("header 0x5100"));
    }

    @Test
    void transportControlsStayInactiveUntilPlaybackStarts() {
        AudioBrowserFrame.TransportState transport = new AudioBrowserFrame.TransportState();

        AudioBrowserFrame.TransportControls initial = transport.controls(true, true);
        assertTrue(initial.playEnabled());
        assertEquals(false, initial.pauseResumeEnabled());
        assertEquals(false, initial.stopEnabled());
        assertEquals("Pause", initial.pauseResumeText());

        transport.togglePauseResume();
        assertEquals("Pause", transport.controls(true, true).pauseResumeText());

        transport.playbackStarted();
        AudioBrowserFrame.TransportControls playing = transport.controls(true, true);
        assertTrue(playing.pauseResumeEnabled());
        assertTrue(playing.stopEnabled());
        assertEquals("Pause", playing.pauseResumeText());

        transport.togglePauseResume();
        assertEquals("Resume", transport.controls(true, true).pauseResumeText());

        transport.playbackStopped();
        AudioBrowserFrame.TransportControls stopped = transport.controls(true, true);
        assertTrue(stopped.playEnabled());
        assertEquals(false, stopped.pauseResumeEnabled());
        assertEquals(false, stopped.stopEnabled());
        assertEquals("Pause", stopped.pauseResumeText());
    }
}
