package linksawakening.app.input;

import java.util.Objects;
import linksawakening.app.debug.DebugMenu;
import linksawakening.app.debug.DebugTools;
import linksawakening.app.screen.ScreenSet;
import linksawakening.app.services.InputServices;
import linksawakening.app.services.PlayerServices;
import linksawakening.app.services.WorldServices;
import linksawakening.app.state.SessionState;

/**
 * Routes debug-menu input before ordinary screen input, preserving edge ownership. One instance is retained
 * for the owning session.
 */
public final class SessionInput {
    private final SessionState state;
    private final InputServices input;
    private final WorldServices world;
    private final PlayerServices player;
    private final ScreenSet screenSet;
    private final InputRouter router;
    private final DebugTools debugTools;

    public SessionInput(
            SessionState state,
            InputServices input,
            WorldServices world,
            PlayerServices player,
            ScreenSet screenSet,
            InputRouter router,
            DebugTools debugTools) {
        this.state = Objects.requireNonNull(state);
        this.input = Objects.requireNonNull(input);
        this.world = Objects.requireNonNull(world);
        this.player = Objects.requireNonNull(player);
        this.screenSet = Objects.requireNonNull(screenSet);
        this.router = Objects.requireNonNull(router);
        this.debugTools = Objects.requireNonNull(debugTools);
    }

    public void onKeyEvent(int key, int action) {
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
                debugTools
                        .menu()
                        .open(
                                world.roomSession().rooms().mapCategory(),
                                world.roomSession().rooms().mapId(),
                                world.roomSession().rooms().currentRoomId(),
                                player.link().position().pixelX(),
                                player.link().position().pixelY());
                input.inputState().clear();
                screenSet.gameplay().clearPendingInput();
            }
            return;
        }
        router.onKeyEvent(key, action);
    }
}
