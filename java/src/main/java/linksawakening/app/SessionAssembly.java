package linksawakening.app;

import linksawakening.app.gameplay.GameplayFrame;
import linksawakening.app.gameplay.TransitionPhase;
import linksawakening.app.host.DesktopHost;
import linksawakening.app.host.HeadlessHost;
import linksawakening.app.host.Host;
import linksawakening.gpu.GPU;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

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
        var prepared = GameplayWiring.build(s, state);
        var wired = FlowWiring.build(s, state, saveState, navigator, lifecycle, prepared);
        return finish(s, state, saveState, navigator, host, lifecycle, prepared, wired);
    }

    private static SessionContext finish(
            SessionServices s,
            SessionState state,
            SaveState saveState,
            SessionNavigator navigator,
            Host host,
            RunState lifecycle,
            GameplayWiring.PreparedGameplay prepared,
            FlowWiring.Flows wired) {
        var frame = new GameplayFrame(
                new TransitionPhase(
                        s.world().scrollController(),
                        s.world().transitionController(),
                        s.player().link(),
                        4),
                wired.simulation(),
                prepared.completion());
        var screens = ScreenWiring.build(
                new ScreenDependencies(
                        s.ui(), s.input(), state.screens(), saveState, prepared.dialogSounds(), wired.music()),
                navigator,
                wired.saves(),
                prepared.completion(),
                new GameplayScreenAdapter(wired.input(), frame, wired.passOut()));
        navigator.attach(
                screens,
                wired.router(),
                new RoomMusicSelection(
                        wired.music(), s.player().playerState(), s.world().roomSession()));
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
        var flows = new SessionFlows(
                wired.saves(),
                wired.router(),
                wired.input(),
                wired.renderer(),
                wired.simulation(),
                wired.interactions(),
                teleporter,
                wired.music());
        var runtime = new SessionRuntime(
                screens,
                new FramePresentation(
                        wired.renderer(), state.debug(), s.visual().indexedDisplayBuffer()),
                new SessionInput(state, s.input(), s.world(), s.player(), screens, wired.input(), teleporter),
                new SessionShutdown(host, s.audio()),
                flows,
                saveState);
        return new SessionContext(s, state, runtime, host, lifecycle);
    }
}
