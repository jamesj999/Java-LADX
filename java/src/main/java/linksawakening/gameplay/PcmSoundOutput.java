package linksawakening.gameplay;

/**
 * Session-owned PCM destination. Implementations receive interleaved samples and release their resources on
 * close.
 */
public interface PcmSoundOutput extends AutoCloseable {

    void play(short[] stereoPcm, int sampleRate);

    /** Continuous frame transport; outputs may prefill to absorb scheduling jitter. */
    default void playFrame(short[] stereoPcm, int sampleRate) {
        play(stereoPcm, sampleRate);
    }

    default void playReplacing(short[] stereoPcm, int sampleRate) {
        play(stereoPcm, sampleRate);
    }

    @Override
    default void close() {}
}
