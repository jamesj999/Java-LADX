package linksawakening.app;

import java.util.Objects;
import linksawakening.app.flow.SaveCoordinator;
import linksawakening.app.flow.ScreenRouter;
import linksawakening.app.flow.SessionContext;
import linksawakening.app.flow.SessionControllers;
import linksawakening.app.gameplay.GameplaySimulation;
import linksawakening.app.gameplay.interaction.BushInteraction;
import linksawakening.app.gameplay.interaction.DialogInteractions;
import linksawakening.app.gameplay.interaction.LiftInteraction;
import linksawakening.app.input.InputRouter;
import linksawakening.app.presentation.RenderStateAssembler;
import linksawakening.app.services.SessionServices;
import linksawakening.app.state.FrameClock;
import linksawakening.app.state.GameplaySounds;
import linksawakening.app.state.OpeningStoryState;
import linksawakening.app.state.RunState;
import linksawakening.app.state.SaveState;
import linksawakening.app.state.SessionState;
import linksawakening.app.story.OpeningStory;
import linksawakening.app.wiring.SessionWiring;
import linksawakening.config.AppConfig;
import linksawakening.gameplay.PcmSoundOutput;
import linksawakening.recording.RecordingControl;
import linksawakening.recording.RecordingOptions;
import linksawakening.startup.StartupCoordinator;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.transition.Warp;

/** Owns the desktop and headless frame loops and delegates to retained session flows. */
public class GameSession implements AutoCloseable {
    // Recording can be configured before SessionWiring builds the host and runtime.
    final RecordingControl recording = new RecordingControl();
    private boolean closed;

    private final FrameClock clock = new FrameClock();
    private final OpeningStoryState opening = new OpeningStoryState();
    private final GameplaySounds sounds = new GameplaySounds();

    PcmSoundOutput headlessPcm;

    final RomRandomByteSource random;

    SessionContext context;

    public GameSession() {
        this(new RomRandomByteSource());
    }

    public GameSession(RomRandomByteSource random) {
        this.random = Objects.requireNonNull(random);
    }

    public GameSession(PcmSoundOutput output) {
        this();
        headlessPcm = Objects.requireNonNull(output);
    }

    /** Configure before initializing either a desktop or headless session. */
    public void configureRecording(RecordingOptions options) {
        if (context != null || closed) throw new IllegalStateException("Session already initialized or closed");
        recording.configure(options);
    }

    public void run(String[] args) {
        LaunchOptions options = LaunchOptions.parse(args);
        if (options.help()) {
            System.out.print(LaunchOptions.usage());
            return;
        }
        try (this) {
            if (options.recording() != null) configureRecording(options.recording());
            initialize(options.headless());
            if (options.newGame()) {
                context.services().ui().dialogController().setPlayerName("A");
                context.runtime().flows().saves().startNewGame();
            } else if (StartupCoordinator.shouldStartIntroCutscene(
                    context.state().options().config())) {
                context.runtime().flows().router().startIntroCutscene();
            } else if (context.state().options().config().startMode() == AppConfig.StartMode.NORMAL
                    || !context.state().options().config().showTitleScreen()) {
                context.runtime().flows().router().startConfiguredGameplay();
            } else {
                context.runtime().flows().router().startTitleScreenWithoutIntro();
            }
            context.host().open(this::onKeyEvent);
            recording.notifyHost();
            FramePacer pacer = options.headless() ? null : new FramePacer();
            long rendered = 0;
            while (!context.host().shouldClose()
                    && context.lifecycle().running()
                    && (options.frames() == 0 || rendered < options.frames())) {
                context.host().pollEvents();
                renderFrame();
                context.host().present(context.services().visual().indexedDisplayBuffer());
                rendered++;
                if (options.headless()) continue;
                if (!pacer.awaitNextFrame()) break;
            }
        }
    }

    public void initializeHeadlessIntro() {
        if (context != null) throw new IllegalStateException("Game already initialized");
        initialize(true);
        context.runtime().flows().router().startIntroCutscene();
    }

