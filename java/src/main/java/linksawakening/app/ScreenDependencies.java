package linksawakening.app;

import java.util.Objects;
import linksawakening.app.debug.*;
import linksawakening.app.gameplay.*;
import linksawakening.app.host.*;
import linksawakening.app.screen.*;
import linksawakening.app.story.*;
import linksawakening.world.transition.*;

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
