package linksawakening.gameplay;

public interface PcmSoundOutput extends AutoCloseable {

    void play(short[] stereoPcm, int sampleRate);

    @Override
    default void close() {
    }
}
