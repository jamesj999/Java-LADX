package linksawakening.app.wiring;

import linksawakening.app.audio.RoomMusicSelection;
import linksawakening.app.debug.DebugTeleporter;
import linksawakening.app.debug.DebugTools;
import linksawakening.app.flow.FrameLoopServices;
import linksawakening.app.flow.SessionContext;
import linksawakening.app.flow.SessionControllers;
import linksawakening.app.flow.SessionNavigator;
import linksawakening.app.flow.SessionShutdown;
import linksawakening.app.gameplay.GameplayFrame;
import linksawakening.app.gameplay.TransitionPhase;
import linksawakening.app.host.DesktopHost;
import linksawakening.app.host.HeadlessHost;
import linksawakening.app.host.Host;
import linksawakening.app.input.GameplayScreenAdapter;
import linksawakening.app.input.SessionInput;
import linksawakening.app.presentation.FramePresentation;
import linksawakening.app.screen.ScreenSet;
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

/**
 * Application composition root. Runs once to build services, attach navigation, and return the fully wired
 * session.
 */
public final class SessionWiring {
    private SessionWiring() {}

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
        var built = SubsystemWiring.build(
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
        var frame = gameplayFrame(s, prepared, wired);
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
        var flows = new SessionControllers(
                wired.saves(),
                wired.router(),
                wired.input(),
                wired.renderer(),
                wired.simulation(),
                wired.interactions(),
                teleporter,
                wired.music());
        var runtime = frameLoop(s, state, saveState, host, screens, flows);
        return new SessionContext(s, state, runtime, host, lifecycle);
    }

    private static GameplayFrame gameplayFrame(
            SessionServices s, GameplayWiring.PreparedGameplay prepared, FlowWiring.Flows wired) {
        return new GameplayFrame(
                new TransitionPhase(
                        s.world().scrollController(),
                        s.world().transitionController(),
                        s.player().link(),
                        4),
                wired.simulation(),
                prepared.completion());
    }

    private static FrameLoopServices frameLoop(
            SessionServices s,
            SessionState state,
            SaveState saveState,
            Host host,
            ScreenSet screens,
            SessionControllers controllers) {
        var debugTools = new DebugTools(
                state.debug(), s.player().playerState(), s.world().roomSession(), controllers.teleporter());
        return new FrameLoopServices(
                screens,
                new FramePresentation(
                        controllers.renderer(), debugTools, s.visual().indexedDisplayBuffer()),
                new SessionInput(
                        state, s.input(), s.world(), s.player(), screens, controllers.inputRouter(), debugTools),
                new SessionShutdown(host, s.audio()),
                controllers,
                saveState);
    }
}
