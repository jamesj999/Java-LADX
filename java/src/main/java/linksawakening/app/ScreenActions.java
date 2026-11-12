package linksawakening.app;

import java.util.Objects;
import linksawakening.app.debug.*;
import linksawakening.app.gameplay.*;
import linksawakening.app.host.*;
import linksawakening.app.screen.*;
import linksawakening.app.story.*;
import linksawakening.world.transition.*;

/** Navigation and persistence destinations used by screen transitions. */
public record ScreenActions(ScreenNavigator navigator, SaveActions saves, SaveState saveState, MusicDirector music) {
    public ScreenActions {
        Objects.requireNonNull(navigator);
        Objects.requireNonNull(saves);
        Objects.requireNonNull(saveState);
        Objects.requireNonNull(music);
    }
}