    public void initializeHeadlessNewGame() {
        if (context != null) throw new IllegalStateException("Game already initialized");
        initialize(true);
        context.services().ui().dialogController().setPlayerName("A");
        context.runtime().flows().saves().startNewGame();
    }

    public byte[] headlessFrame() {
        renderFrame();
        return context.services().visual().indexedDisplayBuffer().clone();
    }

    /** Full unpaced frame into caller-owned storage; retains no reference to the output. */
    public byte[] headlessFrame(byte[] output) {
        var pixels = context.services().visual().indexedDisplayBuffer();
        if (output.length != pixels.length) throw new IllegalArgumentException("Framebuffer length");
        renderFrame();
        System.arraycopy(pixels, 0, output, 0, pixels.length);
        return output;
    }

    public void headlessButton(String button, boolean pressed) {
        context.runtime().flows().inputRouter().headlessButton(button, pressed);
    }

    public void debugTeleport(Warp destination) {
        context.runtime().flows().teleporter().teleport(destination);
    }

    public HeadlessGameDriver.Diagnostics diagnostics() {
        return new HeadlessGameDriver.Diagnostics(context);
    }

    public FrameClock frameClock() {
        return clock;
    }

    public OpeningStoryState openingState() {
        return opening;
    }

    public GameplaySounds gameplaySounds() {
        return sounds;
    }

    public SessionState state() {
        return context.state();
    }

    public SessionServices services() {
        return context.services();
    }

    public SessionControllers flows() {
        return context.runtime().flows();
    }

    public SaveState saveState() {
        return context.runtime().saves();
    }

    public RunState lifecycle() {
        return context.lifecycle();
    }

    @Override
    public void close() {
        cleanup();
    }

    void stepWithoutRender() {
        update();
    }

    void update() {
        if (context.runtime().debug().menu().isOpen()) return;
        clock.advance();
        // The engine's frame boundary uses scanline zero for non-entity draws.
        random.beginFrame(clock.value(), 0);

        context.runtime().flows().music().tick();

        context.state().screens().currentScreen().tick();
    }

    void cleanup() {
        if (closed) return;
        closed = true;
        try {
            if (context != null) context.runtime().shutdown().close();
        } finally {
            recording.close();
        }
    }

    void onKeyEvent(int key, int action) {
        if (key >= 0 && key == context.services().input().inputConfig().recordingKey()) {
            if (action == linksawakening.input.KeyCodes.PRESS
                    && (recording.active()
                            || context.state().screens().currentScreen()
                                    instanceof linksawakening.app.screen.GameplayScreen)) {
                recording.toggle();
            }
            return;
        }
        context.runtime().input().onKeyEvent(key, action);
    }

    SaveCoordinator saves() {
        return context.runtime().flows().saves();
    }

    ScreenRouter router() {
        return context.runtime().flows().router();
    }

    RenderStateAssembler renderer() {
        return context.runtime().flows().renderer();
    }

    InputRouter inputRouter() {
        return context.runtime().flows().inputRouter();
    }

    OpeningStory openingStory() {
        return context.runtime().flows().interactions().story();
    }

    GameplaySimulation simulation() {
        return context.runtime().flows().simulation();
    }

    BushInteraction bushes() {
        return context.runtime().flows().interactions().bush();
    }

    DialogInteractions dialogs() {
        return context.runtime().flows().interactions().dialogs();
    }

    LiftInteraction lifting() {
        return context.runtime().flows().interactions().lift();
    }

    private void renderFrame() {
        recording.beginFrame();
        update();
        context.runtime().presentation().composeFrameBuffer();
        recording.captureFrame(context.services().visual().indexedDisplayBuffer());
    }

    private void initialize(boolean headless) {
        if (closed || context != null) throw new IllegalStateException("Session already initialized or closed");
        context = SessionWiring.build(random, headlessPcm, clock, opening, sounds, headless, recording::decorateHost);
    }
}
