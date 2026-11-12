package linksawakening.app.flow;

import java.util.Objects;
import linksawakening.app.input.SessionInput;
import linksawakening.app.presentation.FramePresentation;
import linksawakening.app.screen.ScreenSet;
import linksawakening.app.state.SaveState;

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
