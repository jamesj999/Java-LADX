package linksawakening.app.flow;

import java.util.Objects;
import linksawakening.app.audio.MusicDirector;
import linksawakening.app.debug.DebugTeleporter;
import linksawakening.app.gameplay.GameplaySimulation;
import linksawakening.app.gameplay.interaction.GameplayInteractions;
import linksawakening.app.input.InputRouter;
import linksawakening.app.presentation.RenderStateAssembler;

/**
 * Session-long domain controllers exposed for delegation and headless setup; contains no mutable state of
 * its own.
 */
public record SessionControllers(
        SaveCoordinator saves,
        ScreenRouter router,
        InputRouter inputRouter,
        RenderStateAssembler renderer,
        GameplaySimulation simulation,
        GameplayInteractions interactions,
        DebugTeleporter teleporter,
        MusicDirector music) {
    public SessionControllers {
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
