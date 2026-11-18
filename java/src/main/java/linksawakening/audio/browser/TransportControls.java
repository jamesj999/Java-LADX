package linksawakening.audio.browser;
/** Availability and labels presented by the browser transport. */
record TransportControls(
        boolean playEnabled, boolean pauseResumeEnabled, boolean stopEnabled, String pauseResumeText) {}
