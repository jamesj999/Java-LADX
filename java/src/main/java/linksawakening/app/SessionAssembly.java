package linksawakening.app;

import linksawakening.app.debug.*;
import linksawakening.app.gameplay.*;
import linksawakening.app.host.*;
import linksawakening.app.screen.*;
import linksawakening.app.story.*;
import linksawakening.gpu.GPU;
import linksawakening.world.transition.*;

/** Connects application flows after ROM-backed services have been constructed. */
final class SessionAssembly {
    private SessionAssembly() {}

    static SessionContext build(GameSession session, boolean headless) {
        var screens = new ScreenState();
        var navigator = new SessionNavigator(screens);
        Host host = headless
                ? (session.headlessPcm == null ? new HeadlessHost() : new HeadlessHost(session.headlessPcm))
                : new DesktopHost();
        var built = SessionWiring.build(
                session.random,
                new GPU(),
                new TransitionController(),
                new ScrollController(),
                navigator,
                host,
                headless);
        var services = built.services();
        session.gameplaySounds().replace(services.audio().gameplaySoundSink());
        var state = new SessionState(
                screens,
                built.scene(),
                new DebugState(),
                session.frameClock(),
                session.openingState(),
                new GameplayInputState(),
                new SessionOptions(services.rom().appConfig()),
                session.gameplaySounds());
        var lifecycle = new RunState();
        return wire(services, state, built.saves(), navigator, host, lifecycle);
    }

    private static SessionContext wire(
            SessionServices s,
            SessionState state,
            SaveState saveState,
            SessionNavigator navigator,
            Host host,
            RunState lifecycle) {
        var music = new MusicDirector(s.audio().musicPlayer(), s.audio().gameplayMusicController());
        var dialogSounds =
                new SessionDialogSounds(s.ui().dialogController(), s.audio().dialogSoundSink());
        var blockers = new DialogBlockerPolicy(
                s.ui().inventoryController(),
                s.ui().dialogController(),
                s.world().scrollController(),
                s.world().transitionController(),
                state.input());
        var completion = new FrameInputCompletion(s.input().inputState(), state.input());
        var lift = new LiftInteraction(
                s.input().inputState(),
                s.input().inputConfig(),
                s.player().playerState(),
                s.player().link(),
                s.world().roomSession());
        var dialogs = new DialogInteractions(s.input(), s.world(), s.player(), s.ui(), blockers);
        var bush = new BushInteraction(s.world(), s.player(), s.visual(), state.sounds());
        var story = new OpeningStory(
                s.player(),
                s.world(),
                s.input(),
                s.ui(),
                state,
                new OpeningActions(s.visual().newGameTarinShieldPalette(), blockers, music));
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
        var interactions = new GameplayInteractions(story, lift, bush, dialogs);
        var simulation = new GameplaySimulation(
                s.rom(),
                s.world(),
                s.player(),
                s.ui(),
                s.input(),
                s.visual(),
                state,
                new GameplayControl(s.audio(), music, interactions, passOut, gameOver, blockers, input, dialogSounds));
        var frame = new GameplayFrame(
                new TransitionPhase(
                        s.world().scrollController(),
                        s.world().transitionController(),
                        s.player().link(),
                        4),
                simulation,
                completion);
        var screens = ScreenWiring.build(
                new ScreenDependencies(s.ui(), s.input(), state.screens(), saveState, dialogSounds, music),
                navigator,
                saves,
                completion,
                new GameplayScreenAdapter(input, frame, passOut));
        navigator.attach(
                screens,
                router,
                new RoomMusicSelection(
                        music, s.player().playerState(), s.world().roomSession()));
        state.screens().currentScreen = screens.title();
        var teleporter = new DebugTeleporter(
                s.world().roomSession(),
                s.player().link(),
                state.opening(),
                state.debug(),
                s.visual().transientVfxSystem(),
                s.visual().droppableRupeeSystem(),
                s.input().inputState(),
                screens.gameplay());
        var flows = new SessionFlows(saves, router, input, renderer, simulation, interactions, teleporter, music);
        var runtime = new SessionRuntime(
                screens,
                new FramePresentation(renderer, state.debug(), s.visual().indexedDisplayBuffer()),
                new SessionInput(state, s.input(), s.world(), s.player(), screens, input, teleporter),
                new SessionShutdown(host, s.audio()),
                flows,
                saveState);
        return new SessionContext(s, state, runtime, host, lifecycle);
    }
}
