package linksawakening.app;

import java.util.Objects;
import linksawakening.app.debug.DebugLog;
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
import linksawakening.recording.GameplayRecorder;
import linksawakening.recording.RecordingOptions;
import linksawakening.startup.StartupCoordinator;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.transition.Warp;

/** Owns the desktop and headless frame loops and delegates to retained session flows. */
public class GameSession implements AutoCloseable {
    GameplayRecorder recorder;
    private boolean closed;
    private RecordingOptions recordingSettings =
            RecordingOptions.defaults(java.nio.file.Path.of("recordings/gameplay.mp4"));

    /** Toggle from the input callback, between complete simulation frames. */
    void toggleRecording() {
        if (closed) return;
        try {
            if (recorder != null) {
                GameplayRecorder finished = recorder;
                recorder = null; // Detach audio before finalizing, including on encoder failure.
                finished.close();
                DebugLog.log("Recording stopped");
            } else {
                var directory = recordingSettings.output().toAbsolutePath().getParent();
                var output = directory.resolve("gameplay-"
                        + java.time.LocalDateTime.now()
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                        + "-" + java.util.UUID.randomUUID() + ".mp4");
                recorder = new GameplayRecorder(new RecordingOptions(
                        output, recordingSettings.scale(), recordingSettings.crf(), recordingSettings.ffmpeg()));
                DebugLog.log("Recording started: " + output);
            }
        } catch (java.io.UncheckedIOException failure) {
            System.err.println("Recording failed: " + failure.getMessage() + ": "
                    + failure.getCause().getMessage());
        } finally {
            context.host().recordingStatus(recorder != null);
        }
    }

    /** Configure before initializing either a desktop or headless session. */
    public void configureRecording(RecordingOptions options) {
        if (context != null || recorder != null || closed)
            throw new IllegalStateException("Session already initialized or closed");
        recorder = new GameplayRecorder(options);
        recordingSettings = options;
    }

    private void renderFrame() {
        if (recorder != null) recorder.beginFrame();
        update();
        context.runtime().presentation().composeFrameBuffer();
        if (recorder != null) recorder.captureFrame(context.services().visual().indexedDisplayBuffer());
    }

    private final FrameClock clock = new FrameClock();
    private final OpeningStoryState opening = new OpeningStoryState();
    private final GameplaySounds sounds = new GameplaySounds();

    public FrameClock frameClock() {
        return clock;
    }

    public OpeningStoryState openingState() {
        return opening;
    }

    public GameplaySounds gameplaySounds() {
        return sounds;
    }

    @Override
    public void close() {
        cleanup();
    }

    PcmSoundOutput headlessPcm;

    final RomRandomByteSource random;

    final long frameDurationNs = 1_000_000_000L / 60L;

    SessionContext context;

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

    public byte[] headlessFrame() {
        renderFrame();
        return context.services().visual().indexedDisplayBuffer().clone();
    }

    void stepWithoutRender() {
        update();
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
            context.host().recordingStatus(recorder != null);
            long nextFrameNs = System.nanoTime();
            long rendered = 0;
            while (!context.host().shouldClose()
                    && context.lifecycle().running()
                    && (options.frames() == 0 || rendered < options.frames())) {
                context.host().pollEvents();
                renderFrame();
                context.host().present(context.services().visual().indexedDisplayBuffer());
                rendered++;
                if (options.headless()) continue;
                nextFrameNs += frameDurationNs;
                long sleepNs = nextFrameNs - System.nanoTime();
                if (sleepNs > 0) {
                    try {
                        Thread.sleep(sleepNs / 1_000_000L, (int) (sleepNs % 1_000_000L));
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    // We're behind — drop the backlog so we don't spiral.
                    nextFrameNs = System.nanoTime();
                }
            }
        }
    }

    public HeadlessGameDriver.Diagnostics diagnostics() {
        return new HeadlessGameDriver.Diagnostics(context);
    }

    public void headlessButton(String button, boolean pressed) {
        context.runtime().flows().inputRouter().headlessButton(button, pressed);
    }

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

    public void initializeHeadlessIntro() {
        if (context != null) throw new IllegalStateException("Game already initialized");
        initialize(true);
        context.runtime().flows().router().startIntroCutscene();
    }

    void update() {
        if (context.state().debug().debugMenu != null
                && context.state().debug().debugMenu.isOpen()) return;
        clock.advance();
        // The engine's frame boundary uses scanline zero for non-entity draws.
        random.beginFrame(clock.value(), 0);

        context.runtime().flows().music().tick();

        context.state().screens().currentScreen.tick();
    }

    void cleanup() {
        if (closed) return;
        closed = true;
        try {
            if (context != null) context.runtime().shutdown().close();
        } finally {
            if (recorder != null) recorder.close();
        }
    }

    public void initializeHeadlessNewGame() {
        if (context != null) throw new IllegalStateException("Game already initialized");
        initialize(true);
        context.services().ui().dialogController().setPlayerName("A");
        context.runtime().flows().saves().startNewGame();
    }

    private void initialize(boolean headless) {
        if (closed || context != null) throw new IllegalStateException("Session already initialized or closed");
        context = SessionWiring.build(
                random,
                headlessPcm,
                clock,
                opening,
                sounds,
                headless,
                host -> new linksawakening.recording.RecordingHost(host, () -> recorder));
    }

    void onKeyEvent(int key, int action) {
        if (key >= 0 && key == context.services().input().inputConfig().recordingKey()) {
            if (action == linksawakening.input.KeyCodes.PRESS
                    && (recorder != null
                            || context.state().screens().currentScreen
                                    instanceof linksawakening.app.screen.GameplayScreen)) {
                toggleRecording();
            }
            return;
        }
        context.runtime().input().onKeyEvent(key, action);
    }

    public void debugTeleport(Warp destination) {
        context.runtime().flows().teleporter().teleport(destination);
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
}
