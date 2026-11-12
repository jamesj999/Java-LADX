package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
import linksawakening.app.gameplay.BushInteraction;
import linksawakening.app.gameplay.DialogInteractions;
import linksawakening.app.gameplay.GameplaySimulation;
import linksawakening.app.gameplay.LiftInteraction;
import linksawakening.app.story.OpeningStory;
import linksawakening.config.AppConfig;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gameplay.PcmSoundOutput;
import linksawakening.recording.GameplayRecorder;
import linksawakening.recording.RecordingOptions;
import linksawakening.startup.MarinWakeUpMotion;
import linksawakening.startup.StartupCoordinator;
import linksawakening.startup.TarinShieldMotion;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.transition.Warp;

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
            context.host.recordingStatus(recorder != null);
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
        context.composeFrameBuffer();
        if (recorder != null) recorder.captureFrame(context.services.indexedDisplayBuffer());
    }

    GameplaySoundSink gameplaySoundSink = GameplaySoundSink.none();

    MarinWakeUpMotion newGameWakeUpMotion;

    TarinShieldMotion newGameTarinShieldMotion;

    PcmSoundOutput headlessPcm;

    int frameCounter;

    final RomRandomByteSource random;

    final long frameDurationNs = 1_000_000_000L / 60L;

    SessionContext context;

    public byte[] headlessFrame() {
        renderFrame();
        return context.services.indexedDisplayBuffer().clone();
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
                context.services.dialogController().setPlayerName("A");
                context.saves().startNewGame();
            } else if (StartupCoordinator.shouldStartIntroCutscene(context.currentAppConfig())) {
                context.router().startIntroCutscene();
            } else if (context.currentAppConfig().startMode() == AppConfig.StartMode.NORMAL
                    || !context.currentAppConfig().showTitleScreen()) {
                context.router().startConfiguredGameplay();
            } else {
                context.router().startTitleScreenWithoutIntro();
            }
            context.host.open(context::onKeyEvent);
            context.host.recordingStatus(recorder != null);
            long nextFrameNs = System.nanoTime();
            long rendered = 0;
            while (!context.host.shouldClose()
                    && context.running
                    && (options.frames() == 0 || rendered < options.frames())) {
                context.host.pollEvents();
                renderFrame();
                context.host.present(context.services.indexedDisplayBuffer());
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
        context.inputRouter().headlessButton(button, pressed);
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
        context.router().startIntroCutscene();
    }

    void update() {
        if (context.debug.debugMenu != null && context.debug.debugMenu.isOpen()) return;
        frameCounter = (frameCounter + 1) & 0xFF;
        // The engine's frame boundary uses scanline zero for non-entity draws.
        random.beginFrame(frameCounter, 0);

        context.musicDirector.tick();

        context.screens.currentScreen.tick();
    }

    void cleanup() {
        close();
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try {
            if (context == null) return;
            context.host.releasePresentation();
            if (context.services.dialogSoundOutput() != null) {
                context.services.dialogSoundOutput().close();
            }
            if (context.services.musicPlayer() != null) {
                context.services.musicPlayer().close();
            }

            context.host.close();
        } finally {
            if (recorder != null) recorder.close();
        }
    }

    public void initializeHeadlessNewGame() {
        if (context != null) throw new IllegalStateException("Game already initialized");
        initialize(true);
        context.services.dialogController().setPlayerName("A");
        context.saves().startNewGame();
    }

    private void initialize(boolean headless) {
        if (closed || context != null) throw new IllegalStateException("Session already initialized or closed");
        context = new SessionContext(this, headless);
    }

    void onKeyEvent(int key, int action) {
        context.onKeyEvent(key, action);
    }

    private void debugTeleport(Warp destination) {
        context.debugTeleport(destination);
    }

    SaveCoordinator saves() {
        return context.saves();
    }

    ScreenRouter router() {
        return context.router();
    }

    RenderStateAssembler renderer() {
        return context.renderer();
    }

    InputRouter inputRouter() {
        return context.inputRouter();
    }

    OpeningStory openingStory() {
        return context.openingStory();
    }

    GameplaySimulation simulation() {
        return context.simulation();
    }

    BushInteraction bushes() {
        return context.bushes();
    }

    DialogInteractions dialogs() {
        return context.dialogs();
    }

    LiftInteraction lifting() {
        return context.lifting();
    }
}
