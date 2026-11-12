package linksawakening.app.flow;

import java.util.Objects;
import linksawakening.app.host.Host;
import linksawakening.app.services.SessionServices;
import linksawakening.app.state.RunState;
import linksawakening.app.state.SessionState;

/**
 * Immutable result of session wiring: subsystem services, shared mutable state, frame-loop entry points,
 * host and stop state. Lives until session close.
 */
public record SessionContext(
        SessionServices services, SessionState state, FrameLoopServices runtime, Host host, RunState lifecycle) {
    public SessionContext {
        Objects.requireNonNull(services);
        Objects.requireNonNull(state);
        Objects.requireNonNull(runtime);
        Objects.requireNonNull(host);
        Objects.requireNonNull(lifecycle);
    }
}
