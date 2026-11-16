package linksawakening.audio.sfx;

/** Optional SFX transport capability, represented by an explicit silent implementation. */
public interface SoundEffectPlayback {

    static SoundEffectPlayback none() {
        return Silent.INSTANCE;
    }

    boolean available();

    boolean isPlaying();

    void play(SoundEffect effect);

    void tick60Hz();

    void stop();

    /** A transport without an SFX engine; never owns channels or advances an APU. */
    enum Silent implements SoundEffectPlayback {
        INSTANCE;

        @Override
        public boolean available() {
            return false;
        }

        @Override
        public boolean isPlaying() {
            return false;
        }

        @Override
        public void play(SoundEffect effect) {}

        @Override
        public void tick60Hz() {}

        @Override
        public void stop() {}
    }
}
