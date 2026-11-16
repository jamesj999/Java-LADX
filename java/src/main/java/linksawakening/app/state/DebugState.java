package linksawakening.app.state;

/** Mutable debug state owned by one session. */
public final class DebugState {
    public boolean debugNoClip;

    public void setNoClip(boolean enabled) {
        debugNoClip = enabled;
    }
}
