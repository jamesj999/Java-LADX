package linksawakening.app;

import java.util.Objects;
import linksawakening.app.debug.*;
import linksawakening.app.gameplay.*;
import linksawakening.app.host.*;
import linksawakening.app.screen.*;
import linksawakening.app.story.*;
import linksawakening.world.transition.*;

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
