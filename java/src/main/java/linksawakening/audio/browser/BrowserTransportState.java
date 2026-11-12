package linksawakening.audio.browser;

import linksawakening.audio.browser.AudioBrowserFrame.TransportControls;

class BrowserTransportState {
    private boolean playbackActive;
    private boolean paused;

    public void playbackStarted() {
        playbackActive = true;
        paused = false;
    }

    public void playbackStopped() {
        playbackActive = false;
        paused = false;
    }

    public void soundEffectPreviewStarted() {
        playbackStopped();
    }

    public boolean playbackActive() {
        return playbackActive;
    }

    public boolean togglePauseResume() {
        if (!playbackActive) {
            paused = false;
            return false;
        }
        paused = !paused;
        return paused;
    }

    public TransportControls controls(boolean audioAvailable, boolean trackSelected) {
        boolean playbackControlsEnabled = audioAvailable && playbackActive;
        return new TransportControls(
                audioAvailable && trackSelected,
                playbackControlsEnabled,
                playbackControlsEnabled,
                playbackActive && paused ? "Resume" : "Pause");
    }
}
