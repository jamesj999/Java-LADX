package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import linksawakening.config.AppConfig;
import linksawakening.state.PlayerState;

/** Compatibility names for tests outside the app ownership boundary. */
public final class ScreenRouter {
    private ScreenRouter() {}

    public static void applyConfiguredItemProfile(AppConfig config, PlayerState playerState) {
        linksawakening.app.flow.ScreenRouter.applyConfiguredItemProfile(config, playerState);
    }

    public static void dispatchConfiguredGameplay(
            AppConfig config, Runnable startNewGame, Runnable startConfiguredLocation) {
        linksawakening.app.flow.ScreenRouter.dispatchConfiguredGameplay(config, startNewGame, startConfiguredLocation);
    }
}
