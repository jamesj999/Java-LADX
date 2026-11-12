package linksawakening.app;

import java.util.Objects;
import linksawakening.app.gameplay.DialogSoundRouting;
import linksawakening.app.story.MusicRequests;

/** The concrete services shared by screen constructors. */
public record ScreenDependencies(
        UiServices ui,
        InputServices input,
        ScreenState screens,
        SaveState saveState,
        DialogSoundRouting dialogSounds,
        MusicRequests music) {
    public ScreenDependencies {
        Objects.requireNonNull(ui);
        Objects.requireNonNull(input);
        Objects.requireNonNull(screens);
        Objects.requireNonNull(saveState);
        Objects.requireNonNull(dialogSounds);
        Objects.requireNonNull(music);
    }
}
