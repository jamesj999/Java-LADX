package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import linksawakening.app.gameplay.BushInteraction;
import linksawakening.app.gameplay.DialogInteractions;
import linksawakening.app.gameplay.GameplaySimulation;
import linksawakening.app.gameplay.LiftInteraction;
import linksawakening.app.story.OpeningStory;
import linksawakening.config.AppConfig;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.startup.MarinWakeUpMotion;
import linksawakening.startup.StartupCoordinator;
import linksawakening.startup.TarinShieldMotion;
import linksawakening.world.entity.RomRandomByteSource;

public class GameSession {
    GameplaySoundSink gameplaySoundSink = GameplaySoundSink.none();

    MarinWakeUpMotion newGameWakeUpMotion;

    TarinShieldMotion newGameTarinShieldMotion;

    linksawakening.gameplay.PcmSoundOutput headlessPcm;

    int frameCounter;

    final RomRandomByteSource random;

    final long FRAME_DURATION_NS = 1_000_000_000L / 60L;

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

            nextFrameNs += FRAME_DURATION_NS;
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
        return new HeadlessGameDriver.Diagnostics(
                context.gpu,
                context.services.roomSession(),
                context.transitionController,
                frameCounter,
                context.scrollController,
                context.services.playerState(),
                context.services.inventoryMenu(),
                context.services.dialogController(),
                context.services.equipmentController(),
                context.services.link());
    }

    public void headlessButton(String button, boolean pressed) {
        context.inputRouter().headlessButton(button, pressed);
    }

    public GameSession() {
        this(new RomRandomByteSource());
    }

    public GameSession(RomRandomByteSource random) {
        this.random = java.util.Objects.requireNonNull(random);
    }

    public GameSession(linksawakening.gameplay.PcmSoundOutput output) {
        this();
        headlessPcm = java.util.Objects.requireNonNull(output);
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

        context.currentScreen.tick();
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

    private void debugTeleport(linksawakening.world.transition.Warp destination) {
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
