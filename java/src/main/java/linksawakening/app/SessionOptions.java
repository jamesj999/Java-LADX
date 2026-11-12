package linksawakening.app;

import java.util.Objects;
import linksawakening.app.debug.*;
import linksawakening.app.gameplay.*;
import linksawakening.app.host.*;
import linksawakening.app.screen.*;
import linksawakening.app.story.*;
import linksawakening.config.AppConfig;
import linksawakening.world.transition.*;

/** Mutable launch options, also used for headless scenario setup. */
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
