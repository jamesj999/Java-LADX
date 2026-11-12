package linksawakening.app;

import java.util.Objects;
import linksawakening.app.debug.*;
import linksawakening.app.gameplay.*;
import linksawakening.app.host.*;
import linksawakening.app.screen.*;
import linksawakening.app.story.*;
import linksawakening.world.transition.*;

/** Mutable state shared by session owners; values are read at use time. */
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
