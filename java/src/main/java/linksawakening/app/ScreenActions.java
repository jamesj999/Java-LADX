package linksawakening.app;

import java.util.Objects;
import linksawakening.app.screen.SaveActions;
import linksawakening.app.screen.ScreenNavigator;

/** Navigation and persistence destinations used by screen transitions. */
public record ScreenActions(ScreenNavigator navigator, SaveActions saves, SaveState saveState, MusicDirector music) {
    public ScreenActions {
        Objects.requireNonNull(navigator);
        Objects.requireNonNull(saves);
        Objects.requireNonNull(saveState);
        Objects.requireNonNull(music);
    }
}
