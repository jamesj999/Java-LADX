package linksawakening.app;

import java.util.Objects;
import linksawakening.gameplay.GameplaySoundSink;

/** Replaceable application effect sink for headless recording and gameplay routing. */
public final class GameplaySounds {
    private GameplaySoundSink sink = GameplaySoundSink.none();

    public GameplaySoundSink sink() {
        return sink;
    }

    public void replace(GameplaySoundSink value) {
        sink = Objects.requireNonNull(value);
    }
}
