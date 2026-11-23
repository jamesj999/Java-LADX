package linksawakening.audio.openal;

/** Device-independent startup/recovery policy for frame-fed PCM. */
final class PcmStreamBuffering {
    // Three 60 Hz gameplay frames: one playing, two covering producer scheduling jitter.
    private static final int PREFILL_FRAMES = 3;
    private boolean started;
    private long underruns;

    void observePlaying(boolean playing) {
        if (started && !playing) {
            underruns++;
            started = false;
        }
    }

    boolean ready(int queuedFrames, int sampleRate) {
        return queuedFrames >= (sampleRate * (long) PREFILL_FRAMES + 59) / 60;
    }

    void started() {
        started = true;
    }

    void reset() {
        started = false;
    }

    long underrunCount() {
        return underruns;
    }
}
