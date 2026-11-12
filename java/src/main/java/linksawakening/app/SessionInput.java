package linksawakening.app;

import java.util.Objects;
import linksawakening.app.debug.DebugMenu;
import linksawakening.app.screen.ScreenSet;

/** Routes debug-menu input before ordinary screen input, preserving edge ownership. */
public final class SessionInput {
    private final SessionState state;
    private final InputServices input;
    private final WorldServices world;
    private final PlayerServices player;
    private final ScreenSet screenSet;
    private final InputRouter router;
    private final DebugTeleporter teleporter;

    public SessionInput(
            SessionState state,
            InputServices input,
            WorldServices world,
            PlayerServices player,
            ScreenSet screenSet,
            InputRouter router,
            DebugTeleporter teleporter) {
        this.state = Objects.requireNonNull(state);
        this.input = Objects.requireNonNull(input);
        this.world = Objects.requireNonNull(world);
        this.player = Objects.requireNonNull(player);
        this.screenSet = Objects.requireNonNull(screenSet);
        this.router = Objects.requireNonNull(router);
        this.teleporter = Objects.requireNonNull(teleporter);
    }

    void onKeyEvent(int key, int action) {
        if (state.debug().debugMenu != null && state.debug().debugMenu.isOpen()) {
            state.debug().debugMenu.key(key, action);
            input.inputState().clear();
            screenSet.gameplay().clearPendingInput();
            return;
        }
        if (state.options().config().debugEnabled() && DebugMenu.isToggle(key, action)) {
            if (state.screens().currentScreen == screenSet.gameplay()
                    && router.canPresentFileSave()
                    && !world.roomSession().doors().tailCaveKeyholeSequenceActive()) {
                if (state.debug().debugMenu == null)
                    state.debug().debugMenu = new DebugMenu(player.playerState(), world.roomSession(), teleporter);
                state.debug()
                        .debugMenu
                        .open(
                                world.roomSession().rooms().mapCategory(),
                                world.roomSession().rooms().mapId(),
                                world.roomSession().rooms().currentRoomId(),
                                player.link().pixelX(),
                                player.link().pixelY());
                input.inputState().clear();
                screenSet.gameplay().clearPendingInput();
            }
            return;
        }
        router.onKeyEvent(key, action);
    }
}
