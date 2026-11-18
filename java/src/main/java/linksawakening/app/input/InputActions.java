package linksawakening.app.input;

import java.util.Objects;
import linksawakening.app.gameplay.interaction.DialogInteractions;
import linksawakening.app.screen.SaveActions;
import linksawakening.app.screen.ScreenNavigator;
import linksawakening.app.screen.SessionLifecycle;
import linksawakening.app.story.OpeningStory;
import linksawakening.rom.RomTables;

/**
 * Input-routing destinations, separate from live input state. The immutable bundle is retained for the
 * owning session.
 */
public record InputActions(
        RomTables tables,
        ScreenNavigator navigator,
        SessionLifecycle lifecycle,
        NoClipControl debug,
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
