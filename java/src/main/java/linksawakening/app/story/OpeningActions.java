package linksawakening.app.story;

import java.util.Objects;
import linksawakening.app.*;
import linksawakening.app.debug.*;
import linksawakening.app.gameplay.*;
import linksawakening.app.host.*;
import linksawakening.app.screen.*;
import linksawakening.world.transition.*;

/** Opening-story presentation and dialog gate dependencies. */
public record OpeningActions(int[] shieldPalette, DialogBlockers blockers, MusicRequests music) {
    public OpeningActions {
        Objects.requireNonNull(shieldPalette);
        Objects.requireNonNull(blockers);
        Objects.requireNonNull(music);
    }
}
