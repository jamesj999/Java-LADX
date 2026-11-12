package linksawakening.app;

import java.util.Objects;
import linksawakening.audio.music.FrameMusicPlayer;
import linksawakening.audio.music.GameplayMusicController;
import linksawakening.gameplay.DialogSoundSink;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gameplay.PcmSoundOutput;

/** Long-lived audio services constructed before the first frame. */
public record AudioServices(
        FrameMusicPlayer musicPlayer,
        GameplayMusicController gameplayMusicController,
        DialogSoundSink dialogSoundSink,
        GameplaySoundSink gameplaySoundSink,
        PcmSoundOutput dialogSoundOutput) {
    public AudioServices {
        Objects.requireNonNull(musicPlayer);
        Objects.requireNonNull(gameplayMusicController);
        Objects.requireNonNull(dialogSoundSink);
        Objects.requireNonNull(gameplaySoundSink);
        Objects.requireNonNull(dialogSoundOutput);
    }
}
