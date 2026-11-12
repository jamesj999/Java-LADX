package linksawakening.app.flow;

import java.util.Objects;
import linksawakening.app.input.SessionInput;
import linksawakening.app.presentation.FramePresentation;
import linksawakening.app.screen.ScreenSet;
import linksawakening.app.state.SaveState;

/**
 * Session-long entry points used by the frame loop: screens, presentation, key dispatch, shutdown,
 * controllers and save storage.
 */
public record FrameLoopServices(
        ScreenSet screens,
        FramePresentation presentation,
        SessionInput input,
        SessionShutdown shutdown,
        SessionControllers flows,
        SaveState saves) {
    public FrameLoopServices {
        Objects.requireNonNull(screens);
        Objects.requireNonNull(presentation);
        Objects.requireNonNull(input);
        Objects.requireNonNull(shutdown);
        Objects.requireNonNull(flows);
        Objects.requireNonNull(saves);
    }
}
