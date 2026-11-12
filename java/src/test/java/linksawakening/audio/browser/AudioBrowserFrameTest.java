package linksawakening.audio.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.audio.music.MusicTrack;
import org.junit.jupiter.api.Test;

final class AudioBrowserFrameTest {
    @Test
    void filterTracksByNameOrHexId() {
        MusicBrowserModel model = new MusicBrowserModel(List.of(
                new MusicTrack(0x01, "MUSIC_TITLE_SCREEN", 0x1B, 0x4077, 0x5000, 0x6C000),
                new MusicTrack(0x04, "MUSIC_MABE_VILLAGE", 0x1B, 0x407D, 0x5100, 0x6C100),
                new MusicTrack(0x11, "MUSIC_FILE_SELECT", 0x1E, 0x407F, 0x5200, 0x78200)));

        assertEquals(1, model.filteredTracks("mabe").size());
        assertEquals(1, model.filteredTracks("0x11").size());
        assertEquals(3, model.filteredTracks("").size());
    }

    @Test
    void selectedTrackMetadataIncludesBankAndHeader() {
        MusicBrowserModel model = new MusicBrowserModel(List.of());
        MusicTrack track = new MusicTrack(0x04, "MUSIC_MABE_VILLAGE", 0x1B, 0x407D, 0x5100, 0x6C100);

        String metadata = model.metadata(track);

        assertTrue(metadata.contains("0x04"));
        assertTrue(metadata.contains("MUSIC_MABE_VILLAGE"));
        assertTrue(metadata.contains("bank 0x1B"));
        assertTrue(metadata.contains("header 0x5100"));
    }

    @Test
    void transportControlsStayInactiveUntilPlaybackStarts() {
        BrowserTransportState transport = new BrowserTransportState();

        TransportControls initial = transport.controls(true, true);
        assertTrue(initial.playEnabled());
        assertEquals(false, initial.pauseResumeEnabled());
        assertEquals(false, initial.stopEnabled());
        assertEquals("Pause", initial.pauseResumeText());

        transport.togglePauseResume();
        assertEquals("Pause", transport.controls(true, true).pauseResumeText());

        transport.playbackStarted();
        TransportControls playing = transport.controls(true, true);
        assertTrue(playing.pauseResumeEnabled());
        assertTrue(playing.stopEnabled());
        assertEquals("Pause", playing.pauseResumeText());

        transport.togglePauseResume();
        assertEquals("Resume", transport.controls(true, true).pauseResumeText());

        transport.playbackStopped();
        TransportControls stopped = transport.controls(true, true);
        assertTrue(stopped.playEnabled());
        assertEquals(false, stopped.pauseResumeEnabled());
        assertEquals(false, stopped.stopEnabled());
        assertEquals("Pause", stopped.pauseResumeText());
    }

    @Test
    void soundEffectPreviewResetsMusicTransportState() {
        BrowserTransportState transport = new BrowserTransportState();
        transport.playbackStarted();
        transport.togglePauseResume();

        transport.soundEffectPreviewStarted();

        TransportControls controls = transport.controls(true, true);
        assertTrue(controls.playEnabled());
        assertEquals(false, controls.pauseResumeEnabled());
        assertEquals(false, controls.stopEnabled());
        assertEquals("Pause", controls.pauseResumeText());
    }
}
