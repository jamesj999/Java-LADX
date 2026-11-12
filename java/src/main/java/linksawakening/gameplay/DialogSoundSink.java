package linksawakening.gameplay;

import linksawakening.dialog.DialogController;

/** Contract for dialog sound sink. */
public interface DialogSoundSink {
    void play(DialogController.SoundEvent event);
}
