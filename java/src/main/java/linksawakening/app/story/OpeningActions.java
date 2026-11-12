package linksawakening.app.story;

import java.util.Objects;
import linksawakening.app.gameplay.DialogBlockers;

/** Opening-story presentation and dialog gate dependencies. */
public record OpeningActions(int[] shieldPalette, DialogBlockers blockers, MusicRequests music) {
    public OpeningActions {
        Objects.requireNonNull(shieldPalette);
        Objects.requireNonNull(blockers);
        Objects.requireNonNull(music);
    }
}
