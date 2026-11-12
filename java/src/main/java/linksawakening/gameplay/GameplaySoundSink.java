package linksawakening.gameplay;

/** Contract for gameplay sound sink. */
public interface GameplaySoundSink {
    void play(GameplaySoundEvent event);

    static GameplaySoundSink none() {
        return event -> {};
    }
}
