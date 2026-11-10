package linksawakening.audio.openal;

/** Native boundary for the PCM transport; fake devices exercise recovery without OpenAL. */
interface PcmAlDevice extends AutoCloseable {
    int INITIAL = 0x1011, PLAYING = 0x1012, PAUSED = 0x1013, STOPPED = 0x1014;
    void makeCurrent();
    int state();
    int queued();
    int processed();
    int createBuffer(short[] pcm, int sampleRate);
    void queue(int buffer);
    int unqueue();
    void deleteBuffer(int buffer);
    void play();
    void stop();
    void rewind();
    void resetSource();
    boolean connected();
    @Override void close();
}
