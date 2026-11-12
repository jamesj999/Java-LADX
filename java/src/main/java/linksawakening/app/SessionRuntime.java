package linksawakening.app;

import java.util.Objects;
import linksawakening.app.debug.*;
import linksawakening.app.gameplay.*;
import linksawakening.app.host.*;
import linksawakening.app.screen.*;
import linksawakening.app.story.*;
import linksawakening.world.transition.*;

/** Fully wired runtime entry points; contains no session callbacks. */
public record SessionRuntime(
        ScreenSet screens,
        FramePresentation presentation,
        SessionInput input,
        SessionShutdown shutdown,
        SessionFlows flows,
        SaveState saves) {
    public SessionRuntime {
        Objects.requireNonNull(screens);
        Objects.requireNonNull(presentation);
        Objects.requireNonNull(input);
        Objects.requireNonNull(shutdown);
        Objects.requireNonNull(flows);
        Objects.requireNonNull(saves);
    }
}
