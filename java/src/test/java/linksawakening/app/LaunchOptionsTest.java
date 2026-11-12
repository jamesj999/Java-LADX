package linksawakening.app;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LaunchOptionsTest {
    @Test
    void defaultsPreserveDesktopAndNativeRecordingResolution() {
        assertFalse(LaunchOptions.parse(new String[0]).headless());
        assertNull(LaunchOptions.parse(new String[0]).recording());
        var options = LaunchOptions.parse(new String[] {"--headless", "--frames", "120", "--record", "clip.mp4"});
        assertTrue(options.headless());
        assertEquals(120, options.frames());
        assertEquals(1, options.recording().scale());
        assertEquals(18, options.recording().crf());
    }

    @Test
    void rejectsUnboundedHeadlessRunsAndInvalidEncodingOptionsBeforeOpeningResources() {
        for (String[] args : new String[][] {
            {"--headless"},
            {"--frames", "0"},
            {"--frames", "-1"},
            {"--record"},
            {"--record", "x.webm"},
            {"--record-scale", "2"},
            {"--record", "x.mp4", "--record-scale", "0"},
            {"--record", "x.mp4", "--record-crf", "52"},
            {"--unknown"}
        }) assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse(args));
        assertTrue(LaunchOptions.parse(new String[] {"--headless", "--help"}).help());
    }
}
