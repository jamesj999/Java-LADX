package linksawakening.app.state;

import java.util.Objects;

/**
 * Mutable state shared by session owners; values are read at use time. The immutable bundle is retained for
 * the owning session.
 */
public record SessionState(
        ScreenState screens,
        SceneState scene,
        DebugState debug,
        FrameClock clock,
        OpeningStoryState opening,
        GameplayInputState input,
        SessionOptions options,
        GameplaySounds sounds) {
    public SessionState {
        Objects.requireNonNull(screens);
        Objects.requireNonNull(scene);
        Objects.requireNonNull(debug);
        Objects.requireNonNull(clock);
        Objects.requireNonNull(opening);
        Objects.requireNonNull(input);
        Objects.requireNonNull(options);
        Objects.requireNonNull(sounds);
    }
}
