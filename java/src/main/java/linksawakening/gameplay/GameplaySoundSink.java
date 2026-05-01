package linksawakening.gameplay;

public interface GameplaySoundSink {
    void play(GameplaySoundEvent event);

    static GameplaySoundSink none() {
        return event -> {
        };
    }
}
