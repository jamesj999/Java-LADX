package linksawakening.gameplay;

import linksawakening.dialog.DialogController;

/** Stateless drain of pending dialog sounds into the supplied session audio sink. */
public final class DialogSoundRouter {

    private DialogSoundRouter() {}

    public static void routePending(DialogController dialogController, DialogSoundSink soundSink) {
        if (dialogController == null || soundSink == null) {
            return;
        }
        var events = dialogController.consumeSoundEvents();
        for (int i = 0; i < events.size(); i++) soundSink.play(events.get(i));
    }
}
