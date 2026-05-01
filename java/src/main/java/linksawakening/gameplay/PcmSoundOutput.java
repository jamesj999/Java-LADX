package linksawakening.gameplay;

public interface PcmSoundOutput extends AutoCloseable {

    void play(short[] stereoPcm, int sampleRate);

    default void playReplacing(short[] stereoPcm, int sampleRate) {
        play(stereoPcm, sampleRate);
    }

    @Override
    default void close() {
    }
}
