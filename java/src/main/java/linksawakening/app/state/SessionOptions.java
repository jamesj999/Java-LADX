package linksawakening.app.state;

import java.util.Objects;
import linksawakening.config.AppConfig;

/**
 * Mutable launch options, also used for headless scenario setup. One instance is retained for the owning
 * session.
 */
public final class SessionOptions {
    private AppConfig appConfig;

    public SessionOptions(AppConfig config) {
        replace(config);
    }

    public AppConfig config() {
        return appConfig;
    }

    public void replace(AppConfig config) {
        this.appConfig = Objects.requireNonNull(config);
    }
}
