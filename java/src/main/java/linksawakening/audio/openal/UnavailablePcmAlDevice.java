package linksawakening.audio.openal;

/** Inert native boundary after device initialization fails; diagnostic queries report unavailable. */
enum UnavailablePcmAlDevice implements PcmAlDevice {
    INSTANCE;

    @Override
    public void makeCurrent() {}

    @Override
    public int state() {
        return -1;
    }

    @Override
    public int queued() {
        return -1;
    }

    @Override
    public int processed() {
        return -1;
    }

    @Override
    public int createBuffer(short[] pcm, int sampleRate) {
        return 0;
    }

    @Override
    public void queue(int buffer) {}

    @Override
    public int unqueue() {
        return 0;
    }

    @Override
    public void deleteBuffer(int buffer) {}

    @Override
    public void play() {}

    @Override
    public void stop() {}

    @Override
    public void rewind() {}

    @Override
    public void resetSource() {}

    @Override
    public boolean connected() {
        return false;
    }

    @Override
    public void close() {}
}
