package linksawakening.gameplay;

/**
 * Session audio destination for named gameplay effects; implementations may record requests in headless
 * tests.
 */
public interface GameplaySoundSink {

    static GameplaySoundSink none() {
        return event -> {};
    }

    void play(GameplaySoundEvent event);

    default void playRomSound(linksawakening.audio.sfx.SoundEffectNamespace namespace, int id) {}
}
