package linksawakening.gameplay;

/**
 * Session audio destination for named gameplay effects; implementations may record requests in headless
 * tests.
 */
public interface GameplaySoundSink {
    void play(GameplaySoundEvent event);

    static GameplaySoundSink none() {
        return event -> {};
    }
}
