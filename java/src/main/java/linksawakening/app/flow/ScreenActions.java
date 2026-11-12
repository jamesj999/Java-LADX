package linksawakening.app.flow;

import java.util.Objects;
import linksawakening.app.audio.MusicDirector;
import linksawakening.app.screen.SaveActions;
import linksawakening.app.screen.ScreenNavigator;
import linksawakening.app.state.SaveState;

/**
 * Navigation and persistence destinations used by screen transitions. The immutable bundle is retained for
 * the owning session.
 */
public record ScreenActions(ScreenNavigator navigator, SaveActions saves, SaveState saveState, MusicDirector music) {
    public ScreenActions {
        Objects.requireNonNull(navigator);
        Objects.requireNonNull(saves);
        Objects.requireNonNull(saveState);
        Objects.requireNonNull(music);
    }
}
