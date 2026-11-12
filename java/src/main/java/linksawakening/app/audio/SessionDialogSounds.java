package linksawakening.app.audio;

import java.util.Objects;
import linksawakening.app.gameplay.interaction.DialogSoundRouting;
import linksawakening.dialog.DialogController;
import linksawakening.gameplay.DialogSoundRouter;
import linksawakening.gameplay.DialogSoundSink;

/** Adapts dialog requests to the session's audio sink. One instance is retained for the owning session. */
public final class SessionDialogSounds implements DialogSoundRouting {
    private final DialogController dialog;
    private final DialogSoundSink sink;

    public SessionDialogSounds(DialogController dialog, DialogSoundSink sink) {
        this.dialog = Objects.requireNonNull(dialog);
        this.sink = Objects.requireNonNull(sink);
    }

    public void route() {
        DialogSoundRouter.routePending(dialog, sink);
    }
}
