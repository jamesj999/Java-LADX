package linksawakening.audio.openal;

final class PendingOpenAlBuffer {

    private int buffer;
    private boolean tracked;

    PendingOpenAlBuffer(int buffer) {
        this.buffer = buffer;
    }

    int id() {
        return buffer;
    }

    void markTracked() {
        tracked = true;
    }

    int releaseUntracked() {
        if (buffer == 0 || tracked) {
            return 0;
        }
        int released = buffer;
        buffer = 0;
        return released;
    }
}
