package linksawakening.gameplay;

import linksawakening.dialog.DialogController;

/** Session audio destination for dialog sounds; recording implementations support headless checks. */
public interface DialogSoundSink {
    void play(DialogController.SoundEvent event);
}
