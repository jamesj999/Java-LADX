package linksawakening.app;

import java.util.Objects;
import linksawakening.app.debug.*;
import linksawakening.app.gameplay.*;
import linksawakening.app.host.*;
import linksawakening.app.screen.*;
import linksawakening.app.story.*;
import linksawakening.world.transition.*;

/** Retained application flows, available to headless diagnostics and setup. */
public record SessionFlows(
        SaveCoordinator saves,
        ScreenRouter router,
        InputRouter inputRouter,
        RenderStateAssembler renderer,
        GameplaySimulation simulation,
        GameplayInteractions interactions,
        DebugTeleporter teleporter,
        MusicDirector music) {
    public SessionFlows {
        Objects.requireNonNull(saves);
        Objects.requireNonNull(router);
        Objects.requireNonNull(inputRouter);
        Objects.requireNonNull(renderer);
        Objects.requireNonNull(simulation);
        Objects.requireNonNull(interactions);
        Objects.requireNonNull(teleporter);
        Objects.requireNonNull(music);
    }
}
