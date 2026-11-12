package linksawakening.app.input;

import java.util.Objects;
import linksawakening.app.debug.DebugLog;
import linksawakening.app.state.DebugState;
import linksawakening.entity.Link;

/** Keeps the session debug flag and Link's motion gate synchronized. */
public final class NoClipControl implements DebugActions {
    private final DebugState state;
    private final Link link;

    public NoClipControl(DebugState state, Link link) {
        this.state = Objects.requireNonNull(state);
        this.link = Objects.requireNonNull(link);
    }

    public void toggleNoClip() {
        state.debugNoClip = !state.debugNoClip;
        link.motion().setDebugNoClip(state.debugNoClip);
        DebugLog.log("Debug no-clip: " + (state.debugNoClip ? "ON" : "OFF"));
    }
}
