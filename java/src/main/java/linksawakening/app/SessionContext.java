package linksawakening.app;

import java.util.Objects;
import linksawakening.app.host.Host;

/** Immutable composition result; all behavior belongs to the referenced owners. */
public record SessionContext(
        SessionServices services, SessionState state, SessionRuntime runtime, Host host, RunState lifecycle) {
    public SessionContext {
        Objects.requireNonNull(services);
        Objects.requireNonNull(state);
        Objects.requireNonNull(runtime);
        Objects.requireNonNull(host);
        Objects.requireNonNull(lifecycle);
    }
}
