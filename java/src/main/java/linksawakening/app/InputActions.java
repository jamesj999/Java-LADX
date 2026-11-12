package linksawakening.app;

import java.util.Objects;
import linksawakening.app.debug.*;
import linksawakening.app.gameplay.*;
import linksawakening.app.host.*;
import linksawakening.app.screen.*;
import linksawakening.app.story.*;
import linksawakening.rom.RomTables;
import linksawakening.world.transition.*;

/** Input-routing destinations, separate from live input state. */
public record InputActions(
        RomTables tables,
        ScreenNavigator navigator,
        SessionLifecycle lifecycle,
        DebugActions debug,
        SaveActions saves,
        OpeningStory story,
        DialogInteractions dialogs) {
    public InputActions {
        Objects.requireNonNull(tables);
        Objects.requireNonNull(navigator);
        Objects.requireNonNull(lifecycle);
        Objects.requireNonNull(debug);
        Objects.requireNonNull(saves);
        Objects.requireNonNull(story);
        Objects.requireNonNull(dialogs);
    }
}
