package linksawakening.app.wiring;

import linksawakening.app.audio.MusicDirector;
import linksawakening.app.flow.SaveCoordinator;
import linksawakening.app.flow.ScreenActions;
import linksawakening.app.flow.ScreenRouter;
import linksawakening.app.flow.SessionNavigator;
import linksawakening.app.flow.StoryActions;
import linksawakening.app.gameplay.GameplayControl;
import linksawakening.app.gameplay.GameplaySimulation;
import linksawakening.app.gameplay.interaction.GameplayInteractions;
import linksawakening.app.gameplay.recovery.GameOverFlow;
import linksawakening.app.gameplay.recovery.PassOutController;
import linksawakening.app.input.InputActions;
import linksawakening.app.input.InputRouter;
import linksawakening.app.input.NoClipControl;
import linksawakening.app.presentation.RenderStateAssembler;
import linksawakening.app.services.SessionServices;
import linksawakening.app.state.RunState;
import linksawakening.app.state.SaveState;
import linksawakening.app.state.SessionState;

/** Connects persistence, navigation, input and simulation to their actual dependencies. */
final class FlowWiring {
    private FlowWiring() {}

    record Flows(
            SaveCoordinator saves,
            ScreenRouter router,
            InputRouter input,
            RenderStateAssembler renderer,
            GameplaySimulation simulation,
            GameplayInteractions interactions,
            MusicDirector music,
            PassOutController passOut) {}

    static Flows build(
            SessionServices s,
            SessionState state,
            SaveState saveState,
            SessionNavigator navigator,
            RunState lifecycle,
            GameplayWiring.PreparedGameplay prepared) {
        var music = prepared.music();
        var story = prepared.interactions().story();
        var dialogs = prepared.interactions().dialogs();
        var interactions = prepared.interactions();
        var blockers = prepared.blockers();
        var dialogSounds = prepared.dialogSounds();
        var renderer = new RenderStateAssembler(s.rom(), s.world(), s.player(), s.ui(), s.visual(), state);
        var saves = new SaveCoordinator(
                s.rom(), s.world(), s.player(), s.input(), state, saveState, navigator, new StoryActions(music, story));
        var passOut = new PassOutController();
        var gameOver = new GameOverFlow(
                s.player().equipmentController(),
                passOut,
                saves,
                s.visual().transientVfxSystem(),
                s.visual().droppableRupeeSystem());
        var router = new ScreenRouter(
                s.rom(),
                s.world(),
                s.player(),
                s.ui(),
                s.input(),
                state,
                new ScreenActions(navigator, saves, saveState, music));
        var input = new InputRouter(
                s.input(),
                s.world(),
                s.player(),
                s.ui(),
                state,
                new InputActions(
                        s.rom().romTables(),
                        navigator,
                        lifecycle,
                        new NoClipControl(state.debug(), s.player().link()),
                        saves,
                        story,
                        dialogs));
        var simulation = new GameplaySimulation(
                s.rom(),
                s.world(),
                s.player(),
                s.ui(),
                s.input(),
                s.visual(),
                state,
                new GameplayControl(s.audio(), music, interactions, passOut, gameOver, blockers, input, dialogSounds));
        return new Flows(saves, router, input, renderer, simulation, interactions, music, passOut);
    }
}
