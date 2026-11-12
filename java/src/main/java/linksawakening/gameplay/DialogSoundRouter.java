package linksawakening.gameplay;

import linksawakening.dialog.DialogController;

/** Dialog sound router behavior and state. */
public final class DialogSoundRouter {

    private DialogSoundRouter() {}

    public static void routePending(DialogController dialogController, DialogSoundSink soundSink) {
        if (dialogController == null || soundSink == null) {
            return;
        }
        for (DialogController.SoundEvent event : dialogController.consumeSoundEvents()) {
            soundSink.play(event);
        }
    }
}
