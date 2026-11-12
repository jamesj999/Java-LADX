package linksawakening.app;

import java.util.Objects;
import linksawakening.app.gameplay.DialogInteractions;
import linksawakening.app.screen.SaveActions;
import linksawakening.app.screen.ScreenNavigator;
import linksawakening.app.screen.SessionLifecycle;
import linksawakening.app.story.OpeningStory;
import linksawakening.rom.RomTables;

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
