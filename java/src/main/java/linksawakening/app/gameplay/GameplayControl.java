package linksawakening.app.gameplay;

import java.util.Objects;
import linksawakening.app.AudioServices;
import linksawakening.app.MusicDirector;

/** Named flow ports required by simulation phases. */
public record GameplayControl(
        AudioServices audio,
        MusicDirector music,
        GameplayInteractions interactions,
        PassOutController passOut,
        GameOverActions gameOver,
        DialogBlockers blockers,
        DebugScroll debugScroll,
        DialogSoundRouting dialogSounds) {
    public GameplayControl {
        Objects.requireNonNull(audio);
        Objects.requireNonNull(music);
        Objects.requireNonNull(interactions);
        Objects.requireNonNull(passOut);
        Objects.requireNonNull(gameOver);
        Objects.requireNonNull(blockers);
        Objects.requireNonNull(debugScroll);
        Objects.requireNonNull(dialogSounds);
    }
}
