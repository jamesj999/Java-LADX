package linksawakening.app.wiring;

import linksawakening.app.audio.RoomMusicSelection;
import linksawakening.app.debug.DebugTeleporter;
import linksawakening.app.flow.SessionContext;
import linksawakening.app.flow.SessionFlows;
import linksawakening.app.flow.SessionNavigator;
import linksawakening.app.flow.SessionRuntime;
import linksawakening.app.flow.SessionShutdown;
import linksawakening.app.gameplay.GameplayFrame;
import linksawakening.app.gameplay.TransitionPhase;
import linksawakening.app.host.DesktopHost;
import linksawakening.app.host.HeadlessHost;
import linksawakening.app.host.Host;
import linksawakening.app.input.GameplayScreenAdapter;
import linksawakening.app.input.SessionInput;
import linksawakening.app.presentation.FramePresentation;
import linksawakening.app.services.SessionServices;
import linksawakening.app.state.DebugState;
import linksawakening.app.state.FrameClock;
import linksawakening.app.state.GameplayInputState;
import linksawakening.app.state.GameplaySounds;
import linksawakening.app.state.OpeningStoryState;
import linksawakening.app.state.RunState;
import linksawakening.app.state.SaveState;
import linksawakening.app.state.ScreenState;
import linksawakening.app.state.SessionOptions;
import linksawakening.app.state.SessionState;
import linksawakening.app.wiring.ScreenWiring.ScreenDependencies;
import linksawakening.gameplay.PcmSoundOutput;
import linksawakening.gpu.GPU;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/** Connects application flows after ROM-backed services have been constructed. */
public final class SessionAssembly {
    private SessionAssembly() {}

    public static SessionContext build(
            RomRandomByteSource random,
            PcmSoundOutput headlessPcm,
            FrameClock clock,
            OpeningStoryState opening,
            GameplaySounds sounds,
            boolean headless) {
        var screens = new ScreenState();
        var navigator = new SessionNavigator(screens);
        Host host = headless
                ? (headlessPcm == null ? new HeadlessHost() : new HeadlessHost(headlessPcm))
                : new DesktopHost();
        var built = SessionWiring.build(
                random, new GPU(), new TransitionController(), new ScrollController(), navigator, host, headless);
        var services = built.services();
        sounds.replace(services.audio().gameplaySoundSink());
        var state = new SessionState(
                screens,
                built.scene(),
                new DebugState(),
                clock,
                opening,
                new GameplayInputState(),
                new SessionOptions(services.rom().appConfig()),
                sounds);
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
