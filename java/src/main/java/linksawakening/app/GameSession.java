package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
import linksawakening.app.gameplay.BushInteraction;
import linksawakening.app.gameplay.DialogInteractions;
import linksawakening.app.gameplay.GameplaySimulation;
import linksawakening.app.gameplay.LiftInteraction;
import linksawakening.app.story.OpeningStory;
import linksawakening.config.AppConfig;
import linksawakening.gameplay.PcmSoundOutput;
import linksawakening.startup.StartupCoordinator;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.transition.Warp;

public class GameSession implements AutoCloseable {
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

    public byte[] headlessFrame() {
        update();
        context.composeFrameBuffer();
        return context.services.indexedDisplayBuffer().clone();
    }

    void stepWithoutRender() {
        update();
    }

    public void run(String[] args) {
        initialize(false);
        if (StartupCoordinator.shouldStartIntroCutscene(context.currentAppConfig())) {
            context.router().startIntroCutscene();
        } else if (context.currentAppConfig().startMode() == AppConfig.StartMode.NORMAL
                || !context.currentAppConfig().showTitleScreen()) {
            context.router().startConfiguredGameplay();
        } else {
            context.router().startTitleScreenWithoutIntro();
        }

        context.host.open(context::onKeyEvent);

        long nextFrameNs = System.nanoTime();

        while (!context.host.shouldClose() && context.running) {
            context.host.pollEvents();
            update();
            context.composeFrameBuffer();
            context.host.present(context.services.indexedDisplayBuffer());

            nextFrameNs += frameDurationNs;
            long sleepNs = nextFrameNs - System.nanoTime();
            if (sleepNs > 0) {
                try {
                    Thread.sleep(sleepNs / 1_000_000L, (int) (sleepNs % 1_000_000L));
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            } else {
                // We're behind — drop the backlog so we don't spiral.
                nextFrameNs = System.nanoTime();
            }
        }

        cleanup();
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
        clock.advance();
        // The engine's frame boundary uses scanline zero for non-entity draws.
        random.beginFrame(clock.value(), 0);

        context.musicDirector.tick();

        context.screens.currentScreen.tick();
    }

    void cleanup() {
        context.host.releasePresentation();
        if (context.services.dialogSoundOutput() != null) {
            context.services.dialogSoundOutput().close();
        }
        if (context.services.musicPlayer() != null) {
            context.services.musicPlayer().close();
        }

        context.host.close();
    }

    public void initializeHeadlessNewGame() {
        if (context != null) throw new IllegalStateException("Game already initialized");
        initialize(true);
        context.services.dialogController().setPlayerName("A");
        context.saves().startNewGame();
    }

    private void initialize(boolean headless) {
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
