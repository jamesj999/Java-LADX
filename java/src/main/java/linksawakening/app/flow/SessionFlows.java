package linksawakening.app.flow;

import java.util.Objects;
import linksawakening.app.audio.MusicDirector;
import linksawakening.app.debug.DebugTeleporter;
import linksawakening.app.gameplay.GameplaySimulation;
import linksawakening.app.gameplay.interaction.GameplayInteractions;
import linksawakening.app.input.InputRouter;
import linksawakening.app.presentation.RenderStateAssembler;

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
