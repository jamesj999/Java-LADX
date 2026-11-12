package linksawakening.app.gameplay;

import java.util.Objects;
import linksawakening.app.audio.MusicDirector;
import linksawakening.app.gameplay.interaction.DialogBlockers;
import linksawakening.app.gameplay.interaction.DialogSoundRouting;
import linksawakening.app.gameplay.interaction.GameplayInteractions;
import linksawakening.app.gameplay.recovery.GameOverActions;
import linksawakening.app.gameplay.recovery.PassOutController;
import linksawakening.app.services.AudioServices;

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
