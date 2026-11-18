package linksawakening.gameplay;

import linksawakening.audio.sfx.SoundEffectNamespace;

public interface PcmSoundOutput extends AutoCloseable {

    void play(short[] stereoPcm, int sampleRate);

    default void playReplacing(short[] stereoPcm, int sampleRate) {
        play(stereoPcm, sampleRate);
    }

    default void playReplacing(SoundEffectNamespace namespace, short[] stereoPcm, int sampleRate) {
        playReplacing(stereoPcm, sampleRate);
    }

    @Override
    default void close() {
    }
}
